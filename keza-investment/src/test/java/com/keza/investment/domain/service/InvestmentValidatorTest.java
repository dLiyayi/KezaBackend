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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentValidator")
class InvestmentValidatorTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvestmentValidator validator;

    private UUID investorId;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
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

    private void mockRetailInvestorWithNoHistory() {
        User retailUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .passwordHash("hash")
                .annualIncome(new BigDecimal("500000"))
                .build();
        retailUser.setId(investorId);
        when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(retailUser));

        Page<Investment> emptyPage = new PageImpl<>(List.of());
        when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(investorId), any(Pageable.class)))
                .thenReturn(emptyPage);
    }

    @Test
    @DisplayName("should pass validation when all conditions are met")
    void shouldPassWhenAllConditionsMet() {
        mockRetailInvestorWithNoHistory();

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
            mockRetailInvestorWithNoHistory();

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
            mockRetailInvestorWithNoHistory();

            assertThatCode(() -> validator.validate(
                    investorId, campaign, new BigDecimal("5000"), KycStatus.APPROVED, false
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass when maxInvestment is null")
        void shouldPassWhenMaxInvestmentNull() {
            campaign.setMaxInvestment(null);
            mockRetailInvestorWithNoHistory();

            assertThatCode(() -> validator.validate(
                    investorId, campaign, new BigDecimal("100000"), KycStatus.APPROVED, false
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass when minInvestment is null and amount is small")
        void shouldPassWhenMinInvestmentNull() {
            campaign.setMinInvestment(null);
            mockRetailInvestorWithNoHistory();

            assertThatCode(() -> validator.validate(
                    investorId, campaign, new BigDecimal("1"), KycStatus.APPROVED, false
            )).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("CMA regulatory limits")
    class CmaLimits {

        @Test
        @DisplayName("should pass when retail investor is within per-campaign limit")
        void shouldPassWhenWithinPerCampaignLimit() {
            User retailUser = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("500000"))
                    .build();
            retailUser.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(retailUser));

            Page<Investment> emptyPage = new PageImpl<>(List.of());
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(investorId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            assertThatCode(() -> validator.validateCmaLimits(investorId, new BigDecimal("100000")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when retail investor exceeds per-campaign limit")
        void shouldThrowWhenExceedingPerCampaignLimit() {
            User retailUser = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("500000"))
                    .build();
            retailUser.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(retailUser));

            assertThatThrownBy(() -> validator.validateCmaLimits(investorId, new BigDecimal("150000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("CMA per-campaign limit")
                    .hasMessageContaining("100000");
        }

        @Test
        @DisplayName("should throw when retail investor exceeds annual aggregate limit")
        void shouldThrowWhenExceedingAnnualAggregateLimit() {
            User retailUser = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("500000"))
                    .build();
            retailUser.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(retailUser));

            // Existing investments totaling 950,000 this year
            Investment existingInvestment = Investment.builder()
                    .investorId(investorId)
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("950000"))
                    .shares(9500)
                    .sharePrice(new BigDecimal("100"))
                    .status(InvestmentStatus.COMPLETED)
                    .build();
            existingInvestment.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));

            Page<Investment> page = new PageImpl<>(List.of(existingInvestment));
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(investorId), any(Pageable.class)))
                    .thenReturn(page);

            assertThatThrownBy(() -> validator.validateCmaLimits(investorId, new BigDecimal("100000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("CMA annual aggregate limit")
                    .hasMessageContaining("1000000");
        }

        @Test
        @DisplayName("should pass when accredited investor exceeds retail limits")
        void shouldPassForAccreditedInvestor() {
            User accreditedUser = User.builder()
                    .email("rich@example.com")
                    .firstName("Rich")
                    .lastName("Investor")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("10000000"))
                    .build();
            accreditedUser.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(accreditedUser));

            assertThatCode(() -> validator.validateCmaLimits(investorId, new BigDecimal("500000")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should treat user with no annual income as non-accredited")
        void shouldTreatNullIncomeAsNonAccredited() {
            User userNoIncome = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(null)
                    .build();
            userNoIncome.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(userNoIncome));

            assertThat(validator.isAccreditedInvestor(investorId)).isFalse();
        }

        @Test
        @DisplayName("should treat non-existent user as non-accredited")
        void shouldTreatMissingUserAsNonAccredited() {
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.empty());

            assertThat(validator.isAccreditedInvestor(investorId)).isFalse();
        }

        @Test
        @DisplayName("should identify user at exact income threshold as accredited")
        void shouldIdentifyUserAtThresholdAsAccredited() {
            User userAtThreshold = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("5000000"))
                    .build();
            userAtThreshold.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(userAtThreshold));

            assertThat(validator.isAccreditedInvestor(investorId)).isTrue();
        }

        @Test
        @DisplayName("should identify user below income threshold as non-accredited")
        void shouldIdentifyUserBelowThresholdAsNonAccredited() {
            User userBelowThreshold = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("4999999"))
                    .build();
            userBelowThreshold.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(userBelowThreshold));

            assertThat(validator.isAccreditedInvestor(investorId)).isFalse();
        }

        @Test
        @DisplayName("should not count cancelled investments towards annual aggregate")
        void shouldNotCountCancelledInvestments() {
            User retailUser = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("500000"))
                    .build();
            retailUser.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(retailUser));

            // Cancelled investment should not count
            Investment cancelledInvestment = Investment.builder()
                    .investorId(investorId)
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("900000"))
                    .shares(9000)
                    .sharePrice(new BigDecimal("100"))
                    .status(InvestmentStatus.CANCELLED)
                    .build();
            cancelledInvestment.setCreatedAt(Instant.now().minus(5, ChronoUnit.DAYS));

            Page<Investment> page = new PageImpl<>(List.of(cancelledInvestment));
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(investorId), any(Pageable.class)))
                    .thenReturn(page);

            // Should pass because cancelled investment does not count
            assertThatCode(() -> validator.validateCmaLimits(investorId, new BigDecimal("100000")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should allow investment at exact annual aggregate limit")
        void shouldAllowAtExactAnnualLimit() {
            User retailUser = User.builder()
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .passwordHash("hash")
                    .annualIncome(new BigDecimal("500000"))
                    .build();
            retailUser.setId(investorId);
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(retailUser));

            Investment existingInvestment = Investment.builder()
                    .investorId(investorId)
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("900000"))
                    .shares(9000)
                    .sharePrice(new BigDecimal("100"))
                    .status(InvestmentStatus.COMPLETED)
                    .build();
            existingInvestment.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));

            Page<Investment> page = new PageImpl<>(List.of(existingInvestment));
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(investorId), any(Pageable.class)))
                    .thenReturn(page);

            // 900,000 + 100,000 = 1,000,000 exactly at limit
            assertThatCode(() -> validator.validateCmaLimits(investorId, new BigDecimal("100000")))
                    .doesNotThrowAnyException();
        }
    }
}
