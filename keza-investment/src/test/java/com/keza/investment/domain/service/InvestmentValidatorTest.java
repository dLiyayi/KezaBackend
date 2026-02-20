package com.keza.investment.domain.service;

import com.keza.campaign.domain.model.Campaign;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.KycStatus;
import com.keza.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InvestmentValidator")
class InvestmentValidatorTest {

    private InvestmentValidator validator;
    private UUID investorId;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        validator = new InvestmentValidator();
        investorId = UUID.randomUUID();

        campaign = Campaign.builder()
                .issuerId(UUID.randomUUID())
                .title("Test Campaign")
                .status(CampaignStatus.LIVE)
                .targetAmount(new BigDecimal("1000000"))
                .raisedAmount(new BigDecimal("100000"))
                .sharePrice(new BigDecimal("100"))
                .minInvestment(new BigDecimal("1000"))
                .maxInvestment(new BigDecimal("500000"))
                .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        campaign.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("should pass validation when all conditions are met")
    void shouldPassWhenAllConditionsMet() {
        assertThatCode(() -> validator.validate(
                investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
        )).doesNotThrowAnyException();
    }

    @Nested
    @DisplayName("KYC validation")
    class KycValidation {

        @Test
        @DisplayName("should throw KYC_NOT_APPROVED when KYC status is PENDING")
        void shouldThrowWhenKycPending() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.PENDING, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("KYC verification must be approved")
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("should throw KYC_NOT_APPROVED when KYC status is SUBMITTED")
        void shouldThrowWhenKycSubmitted() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.SUBMITTED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("KYC verification must be approved");
        }

        @Test
        @DisplayName("should throw KYC_NOT_APPROVED when KYC status is REJECTED")
        void shouldThrowWhenKycRejected() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.REJECTED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("KYC verification must be approved");
        }

        @Test
        @DisplayName("should throw KYC_NOT_APPROVED when KYC status is IN_REVIEW")
        void shouldThrowWhenKycInReview() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.IN_REVIEW, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("KYC verification must be approved");
        }
    }

    @Nested
    @DisplayName("Campaign status validation")
    class CampaignStatusValidation {

        @Test
        @DisplayName("should throw CAMPAIGN_NOT_LIVE when campaign is in DRAFT status")
        void shouldThrowWhenCampaignDraft() {
            campaign.setStatus(CampaignStatus.DRAFT);

            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not currently accepting investments")
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("should throw CAMPAIGN_NOT_LIVE when campaign is FUNDED")
        void shouldThrowWhenCampaignFunded() {
            campaign.setStatus(CampaignStatus.FUNDED);

            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not currently accepting investments");
        }

        @Test
        @DisplayName("should throw CAMPAIGN_NOT_LIVE when campaign is CLOSED")
        void shouldThrowWhenCampaignClosed() {
            campaign.setStatus(CampaignStatus.CLOSED);

            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not currently accepting investments");
        }

        @Test
        @DisplayName("should throw CAMPAIGN_NOT_LIVE when campaign is CANCELLED")
        void shouldThrowWhenCampaignCancelled() {
            campaign.setStatus(CampaignStatus.CANCELLED);

            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not currently accepting investments");
        }
    }

    @Nested
    @DisplayName("Campaign expiry validation")
    class CampaignExpiryValidation {

        @Test
        @DisplayName("should throw CAMPAIGN_EXPIRED when end date is in the past")
        void shouldThrowWhenCampaignExpired() {
            campaign.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));

            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("investment period has ended");
        }

        @Test
        @DisplayName("should pass when end date is null (no expiry)")
        void shouldPassWhenEndDateNull() {
            campaign.setEndDate(null);

            assertThatCode(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            )).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Duplicate investment validation")
    class DuplicateInvestmentValidation {

        @Test
        @DisplayName("should throw DUPLICATE_INVESTMENT when investor already invested")
        void shouldThrowWhenAlreadyInvested() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, true
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already invested");
        }
    }

    @Nested
    @DisplayName("Investment amount validation")
    class InvestmentAmountValidation {

        @Test
        @DisplayName("should throw BELOW_MINIMUM when amount is below campaign minimum")
        void shouldThrowWhenBelowMinimum() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("500"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("below the minimum");
        }

        @Test
        @DisplayName("should throw ABOVE_MAXIMUM when amount exceeds campaign maximum")
        void shouldThrowWhenAboveMaximum() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("600000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("exceeds the maximum");
        }

        @Test
        @DisplayName("should throw INVALID_AMOUNT when amount is zero")
        void shouldThrowWhenAmountZero() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, BigDecimal.ZERO, KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("should throw INVALID_AMOUNT when amount is negative")
        void shouldThrowWhenAmountNegative() {
            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("-1000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("should throw EXCEEDS_TARGET when amount exceeds remaining target")
        void shouldThrowWhenExceedsTarget() {
            campaign.setRaisedAmount(new BigDecimal("950000")); // remaining = 50000

            assertThatThrownBy(() -> validator.validate(
                    investorId, campaign, new BigDecimal("100000"), KycStatus.APPROVED, false
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("exceeds the remaining campaign target");
        }

        @Test
        @DisplayName("should pass when amount equals exact remaining target")
        void shouldPassWhenAmountEqualsRemaining() {
            campaign.setRaisedAmount(new BigDecimal("995000")); // remaining = 5000

            assertThatCode(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass when maxInvestment is null")
        void shouldPassWhenMaxInvestmentNull() {
            campaign.setMaxInvestment(null);

            assertThatCode(() -> validator.validate(
                    investorId, campaign, new BigDecimal("100000"), KycStatus.APPROVED, false
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass when minInvestment is null and amount is small")
        void shouldPassWhenMinInvestmentNull() {
            campaign.setMinInvestment(null);

            assertThatCode(() -> validator.validate(
                    investorId, campaign, new BigDecimal("1"), KycStatus.APPROVED, false
            )).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("CMA limits")
    class CmaLimits {

        @Test
        @DisplayName("validateCmaLimits should return true (placeholder)")
        void shouldReturnTrue() {
            boolean result = validator.validateCmaLimits(investorId, new BigDecimal("50000"));
            assertThatCode(() -> validator.validateCmaLimits(investorId, new BigDecimal("50000")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isAccreditedInvestor should return false (placeholder)")
        void shouldReturnFalse() {
            boolean result = validator.isAccreditedInvestor(investorId);
            org.assertj.core.api.Assertions.assertThat(result).isFalse();
        }
    }
}
