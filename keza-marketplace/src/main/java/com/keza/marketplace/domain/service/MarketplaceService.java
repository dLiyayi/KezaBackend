package com.keza.marketplace.domain.service;

import com.keza.common.exception.BusinessRuleException;
import com.keza.common.util.MoneyUtil;
import com.keza.investment.domain.model.Investment;
import com.keza.common.enums.InvestmentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
public class MarketplaceService {

    private static final Duration HOLDING_PERIOD = Duration.ofDays(365);
    private static final BigDecimal SELLER_FEE_PERCENT = new BigDecimal("2");

    /**
     * Validates that a listing can be created for the given investment.
     * Checks:
     * 1. The investment is in COMPLETED status.
     * 2. The 12-month holding period has elapsed since investment completion.
     * 3. Company consent has been provided.
     */
    public void validateListing(Investment investment, boolean companyConsent) {
        if (investment.getStatus() != InvestmentStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_INVESTMENT_STATUS",
                    "Only completed investments can be listed on the marketplace");
        }

        if (investment.getCompletedAt() == null) {
            throw new BusinessRuleException("MISSING_COMPLETION_DATE",
                    "Investment completion date is not recorded");
        }

        Instant holdingPeriodEnd = investment.getCompletedAt().plus(HOLDING_PERIOD);
        if (Instant.now().isBefore(holdingPeriodEnd)) {
            long daysRemaining = Duration.between(Instant.now(), holdingPeriodEnd).toDays();
            throw new BusinessRuleException("HOLDING_PERIOD_NOT_MET",
                    String.format("Investment must be held for at least 12 months before listing. %d days remaining.", daysRemaining));
        }

        if (!companyConsent) {
            throw new BusinessRuleException("COMPANY_CONSENT_REQUIRED",
                    "Company consent is required to list shares on the marketplace");
        }
    }

    /**
     * Calculates the seller fee as 2% of the total price.
     */
    public BigDecimal calculateSellerFee(BigDecimal totalPrice) {
        return MoneyUtil.percentage(totalPrice, SELLER_FEE_PERCENT);
    }
}
