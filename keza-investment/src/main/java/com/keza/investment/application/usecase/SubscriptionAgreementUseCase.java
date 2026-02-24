package com.keza.investment.application.usecase;

import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.investment.application.dto.SignAgreementRequest;
import com.keza.investment.application.dto.SubscriptionAgreementResponse;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.model.SubscriptionAgreement;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.investment.domain.port.out.SubscriptionAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionAgreementUseCase {

    private final SubscriptionAgreementRepository agreementRepository;
    private final InvestmentRepository investmentRepository;

    @Transactional
    public SubscriptionAgreementResponse signAgreement(UUID investmentId, UUID userId, SignAgreementRequest request) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", investmentId));

        if (!investment.getInvestorId().equals(userId)) {
            throw new BusinessRuleException("You can only sign agreements for your own investments");
        }

        if (agreementRepository.findByInvestmentId(investmentId).isPresent()) {
            throw new BusinessRuleException("Agreement has already been signed for this investment");
        }

        SubscriptionAgreement agreement = SubscriptionAgreement.builder()
                .investmentId(investmentId)
                .userId(userId)
                .campaignId(investment.getCampaignId())
                .agreementVersion(request.getAgreementVersion())
                .signedAt(Instant.now())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .riskAcknowledged(request.isRiskAcknowledged())
                .build();

        agreement = agreementRepository.save(agreement);
        log.info("Subscription agreement signed for investment {} by user {}", investmentId, userId);
        return toResponse(agreement);
    }

    @Transactional(readOnly = true)
    public SubscriptionAgreementResponse getAgreement(UUID investmentId, UUID userId) {
        SubscriptionAgreement agreement = agreementRepository.findByInvestmentId(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionAgreement", "investmentId", investmentId.toString()));

        if (!agreement.getUserId().equals(userId)) {
            throw new BusinessRuleException("You can only view your own subscription agreements");
        }

        return toResponse(agreement);
    }

    @Transactional
    public SubscriptionAgreementResponse countersign(UUID agreementId, UUID adminId, String documentUrl) {
        SubscriptionAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionAgreement", agreementId));

        if (agreement.isCountersigned()) {
            throw new BusinessRuleException("Agreement has already been countersigned");
        }

        agreement.setCountersigned(true);
        agreement.setCountersignedAt(Instant.now());
        agreement.setDocumentUrl(documentUrl);
        agreement = agreementRepository.save(agreement);

        log.info("Subscription agreement {} countersigned by admin {}", agreementId, adminId);
        return toResponse(agreement);
    }

    private SubscriptionAgreementResponse toResponse(SubscriptionAgreement agreement) {
        return SubscriptionAgreementResponse.builder()
                .id(agreement.getId())
                .investmentId(agreement.getInvestmentId())
                .userId(agreement.getUserId())
                .campaignId(agreement.getCampaignId())
                .agreementVersion(agreement.getAgreementVersion())
                .signedAt(agreement.getSignedAt())
                .riskAcknowledged(agreement.isRiskAcknowledged())
                .countersigned(agreement.isCountersigned())
                .countersignedAt(agreement.getCountersignedAt())
                .documentUrl(agreement.getDocumentUrl())
                .createdAt(agreement.getCreatedAt())
                .build();
    }
}
