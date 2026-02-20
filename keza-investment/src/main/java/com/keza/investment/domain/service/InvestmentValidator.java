package com.keza.investment.domain.service;

import com.keza.campaign.domain.model.Campaign;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.KycStatus;
import com.keza.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class InvestmentValidator {

    /**
     * Validates whether an investor is eligible to invest in a campaign.
     *
     * @param investorId  the investor's user ID
     * @param campaign    the campaign to invest in
     * @param amount      the investment amount
     * @param kycStatus   the investor's current KYC status
     * @param alreadyInvested whether the investor already has an investment in this campaign
     */
    public void validate(UUID investorId, Campaign campaign, BigDecimal amount,
                         KycStatus kycStatus, boolean alreadyInvested) {

        validateKycApproved(kycStatus);
        validateCampaignIsLive(campaign);
        validateCampaignNotExpired(campaign);
        validateNoDuplicateInvestment(alreadyInvested);
        validateInvestmentAmount(campaign, amount);
        validateCmaLimits(investorId, amount);
    }

    private void validateKycApproved(KycStatus kycStatus) {
        if (kycStatus != KycStatus.APPROVED) {
            throw new BusinessRuleException("KYC_NOT_APPROVED",
                    "Your KYC verification must be approved before investing. Current status: " + kycStatus);
        }
    }

    private void validateCampaignIsLive(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.LIVE) {
            throw new BusinessRuleException("CAMPAIGN_NOT_LIVE",
                    "Campaign is not currently accepting investments. Status: " + campaign.getStatus());
        }
    }

    private void validateCampaignNotExpired(Campaign campaign) {
        if (campaign.getEndDate() != null && Instant.now().isAfter(campaign.getEndDate())) {
            throw new BusinessRuleException("CAMPAIGN_EXPIRED",
                    "Campaign investment period has ended");
        }
    }

    private void validateNoDuplicateInvestment(boolean alreadyInvested) {
        if (alreadyInvested) {
            throw new BusinessRuleException("DUPLICATE_INVESTMENT",
                    "You have already invested in this campaign");
        }
    }

    private void validateInvestmentAmount(Campaign campaign, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT",
                    "Investment amount must be greater than zero");
        }

        if (campaign.getMinInvestment() != null && amount.compareTo(campaign.getMinInvestment()) < 0) {
            throw new BusinessRuleException("BELOW_MINIMUM",
                    "Investment amount " + amount + " is below the minimum of " + campaign.getMinInvestment());
        }

        if (campaign.getMaxInvestment() != null && amount.compareTo(campaign.getMaxInvestment()) > 0) {
            throw new BusinessRuleException("ABOVE_MAXIMUM",
                    "Investment amount " + amount + " exceeds the maximum of " + campaign.getMaxInvestment());
        }

        BigDecimal remaining = campaign.getTargetAmount().subtract(campaign.getRaisedAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessRuleException("EXCEEDS_TARGET",
                    "Investment amount exceeds the remaining campaign target of " + remaining);
        }
    }

    /**
     * Validates CMA (Capital Markets Authority) regulatory limits.
     * Placeholder: currently always passes. Will be implemented with actual CMA rules
     * including annual investor limits and accredited investor thresholds.
     */
    public boolean validateCmaLimits(UUID investorId, BigDecimal amount) {
        // TODO: Implement CMA regulatory limit checks
        // - Retail investor annual limit (e.g., KES 100,000 per campaign or aggregate)
        // - Accredited investor exemptions
        // - Cross-campaign aggregate checks
        return true;
    }

    /**
     * Checks whether the investor qualifies as an accredited/sophisticated investor
     * under CMA regulations.
     * Placeholder: currently always returns false.
     */
    public boolean isAccreditedInvestor(UUID investorId) {
        // TODO: Implement accredited investor check based on:
        // - Annual income threshold
        // - Net worth threshold
        // - Professional qualifications
        return false;
    }
}
