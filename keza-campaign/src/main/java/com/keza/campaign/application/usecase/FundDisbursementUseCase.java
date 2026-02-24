package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.FundDisbursementRequest;
import com.keza.campaign.application.dto.FundDisbursementResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.FundDisbursement;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.campaign.domain.port.out.FundDisbursementRepository;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.DisbursementStatus;
import com.keza.common.enums.DisbursementType;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundDisbursementUseCase {

    private final FundDisbursementRepository disbursementRepository;
    private final CampaignRepository campaignRepository;

    @Transactional
    public FundDisbursementResponse requestRollingClose(UUID campaignId, UUID issuerId, FundDisbursementRequest request) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("You can only request disbursements for your own campaigns");
        }

        if (campaign.getStatus() != CampaignStatus.LIVE) {
            throw new BusinessRuleException("Campaign must be live to request a rolling close");
        }

        if (campaign.getMinimumAmount() != null && campaign.getRaisedAmount().compareTo(campaign.getMinimumAmount()) < 0) {
            throw new BusinessRuleException("Campaign has not reached its minimum funding goal");
        }

        BigDecimal totalDisbursed = disbursementRepository.sumAmountByCampaignIdAndStatus(campaignId, DisbursementStatus.COMPLETED);
        BigDecimal available = campaign.getRaisedAmount().subtract(totalDisbursed);

        if (request.getAmount().compareTo(available) > 0) {
            throw new BusinessRuleException("Requested amount exceeds available funds. Available: " + available);
        }

        FundDisbursement disbursement = FundDisbursement.builder()
                .campaignId(campaignId)
                .amount(request.getAmount())
                .disbursementType(DisbursementType.ROLLING_CLOSE)
                .status(DisbursementStatus.PENDING)
                .requestedAt(Instant.now())
                .notes(request.getNotes())
                .referenceNumber("RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        disbursement = disbursementRepository.save(disbursement);
        log.info("Rolling close requested for campaign {}: amount={}", campaignId, request.getAmount());

        return toResponse(disbursement);
    }

    @Transactional(readOnly = true)
    public List<FundDisbursementResponse> getDisbursements(UUID campaignId, UUID userId) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        List<FundDisbursement> disbursements = disbursementRepository.findByCampaignIdAndDeletedFalseOrderByRequestedAtDesc(campaignId);
        return disbursements.stream().map(this::toResponse).toList();
    }

    @Transactional
    public FundDisbursementResponse processDisbursement(UUID disbursementId, UUID adminId, boolean approved) {
        FundDisbursement disbursement = disbursementRepository.findByIdAndDeletedFalse(disbursementId)
                .orElseThrow(() -> new ResourceNotFoundException("FundDisbursement", disbursementId));

        if (disbursement.getStatus() != DisbursementStatus.PENDING) {
            throw new BusinessRuleException("Disbursement is not in a pending state");
        }

        if (approved) {
            disbursement.setStatus(DisbursementStatus.COMPLETED);
        } else {
            disbursement.setStatus(DisbursementStatus.FAILED);
        }

        disbursement.setProcessedAt(Instant.now());
        disbursement = disbursementRepository.save(disbursement);

        log.info("Disbursement {} processed by admin {}: approved={}", disbursementId, adminId, approved);
        return toResponse(disbursement);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalDisbursed(UUID campaignId) {
        return disbursementRepository.sumAmountByCampaignIdAndStatus(campaignId, DisbursementStatus.COMPLETED);
    }

    private FundDisbursementResponse toResponse(FundDisbursement disbursement) {
        return FundDisbursementResponse.builder()
                .id(disbursement.getId())
                .campaignId(disbursement.getCampaignId())
                .amount(disbursement.getAmount())
                .disbursementType(disbursement.getDisbursementType())
                .status(disbursement.getStatus())
                .referenceNumber(disbursement.getReferenceNumber())
                .requestedAt(disbursement.getRequestedAt())
                .processedAt(disbursement.getProcessedAt())
                .notes(disbursement.getNotes())
                .createdAt(disbursement.getCreatedAt())
                .updatedAt(disbursement.getUpdatedAt())
                .build();
    }
}
