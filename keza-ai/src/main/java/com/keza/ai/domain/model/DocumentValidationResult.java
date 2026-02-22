package com.keza.ai.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Result of AI-powered document validation.
 *
 * @param isValid              whether the document passed validation
 * @param confidenceScore      confidence score between 0.0 and 1.0
 * @param detectedDocumentType the document type detected by the AI
 * @param extractedFields      key-value pairs of data extracted from the document
 * @param issues               list of issues found during validation
 * @param recommendation       one of AUTO_APPROVE, QUICK_REVIEW, FULL_REVIEW
 * @param rawAiResponse        the raw response string from the AI model
 */
public record DocumentValidationResult(
        boolean isValid,
        double confidenceScore,
        String detectedDocumentType,
        Map<String, String> extractedFields,
        List<String> issues,
        String recommendation,
        String rawAiResponse
) {

    /**
     * Creates a fallback result used when AI validation is unavailable.
     */
    public static DocumentValidationResult fallback(String reason) {
        return new DocumentValidationResult(
                false,
                0.0,
                "UNKNOWN",
                Map.of(),
                List.of(reason),
                "FULL_REVIEW",
                "AI validation unavailable: " + reason
        );
    }
}
