package com.keza.investment.domain.service;

import com.keza.campaign.domain.model.Campaign;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.enums.KycStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.user.domain.model.User;
import com.keza.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentValidator {

    /**
     * CMA Kenya retail crowdfunding limits.
     * Maximum KES 100,000 per campaign for retail investors.
     */
    static final BigDecimal CMA_PER_CAMPAIGN_LIMIT = new BigDecimal("100000");

    /**
     * CMA Kenya maximum aggregate annual investment across all campaigns for retail investors.
     */
    static final BigDecimal CMA_ANNUAL_AGGREGATE_LIMIT = new BigDecimal("1000000");

    /**
     * Minimum annual income threshold (KES) to qualify as an accredited investor.
     */
    static final BigDecimal ACCREDITED_INVESTOR_INCOME_THRESHOLD = new BigDecimal("5000000");

    /**
     * Investment statuses that count towards CMA aggregate limits.
     */
    private static final Set<InvestmentStatus> ACTIVE_INVESTMENT_STATUSES = Set.of(
            InvestmentStatus.PENDING,
            InvestmentStatus.PAYMENT_INITIATED,
            InvestmentStatus.COOLING_OFF,
            InvestmentStatus.COMPLETED
    );

    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;

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
     * Validates CMA (Capital Markets Authority of Kenya) regulatory limits for crowdfunding.
     * <p>
     * CMA rules for retail investors:
     * <ul>
     *   <li>Maximum KES 100,000 per campaign</li>
     *   <li>Maximum KES 1,000,000 aggregate annually across all campaigns</li>
     * </ul>
     * Accredited investors (annual income >= KES 5,000,000) are exempt from these limits.
     *
     * @param investorId the investor's user ID
     * @param amount     the investment amount for the current transaction
     * @throws BusinessRuleException if CMA limits would be exceeded
     */
    public void validateCmaLimits(UUID investorId, BigDecimal amount) {
        if (isAccreditedInvestor(investorId)) {
            log.debug("Investor {} is accredited, CMA retail limits do not apply", investorId);
            return;
        }

        // Check per-campaign limit
        if (amount.compareTo(CMA_PER_CAMPAIGN_LIMIT) > 0) {
            throw new BusinessRuleException("CMA_PER_CAMPAIGN_LIMIT_EXCEEDED",
                    "Investment amount of KES " + amount + " exceeds the CMA per-campaign limit of KES "
                            + CMA_PER_CAMPAIGN_LIMIT + " for retail investors");
        }

        // Check annual aggregate limit
        BigDecimal totalInvestedThisYear = calculateAnnualInvestmentTotal(investorId);
        BigDecimal projectedTotal = totalInvestedThisYear.add(amount);

        if (projectedTotal.compareTo(CMA_ANNUAL_AGGREGATE_LIMIT) > 0) {
            BigDecimal remainingAllowance = CMA_ANNUAL_AGGREGATE_LIMIT.subtract(totalInvestedThisYear);
            throw new BusinessRuleException("CMA_ANNUAL_LIMIT_EXCEEDED",
                    "This investment of KES " + amount + " would bring your annual total to KES "
                            + projectedTotal + ", exceeding the CMA annual aggregate limit of KES "
                            + CMA_ANNUAL_AGGREGATE_LIMIT + " for retail investors. "
                            + "Remaining allowance: KES " + remainingAllowance.max(BigDecimal.ZERO));
        }

        log.debug("CMA limits validated for investor {}. Annual total: {}, new amount: {}, projected: {}",
                investorId, totalInvestedThisYear, amount, projectedTotal);
    }

    /**
     * Checks whether the investor qualifies as an accredited/sophisticated investor
     * under CMA regulations.
     * <p>
     * An investor is considered accredited if their annual income is at least KES 5,000,000.
     *
     * @param investorId the investor's user ID
     * @return true if the investor is accredited, false otherwise
     */
    public boolean isAccreditedInvestor(UUID investorId) {
        Optional<User> userOpt = userRepository.findByIdAndDeletedFalse(investorId);
        if (userOpt.isEmpty()) {
            log.warn("User {} not found for accreditation check, treating as non-accredited", investorId);
            return false;
        }

        User user = userOpt.get();
        BigDecimal annualIncome = user.getAnnualIncome();
        if (annualIncome == null) {
            return false;
        }

        return annualIncome.compareTo(ACCREDITED_INVESTOR_INCOME_THRESHOLD) >= 0;
    }

    /**
     * Calculates the total amount invested by the given investor in the current calendar year.
     * Only counts investments in active statuses (PENDING, PAYMENT_INITIATED, COOLING_OFF, COMPLETED).
     *
     * @param investorId the investor's user ID
     * @return the total invested amount in the current year
     */
    private BigDecimal calculateAnnualInvestmentTotal(UUID investorId) {
        Instant startOfYear = LocalDate.now().withDayOfYear(1)
                .atStartOfDay(ZoneId.of("Africa/Nairobi"))
                .toInstant();

        List<Investment> investments = investmentRepository.findByInvestorIdOrderByCreatedAtDesc(
                investorId, org.springframework.data.domain.Pageable.unpaged()).getContent();

        return investments.stream()
                .filter(inv -> inv.getCreatedAt() != null && !inv.getCreatedAt().isBefore(startOfYear))
                .filter(inv -> ACTIVE_INVESTMENT_STATUSES.contains(inv.getStatus()))
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
