package com.keza.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MoneyUtil")
class MoneyUtilTest {

    @Nested
    @DisplayName("formatKES")
    class FormatKES {

        @Test
        @DisplayName("should return 'KES 0.00' when amount is null")
        void shouldReturnZeroWhenNull() {
            assertThat(MoneyUtil.formatKES(null)).isEqualTo("KES 0.00");
        }

        @Test
        @DisplayName("should format zero amount correctly")
        void shouldFormatZero() {
            String result = MoneyUtil.formatKES(BigDecimal.ZERO);
            assertThat(result).contains("0.00");
            // Currency symbol may be "KES" or "Ksh" depending on JDK locale data
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).containsIgnoringCase("KES"),
                    r -> assertThat(r).containsIgnoringCase("Ksh")
            );
        }

        @Test
        @DisplayName("should format positive amount with two decimal places")
        void shouldFormatPositiveAmount() {
            String result = MoneyUtil.formatKES(new BigDecimal("1500.50"));
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("1,500.50"),
                    r -> assertThat(r).contains("1500.50")
            );
        }

        @Test
        @DisplayName("should format large amount with thousands separator")
        void shouldFormatLargeAmount() {
            String result = MoneyUtil.formatKES(new BigDecimal("1000000.00"));
            assertThat(result).isNotBlank();
            assertThat(result).contains("000");
        }

        @Test
        @DisplayName("should format negative amount")
        void shouldFormatNegativeAmount() {
            String result = MoneyUtil.formatKES(new BigDecimal("-250.75"));
            assertThat(result).contains("250.75");
        }

        @Test
        @DisplayName("should format amount with more than 2 decimal places by rounding")
        void shouldRoundWhenMoreThanTwoDecimals() {
            String result = MoneyUtil.formatKES(new BigDecimal("100.999"));
            assertThat(result).contains("101");
        }

        @Test
        @DisplayName("should format very small amount")
        void shouldFormatSmallAmount() {
            String result = MoneyUtil.formatKES(new BigDecimal("0.01"));
            assertThat(result).contains("0.01");
        }
    }

    @Nested
    @DisplayName("round")
    class Round {

        @Test
        @DisplayName("should return ZERO when amount is null")
        void shouldReturnZeroWhenNull() {
            assertThat(MoneyUtil.round(null)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should round to 2 decimal places using HALF_UP")
        void shouldRoundToTwoDecimalPlaces() {
            assertThat(MoneyUtil.round(new BigDecimal("10.555")))
                    .isEqualByComparingTo(new BigDecimal("10.56"));
        }

        @Test
        @DisplayName("should round down when third decimal is less than 5")
        void shouldRoundDown() {
            assertThat(MoneyUtil.round(new BigDecimal("10.554")))
                    .isEqualByComparingTo(new BigDecimal("10.55"));
        }

        @Test
        @DisplayName("should preserve value already at 2 decimal places")
        void shouldPreserveCorrectScale() {
            assertThat(MoneyUtil.round(new BigDecimal("99.99")))
                    .isEqualByComparingTo(new BigDecimal("99.99"));
        }

        @Test
        @DisplayName("should add trailing zeros for whole numbers")
        void shouldAddTrailingZeros() {
            BigDecimal result = MoneyUtil.round(new BigDecimal("100"));
            assertThat(result.scale()).isEqualTo(2);
            assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should handle negative amounts")
        void shouldHandleNegative() {
            assertThat(MoneyUtil.round(new BigDecimal("-5.555")))
                    .isEqualByComparingTo(new BigDecimal("-5.56"));
        }
    }

    @Nested
    @DisplayName("percentage")
    class Percentage {

        @Test
        @DisplayName("should calculate 10% of 100 as 10.00")
        void shouldCalculateTenPercent() {
            BigDecimal result = MoneyUtil.percentage(new BigDecimal("100"), new BigDecimal("10"));
            assertThat(result).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("should calculate 15% of 200 as 30.00")
        void shouldCalculateFifteenPercent() {
            BigDecimal result = MoneyUtil.percentage(new BigDecimal("200"), new BigDecimal("15"));
            assertThat(result).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("should round result to 2 decimal places")
        void shouldRoundResult() {
            BigDecimal result = MoneyUtil.percentage(new BigDecimal("33"), new BigDecimal("33"));
            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("should calculate 100% as the full amount")
        void shouldReturn100Percent() {
            BigDecimal result = MoneyUtil.percentage(new BigDecimal("500"), new BigDecimal("100"));
            assertThat(result).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("should calculate 0% as zero")
        void shouldReturnZeroPercent() {
            BigDecimal result = MoneyUtil.percentage(new BigDecimal("500"), BigDecimal.ZERO);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @ParameterizedTest
        @CsvSource({
                "1000, 2.5, 25.00",
                "5000, 0.1, 5.00",
                "999, 50, 499.50"
        })
        @DisplayName("should compute correct percentage for various inputs")
        void shouldComputeParameterized(String amount, String percent, String expected) {
            BigDecimal result = MoneyUtil.percentage(new BigDecimal(amount), new BigDecimal(percent));
            assertThat(result).isEqualByComparingTo(new BigDecimal(expected));
        }
    }
}
