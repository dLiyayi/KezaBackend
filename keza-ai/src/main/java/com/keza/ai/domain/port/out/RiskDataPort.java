package com.keza.ai.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port interface for retrieving campaign data needed for risk scoring.
 * This allows keza-ai to score campaigns without depending on the keza-campaign module.
 * The real implementation is provided by keza-app, which bridges both modules.
 */
public interface RiskDataPort {

    /**
     * Retrieves campaign data relevant to risk scoring.
     *
     * @param campaignId the ID of the campaign to retrieve data for
     * @return a CampaignRiskData record containing the scoring-relevant fields
     * @throws CampaignDataNotFoundException if the campaign is not found
     */
    CampaignRiskData getCampaignData(UUID campaignId);

    /**
     * Immutable record containing all campaign fields needed for risk scoring.
     */
    record CampaignRiskData(
            UUID campaignId,
            String title,
            String description,
            String industry,
            String companyName,
            String companyRegistrationNumber,
            String companyWebsite,
            String companyAddress,
            BigDecimal targetAmount,
            BigDecimal raisedAmount,
            int investorCount,
            int wizardStep,
            boolean hasFinancialProjections,
            boolean hasRiskFactors,
            boolean hasUseOfFunds,
            boolean hasTeamMembers,
            boolean hasPitchVideo,
            int mediaCount,
            int updateCount,
            boolean issuerKycApproved,
            int issuerPreviousCampaigns
    ) {}

    /**
     * Exception thrown when campaign data cannot be found.
     */
    class CampaignDataNotFoundException extends RuntimeException {
        public CampaignDataNotFoundException(UUID campaignId) {
            super("Campaign data not found for risk scoring: " + campaignId);
        }
    }
}
