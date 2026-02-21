package com.keza.ai.domain.service;

import com.keza.ai.domain.port.out.RiskDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Default/stub implementation of {@link RiskDataPort} used when no real adapter
 * is available (e.g., in unit tests or when keza-ai runs standalone).
 * Returns moderate default values so risk scoring still produces reasonable results.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RiskDataPort.class)
public class DefaultRiskDataAdapter implements RiskDataPort {

    @Override
    public CampaignRiskData getCampaignData(UUID campaignId) {
        log.warn("Using default risk data adapter for campaign {}. " +
                "Scores will be based on moderate default values.", campaignId);

        return new CampaignRiskData(
                campaignId,
                "Unknown Campaign",         // title
                "",                          // description
                null,                        // industry
                null,                        // companyName
                null,                        // companyRegistrationNumber
                null,                        // companyWebsite
                null,                        // companyAddress
                new BigDecimal("1000000"),   // targetAmount (1M KES - moderate)
                BigDecimal.ZERO,             // raisedAmount
                0,                           // investorCount
                1,                           // wizardStep
                false,                       // hasFinancialProjections
                false,                       // hasRiskFactors
                false,                       // hasUseOfFunds
                false,                       // hasTeamMembers
                false,                       // hasPitchVideo
                0,                           // mediaCount
                0,                           // updateCount
                false,                       // issuerKycApproved
                0                            // issuerPreviousCampaigns
        );
    }
}
