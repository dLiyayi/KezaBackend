package com.keza.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlugUtil")
class SlugUtilTest {

    @Nested
    @DisplayName("slugify")
    class Slugify {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should return empty string for null, empty, or blank input")
        void shouldReturnEmptyForNullEmptyBlank(String input) {
            assertThat(SlugUtil.slugify(input)).isEmpty();
        }

        @Test
        @DisplayName("should convert simple text to lowercase slug")
        void shouldConvertToLowercase() {
            assertThat(SlugUtil.slugify("Hello World")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("should replace spaces with hyphens")
        void shouldReplaceSpacesWithHyphens() {
            assertThat(SlugUtil.slugify("my campaign name")).isEqualTo("my-campaign-name");
        }

        @Test
        @DisplayName("should collapse multiple spaces into single hyphen")
        void shouldCollapseMultipleSpaces() {
            assertThat(SlugUtil.slugify("hello    world")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("should remove special characters")
        void shouldRemoveSpecialCharacters() {
            assertThat(SlugUtil.slugify("hello!@#$%world")).isEqualTo("helloworld");
        }

        @Test
        @DisplayName("should handle mixed special characters and spaces")
        void shouldHandleMixedSpecialCharsAndSpaces() {
            assertThat(SlugUtil.slugify("Keza - AI Platform!")).isEqualTo("keza-ai-platform");
        }

        @Test
        @DisplayName("should collapse multiple dashes into single dash")
        void shouldCollapseMultipleDashes() {
            assertThat(SlugUtil.slugify("hello---world")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("should strip leading and trailing dashes")
        void shouldStripLeadingTrailingDashes() {
            assertThat(SlugUtil.slugify("-hello world-")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("should handle unicode accented characters by normalizing")
        void shouldNormalizeUnicodeAccents() {
            assertThat(SlugUtil.slugify("cafe\u0301")).isEqualTo("cafe");
        }

        @Test
        @DisplayName("should handle string with only special characters")
        void shouldHandleOnlySpecialChars() {
            assertThat(SlugUtil.slugify("!@#$%^&*()")).isEmpty();
        }

        @Test
        @DisplayName("should preserve numbers in slug")
        void shouldPreserveNumbers() {
            assertThat(SlugUtil.slugify("Campaign 2024")).isEqualTo("campaign-2024");
        }

        @Test
        @DisplayName("should preserve underscores")
        void shouldPreserveUnderscores() {
            assertThat(SlugUtil.slugify("hello_world")).isEqualTo("hello_world");
        }

        @Test
        @DisplayName("should handle already-valid slug")
        void shouldHandleAlreadyValidSlug() {
            assertThat(SlugUtil.slugify("already-a-slug")).isEqualTo("already-a-slug");
        }

        @Test
        @DisplayName("should handle single word")
        void shouldHandleSingleWord() {
            assertThat(SlugUtil.slugify("Keza")).isEqualTo("keza");
        }

        @Test
        @DisplayName("should handle tabs and newlines as whitespace")
        void shouldHandleTabsAndNewlines() {
            assertThat(SlugUtil.slugify("hello\tworld\nfoo")).isEqualTo("hello-world-foo");
        }
    }
}
