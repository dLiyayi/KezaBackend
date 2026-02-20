package com.keza.ai.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StubAiChatService")
class StubAiChatServiceTest {

    private StubAiChatService stubAiChatService;

    @BeforeEach
    void setUp() {
        stubAiChatService = new StubAiChatService();
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("should return English disabled response for 'en' language")
        void shouldReturnEnglishResponse() {
            UUID sessionId = UUID.randomUUID();

            String response = stubAiChatService.chat(sessionId, "Hello, how are you?", "en");

            assertThat(response)
                    .contains("AI features are currently disabled")
                    .contains("help.keza.app");
        }

        @Test
        @DisplayName("should return Swahili disabled response for 'sw' language")
        void shouldReturnSwahiliResponse() {
            UUID sessionId = UUID.randomUUID();

            String response = stubAiChatService.chat(sessionId, "Habari", "sw");

            assertThat(response)
                    .contains("Huduma za AI zimezimwa kwa sasa")
                    .contains("help.keza.app");
        }

        @Test
        @DisplayName("should return French disabled response for 'fr' language")
        void shouldReturnFrenchResponse() {
            UUID sessionId = UUID.randomUUID();

            String response = stubAiChatService.chat(sessionId, "Bonjour", "fr");

            assertThat(response)
                    .contains("fonctionnalites IA sont actuellement desactivees")
                    .contains("help.keza.app");
        }

        @ParameterizedTest
        @ValueSource(strings = {"de", "es", "zh", "jp", "unknown", "EN"})
        @DisplayName("should default to English response for unsupported or uppercase languages")
        void shouldDefaultToEnglishForUnsupportedLanguages(String language) {
            UUID sessionId = UUID.randomUUID();

            String response = stubAiChatService.chat(sessionId, "test message", language);

            assertThat(response).contains("AI features are currently disabled");
        }

        @Test
        @DisplayName("should ignore sessionId and message content")
        void shouldIgnoreSessionIdAndMessage() {
            UUID session1 = UUID.randomUUID();
            UUID session2 = UUID.randomUUID();

            String response1 = stubAiChatService.chat(session1, "Tell me about investments", "en");
            String response2 = stubAiChatService.chat(session2, "Completely different question", "en");

            assertThat(response1).isEqualTo(response2);
        }

        @Test
        @DisplayName("should handle case-insensitive language codes")
        void shouldHandleCaseInsensitiveLanguageCodes() {
            UUID sessionId = UUID.randomUUID();

            String responseLower = stubAiChatService.chat(sessionId, "test", "sw");
            String responseUpper = stubAiChatService.chat(sessionId, "test", "SW");

            assertThat(responseLower).isEqualTo(responseUpper);
        }

        @Test
        @DisplayName("should always return non-null, non-empty response")
        void shouldAlwaysReturnNonNullNonEmpty() {
            UUID sessionId = UUID.randomUUID();

            String response = stubAiChatService.chat(sessionId, "", "en");

            assertThat(response).isNotNull().isNotEmpty();
        }
    }
}
