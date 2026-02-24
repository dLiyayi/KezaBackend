package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.CampaignInterestRequest;
import com.keza.campaign.application.dto.CampaignInterestResponse;
import com.keza.campaign.application.dto.CampaignInterestSummary;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.CampaignInterest;
import com.keza.campaign.domain.port.out.CampaignInterestRepository;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignInterestUseCase {

    private final CampaignInterestRepository interestRepository;
    private final CampaignRepository campaignRepository;

    @Transactional
    public CampaignInterestResponse registerInterest(UUID campaignId, UUID userId, CampaignInterestRequest request) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.isTestingTheWaters()) {
            throw new BusinessRuleException("This campaign is not accepting interest registrations");
        }

        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.REVIEW) {
            throw new BusinessRuleException("Interest registration is only available for pre-launch campaigns");
        }

        if (interestRepository.existsByCampaignIdAndUserId(campaignId, userId)) {
            throw new BusinessRuleException("You have already registered interest in this campaign");
        }

        CampaignInterest interest = CampaignInterest.builder()
                .campaignId(campaignId)
                .userId(userId)
                .intendedAmount(request.getIntendedAmount())
                .registeredAt(Instant.now())
                .build();

        interest = interestRepository.save(interest);
        log.info("User {} registered interest in campaign {}", userId, campaignId);

        return toResponse(interest);
    }

    @Transactional(readOnly = true)
    public CampaignInterestSummary getInterestSummary(UUID campaignId, UUID issuerId) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("You can only view interest for your own campaigns");
        }

        long totalRegistrations = interestRepository.countByCampaignId(campaignId);
        var totalIntendedAmount = interestRepository.sumIntendedAmountByCampaignId(campaignId);

        return CampaignInterestSummary.builder()
                .totalRegistrations(totalRegistrations)
                .totalIntendedAmount(totalIntendedAmount)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CampaignInterestResponse> getInterestRegistrations(UUID campaignId, UUID issuerId, Pageable pageable) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("You can only view interest registrations for your own campaigns");
        }

        return interestRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CampaignInterestResponse> notifyInterestedInvestors(UUID campaignId, UUID issuerId) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("You can only notify investors for your own campaigns");
        }

        if (campaign.getStatus() != CampaignStatus.LIVE) {
            throw new BusinessRuleException("Campaign must be live to notify interested investors");
        }

        List<CampaignInterest> registrations = interestRepository.findByCampaignId(campaignId);
        log.info("Notifying {} interested investors for campaign {}", registrations.size(), campaignId);

        return registrations.stream().map(this::toResponse).toList();
    }

    private CampaignInterestResponse toResponse(CampaignInterest interest) {
        return CampaignInterestResponse.builder()
                .id(interest.getId())
                .campaignId(interest.getCampaignId())
                .userId(interest.getUserId())
                .email(interest.getEmail())
                .intendedAmount(interest.getIntendedAmount())
                .registeredAt(interest.getRegisteredAt())
                .build();
    }
}
