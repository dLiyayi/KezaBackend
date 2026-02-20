package com.keza.ai.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub implementation of AI chat that is active when AI features are disabled.
 * Returns canned responses without making any external API calls.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubAiChatService {

    private static final String DISABLED_RESPONSE_EN =
            "AI features are currently disabled. Please contact support for assistance or visit our FAQ at help.keza.app.";

    private static final String DISABLED_RESPONSE_SW =
            "Huduma za AI zimezimwa kwa sasa. Tafadhali wasiliana na msaada kwa usaidizi au tembelea FAQ yetu kwenye help.keza.app.";

    private static final String DISABLED_RESPONSE_FR =
            "Les fonctionnalites IA sont actuellement desactivees. Veuillez contacter le support pour obtenir de l'aide ou visiter notre FAQ sur help.keza.app.";

    /**
     * Returns a canned response indicating AI features are disabled.
     *
     * @param sessionId ignored in stub mode
     * @param message   ignored in stub mode
     * @param language  the language code for the response
     * @return a localized message indicating AI is disabled
     */
    public String chat(UUID sessionId, String message, String language) {
        log.info("AI chat stub called for session {} - AI features are disabled", sessionId);
        return resolveResponse(language);
    }

    private String resolveResponse(String language) {
        return switch (language.toLowerCase()) {
            case "sw" -> DISABLED_RESPONSE_SW;
            case "fr" -> DISABLED_RESPONSE_FR;
            default -> DISABLED_RESPONSE_EN;
        };
    }
}
