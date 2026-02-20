package com.keza.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PhoneUtil")
class PhoneUtilTest {

    @Nested
    @DisplayName("isValidKenyanPhone")
    class IsValidKenyanPhone {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("should return false for null, empty, or blank input")
        void shouldReturnFalseForNullEmptyBlank(String phone) {
            assertThat(PhoneUtil.isValidKenyanPhone(phone)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "+254712345678",
                "+254722345678",
                "+254733345678",
                "+254110345678",
                "+254100345678"
        })
        @DisplayName("should accept valid international format +254 numbers")
        void shouldAcceptValidInternationalFormat(String phone) {
            assertThat(PhoneUtil.isValidKenyanPhone(phone)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "0712345678",
                "0722345678",
                "0110345678"
        })
        @DisplayName("should accept valid local format 0xx numbers")
        void shouldAcceptValidLocalFormat(String phone) {
            assertThat(PhoneUtil.isValidKenyanPhone(phone)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "+254812345678",  // starts with 8 after +254
                "+254212345678",  // starts with 2 after +254
                "0812345678",     // starts with 08
                "0212345678",     // starts with 02
                "+25471234567",   // too short
                "+2547123456789", // too long
                "071234567",      // local too short
                "07123456789",    // local too long
                "254712345678",   // missing + prefix
                "12345",          // random short number
                "+1234567890",    // non-Kenyan international
                "abcdefghij"      // non-numeric
        })
        @DisplayName("should reject invalid phone numbers")
        void shouldRejectInvalidNumbers(String phone) {
            assertThat(PhoneUtil.isValidKenyanPhone(phone)).isFalse();
        }
    }

    @Nested
    @DisplayName("normalizeToInternational")
    class NormalizeToInternational {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("should return null for null, empty, or blank input")
        void shouldReturnNullForNullEmptyBlank(String phone) {
            assertThat(PhoneUtil.normalizeToInternational(phone)).isNull();
        }

        @Test
        @DisplayName("should convert local format 07xx to +254 format")
        void shouldConvertLocalToInternational() {
            assertThat(PhoneUtil.normalizeToInternational("0712345678"))
                    .isEqualTo("+254712345678");
        }

        @Test
        @DisplayName("should convert local format 01xx to +254 format")
        void shouldConvertLocal01ToInternational() {
            assertThat(PhoneUtil.normalizeToInternational("0110345678"))
                    .isEqualTo("+254110345678");
        }

        @Test
        @DisplayName("should add + prefix to 254 numbers missing it")
        void shouldAddPlusPrefix() {
            assertThat(PhoneUtil.normalizeToInternational("254712345678"))
                    .isEqualTo("+254712345678");
        }

        @Test
        @DisplayName("should return +254 number as-is")
        void shouldReturnInternationalAsIs() {
            assertThat(PhoneUtil.normalizeToInternational("+254712345678"))
                    .isEqualTo("+254712345678");
        }

        @Test
        @DisplayName("should strip whitespace before normalizing")
        void shouldStripWhitespace() {
            assertThat(PhoneUtil.normalizeToInternational(" 0712 345 678 "))
                    .isEqualTo("+254712345678");
        }

        @Test
        @DisplayName("should return non-Kenyan number unchanged")
        void shouldReturnNonKenyanUnchanged() {
            assertThat(PhoneUtil.normalizeToInternational("+1234567890"))
                    .isEqualTo("+1234567890");
        }
    }

    @Nested
    @DisplayName("formatForMpesa")
    class FormatForMpesa {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("should return null for null, empty, or blank input")
        void shouldReturnNullForNullEmptyBlank(String phone) {
            assertThat(PhoneUtil.formatForMpesa(phone)).isNull();
        }

        @Test
        @DisplayName("should convert local number to 254 format without +")
        void shouldConvertLocalToMpesaFormat() {
            assertThat(PhoneUtil.formatForMpesa("0712345678"))
                    .isEqualTo("254712345678");
        }

        @Test
        @DisplayName("should strip + from international format")
        void shouldStripPlus() {
            assertThat(PhoneUtil.formatForMpesa("+254712345678"))
                    .isEqualTo("254712345678");
        }

        @Test
        @DisplayName("should return 254 number as-is when no + prefix")
        void shouldReturn254AsIs() {
            assertThat(PhoneUtil.formatForMpesa("254712345678"))
                    .isEqualTo("254712345678");
        }
    }
}
