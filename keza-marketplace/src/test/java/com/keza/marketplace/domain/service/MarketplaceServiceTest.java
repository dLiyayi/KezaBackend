package com.keza.marketplace.domain.service;

import com.keza.common.enums.InvestmentStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.investment.domain.model.Investment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketplaceService")
class MarketplaceServiceTest {

    private MarketplaceService marketplaceService;

    @BeforeEach
    void setUp() {
        marketplaceService = new MarketplaceService();
    }

    @Nested
    @DisplayName("validateListing")
    class ValidateListing {

        @Test
        @DisplayName("should pass validation for completed investment with holding period met and company consent")
        void shouldPassValidation() {
            Investment investment = Investment.builder()
                    .investorId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("100000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("1000"))
                    .status(InvestmentStatus.COMPLETED)
                    .completedAt(Instant.now().minus(Duration.ofDays(400)))
                    .build();

            // Should not throw
            marketplaceService.validateListing(investment, true);
        }

        @Test
        @DisplayName("should reject investment that is not COMPLETED")
        void shouldRejectNonCompletedInvestment() {
            Investment investment = Investment.builder()
                    .investorId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("100000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("1000"))
                    .status(InvestmentStatus.PENDING)
                    .build();

            assertThatThrownBy(() -> marketplaceService.validateListing(investment, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Only completed investments");
        }

        @Test
        @DisplayName("should reject investment with missing completion date")
        void shouldRejectMissingCompletionDate() {
            Investment investment = Investment.builder()
                    .investorId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("100000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("1000"))
                    .status(InvestmentStatus.COMPLETED)
                    .completedAt(null)
                    .build();

            assertThatThrownBy(() -> marketplaceService.validateListing(investment, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("completion date is not recorded");
        }

        @Test
        @DisplayName("should reject investment within 12-month holding period")
        void shouldRejectWhenHoldingPeriodNotMet() {
            Investment investment = Investment.builder()
                    .investorId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("100000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("1000"))
                    .status(InvestmentStatus.COMPLETED)
                    .completedAt(Instant.now().minus(Duration.ofDays(100)))
                    .build();

            assertThatThrownBy(() -> marketplaceService.validateListing(investment, true))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("held for at least 12 months")
                    .hasMessageContaining("days remaining");
        }

        @Test
        @DisplayName("should reject listing without company consent")
        void shouldRejectWithoutCompanyConsent() {
            Investment investment = Investment.builder()
                    .investorId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("100000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("1000"))
                    .status(InvestmentStatus.COMPLETED)
                    .completedAt(Instant.now().minus(Duration.ofDays(400)))
                    .build();

            assertThatThrownBy(() -> marketplaceService.validateListing(investment, false))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Company consent is required");
        }

        @Test
        @DisplayName("should accept investment completed exactly 365 days ago")
        void shouldAcceptExactlyAtHoldingPeriod() {
            Investment investment = Investment.builder()
                    .investorId(UUID.randomUUID())
                    .campaignId(UUID.randomUUID())
                    .amount(new BigDecimal("100000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("1000"))
                    .status(InvestmentStatus.COMPLETED)
                    .completedAt(Instant.now().minus(Duration.ofDays(366)))
                    .build();

            // Should not throw
            marketplaceService.validateListing(investment, true);
        }
    }

    @Nested
    @DisplayName("calculateSellerFee")
    class CalculateSellerFee {

        @Test
        @DisplayName("should calculate 2% seller fee")
        void shouldCalculateTwoPercentFee() {
            BigDecimal totalPrice = new BigDecimal("100000");

            BigDecimal fee = marketplaceService.calculateSellerFee(totalPrice);

            assertThat(fee).isEqualByComparingTo(new BigDecimal("2000.00"));
        }

        @Test
        @DisplayName("should round fee to 2 decimal places")
        void shouldRoundFeeToTwoDecimalPlaces() {
            BigDecimal totalPrice = new BigDecimal("33333");

            BigDecimal fee = marketplaceService.calculateSellerFee(totalPrice);

            assertThat(fee.scale()).isLessThanOrEqualTo(2);
            assertThat(fee).isEqualByComparingTo(new BigDecimal("666.66"));
        }

        @Test
        @DisplayName("should return zero fee for zero price")
        void shouldReturnZeroFeeForZeroPrice() {
            BigDecimal fee = marketplaceService.calculateSellerFee(BigDecimal.ZERO);

            assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
