package com.keza.app.adapter;

import com.keza.ai.domain.port.out.RiskDataPort;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.KycStatus;
import com.keza.user.domain.model.User;
import com.keza.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Real implementation of {@link RiskDataPort} that bridges keza-campaign and keza-user
 * data into the keza-ai risk scoring system.
 * <p>
 * This adapter lives in keza-app because it has access to all modules. It is marked
 * as {@link Primary} so it takes precedence over the default stub in keza-ai.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RiskDataAdapter implements RiskDataPort {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    @Override
    public CampaignRiskData getCampaignData(UUID campaignId) {
        log.debug("Fetching campaign risk data for campaign: {}", campaignId);

        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new CampaignDataNotFoundException(campaignId));

        // Fetch issuer data for KYC and track record
        boolean issuerKycApproved = false;
        int issuerPreviousCampaigns = 0;

        try {
            User issuer = userRepository.findByIdAndDeletedFalse(campaign.getIssuerId())
                    .orElse(null);
            if (issuer != null) {
                issuerKycApproved = issuer.getKycStatus() == KycStatus.APPROVED;
            }

            // Count other campaigns by the same issuer (track record)
            long totalIssuerCampaigns = campaignRepository
                    .findByIssuerIdAndDeletedFalse(campaign.getIssuerId(),
                            org.springframework.data.domain.Pageable.unpaged())
                    .getTotalElements();
            // Subtract 1 for the current campaign
            issuerPreviousCampaigns = Math.max(0, (int) totalIssuerCampaigns - 1);

        } catch (Exception e) {
            log.warn("Could not fetch issuer data for campaign {}: {}", campaignId, e.getMessage());
        }

        return new CampaignRiskData(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getIndustry(),
                campaign.getCompanyName(),
                campaign.getCompanyRegistrationNumber(),
                campaign.getCompanyWebsite(),
                campaign.getCompanyAddress(),
                campaign.getTargetAmount(),
                campaign.getRaisedAmount(),
                campaign.getInvestorCount() != null ? campaign.getInvestorCount() : 0,
                campaign.getWizardStep() != null ? campaign.getWizardStep() : 1,
                isNotBlank(campaign.getFinancialProjections()),
                isNotBlank(campaign.getRiskFactors()),
                isNotBlank(campaign.getUseOfFunds()),
                isNotBlank(campaign.getTeamMembers()),
                isNotBlank(campaign.getPitchVideoUrl()),
                campaign.getMedia() != null ? campaign.getMedia().size() : 0,
                campaign.getUpdates() != null ? campaign.getUpdates().size() : 0,
                issuerKycApproved,
                issuerPreviousCampaigns
        );
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
