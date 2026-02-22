package com.keza.ai.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.ai.domain.model.DocumentQualityResult;
import com.keza.ai.domain.model.DocumentValidationResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI-powered KYC document validation service.
 * <p>
 * Uses Spring AI ChatModel to analyze document content and metadata,
 * providing automated validation with confidence scoring and recommendation
 * for the appropriate review level (AUTO_APPROVE, QUICK_REVIEW, FULL_REVIEW).
 * <p>
 * Protected by a Resilience4j circuit breaker to gracefully handle AI service outages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class AiDocumentValidationService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    // Size thresholds for quality assessment
    private static final long MIN_IMAGE_SIZE_BYTES = 10 * 1024;   // 10KB
    private static final long MIN_PDF_SIZE_BYTES = 1024;           // 1KB
    private static final long MAX_FILE_SIZE_BYTES = 8 * 1024 * 1024; // 8MB

    /**
     * Expected fields per document type, used in the AI prompt to guide extraction.
     */
    private static final Map<String, List<String>> EXPECTED_FIELDS_BY_TYPE = Map.of(
            "NATIONAL_ID", List.of("name", "ID number", "date of birth"),
            "PASSPORT", List.of("name", "passport number", "nationality", "expiry date"),
            "DRIVING_LICENSE", List.of("name", "license number", "expiry date"),
            "KRA_PIN", List.of("name", "KRA PIN number"),
            "PROOF_OF_ADDRESS", List.of("name", "address", "date (within 3 months)"),
            "SELFIE", List.of("face visible", "matches other documents")
    );

    private static final String DOCUMENT_VALIDATION_PROMPT = """
            You are a KYC document validation assistant for a financial platform in East Africa.
            Analyze the following document metadata and extracted text content.

            Evaluate the document on these criteria:
            1. Document type matches the expected type (%s)
            2. Document appears authentic (not obviously tampered)
            3. Text is legible and extractable
            4. Required fields are present: %s
            5. Document is not expired

            Document metadata:
            - File name: %s
            - Content type: %s
            - File size: %d bytes
            - Extracted text: %s

            Respond ONLY in valid JSON format with no additional text:
            {
              "isValid": true/false,
              "confidenceScore": 0.0-1.0,
              "documentType": "detected type",
              "extractedFields": {"name": "...", "idNumber": "...", "expiryDate": "..."},
              "issues": ["issue1", "issue2"],
              "recommendation": "AUTO_APPROVE" | "QUICK_REVIEW" | "FULL_REVIEW"
            }
            """;

    /**
     * Assesses the quality of a document based on file metadata before AI validation.
     * This is a fast, rule-based pre-check that avoids unnecessary AI calls for
     * obviously problematic uploads.
     *
     * @param contentType the MIME content type of the document
     * @param fileSize    the file size in bytes
     * @param fileName    the original file name
     * @return a {@link DocumentQualityResult} with quality score and issues
     */
    public DocumentQualityResult assessDocumentQuality(String contentType, long fileSize, String fileName) {
        log.info("Assessing document quality: file='{}', type='{}', size={} bytes", fileName, contentType, fileSize);

        List<String> issues = new ArrayList<>();
        double qualityScore = 1.0;

        // Check file size: too small
        boolean isImage = contentType != null && contentType.startsWith("image/");
        boolean isPdf = "application/pdf".equalsIgnoreCase(contentType);

        if (isImage && fileSize < MIN_IMAGE_SIZE_BYTES) {
            issues.add(String.format("Image file too small (%d bytes). Minimum expected: %d bytes. " +
                    "May indicate a corrupt or placeholder file.", fileSize, MIN_IMAGE_SIZE_BYTES));
            qualityScore -= 0.4;
        } else if (isPdf && fileSize < MIN_PDF_SIZE_BYTES) {
            issues.add(String.format("PDF file too small (%d bytes). Minimum expected: %d bytes. " +
                    "May indicate an empty or corrupt document.", fileSize, MIN_PDF_SIZE_BYTES));
            qualityScore -= 0.4;
        }

        // Check file size: too large
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            issues.add(String.format("File too large (%d bytes, %.1f MB). Maximum: %d bytes. " +
                    "May indicate an uncompressed scan.", fileSize, fileSize / (1024.0 * 1024.0), MAX_FILE_SIZE_BYTES));
            qualityScore -= 0.2;
        }

        // Check content type matches expected format
        if (contentType == null || contentType.isBlank()) {
            issues.add("Missing content type. Cannot determine document format.");
            qualityScore -= 0.3;
        } else if (!isImage && !isPdf) {
            issues.add(String.format("Unexpected content type '%s'. Expected image (JPEG/PNG) or PDF.", contentType));
            qualityScore -= 0.3;
        }

        qualityScore = Math.max(0.0, Math.min(1.0, qualityScore));
        boolean passesMinimum = qualityScore >= 0.5 && issues.isEmpty() ||
                qualityScore >= 0.5;

        // If there are critical issues (score dropped below 0.5), it does not pass
        passesMinimum = qualityScore >= 0.5;

        log.info("Document quality assessment for '{}': score={}, passes={}, issues={}",
                fileName, qualityScore, passesMinimum, issues);

        return new DocumentQualityResult(qualityScore, issues, passesMinimum);
    }

    /**
     * Validates a KYC document using AI analysis.
     * <p>
     * Protected by a circuit breaker: if the AI service is down, the fallback
     * method returns a FULL_REVIEW result so the document goes to manual review.
     *
     * @param expectedDocType the expected document type (e.g., NATIONAL_ID, PASSPORT)
     * @param fileName        the original file name
     * @param contentType     the MIME content type
     * @param fileSize        the file size in bytes
     * @param extractedText   text extracted from the document (may be empty for images)
     * @return a {@link DocumentValidationResult} with validation details
     */
    @CircuitBreaker(name = "aiDocumentValidation", fallbackMethod = "fallbackValidation")
    public DocumentValidationResult validateDocument(String expectedDocType, String fileName,
                                                     String contentType, long fileSize,
                                                     String extractedText) {
        log.info("AI document validation for type: {}, file: {}", expectedDocType, fileName);

        List<String> expectedFields = EXPECTED_FIELDS_BY_TYPE.getOrDefault(
                expectedDocType, List.of("name", "ID number"));
        String expectedFieldsStr = String.join(", ", expectedFields);

        String prompt = String.format(DOCUMENT_VALIDATION_PROMPT,
                expectedDocType, expectedFieldsStr, fileName, contentType, fileSize,
                extractedText != null && !extractedText.isBlank() ? extractedText : "(no text extracted)");

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("AI document validation response: {}", response);

        return parseValidationResponse(response);
    }

    /**
     * Fallback method invoked when the circuit breaker is open or the AI call fails.
     */
    @SuppressWarnings("unused")
    private DocumentValidationResult fallbackValidation(String expectedDocType, String fileName,
                                                        String contentType, long fileSize,
                                                        String extractedText, Throwable t) {
        log.warn("AI document validation circuit breaker triggered for file '{}': {}", fileName, t.getMessage());
        return DocumentValidationResult.fallback("AI service unavailable: " + t.getMessage());
    }

    /**
     * Parses the AI model's JSON response into a structured {@link DocumentValidationResult}.
     * Falls back to manual parsing if Jackson deserialization fails.
     */
    private DocumentValidationResult parseValidationResponse(String response) {
        try {
            // Try ObjectMapper-based parsing first
            Map<String, Object> parsed = objectMapper.readValue(response, new TypeReference<>() {});
            return buildResultFromMap(parsed, response);
        } catch (Exception e) {
            log.warn("ObjectMapper parsing failed, falling back to manual parsing: {}", e.getMessage());
            return buildResultFromManualParsing(response);
        }
    }

    @SuppressWarnings("unchecked")
    private DocumentValidationResult buildResultFromMap(Map<String, Object> parsed, String rawResponse) {
        boolean isValid = Boolean.TRUE.equals(parsed.get("isValid"));
        double confidenceScore = parseDouble(parsed.get("confidenceScore"), 0.5);
        String detectedDocType = String.valueOf(parsed.getOrDefault("documentType", "UNKNOWN"));

        Map<String, String> extractedFields = new LinkedHashMap<>();
        Object fieldsObj = parsed.get("extractedFields");
        if (fieldsObj instanceof Map<?, ?> map) {
            map.forEach((k, v) -> extractedFields.put(String.valueOf(k), String.valueOf(v)));
        }

        List<String> issues = new ArrayList<>();
        Object issuesObj = parsed.get("issues");
        if (issuesObj instanceof List<?> list) {
            list.forEach(item -> issues.add(String.valueOf(item)));
        }

        String recommendation = determineRecommendation(confidenceScore);

        return new DocumentValidationResult(
                isValid, confidenceScore, detectedDocType,
                extractedFields, issues, recommendation, rawResponse
        );
    }

    private DocumentValidationResult buildResultFromManualParsing(String response) {
        double confidence = parseConfidenceScore(response);
        String recommendation = determineRecommendation(confidence);
        boolean isValid = response.contains("\"isValid\": true") || response.contains("\"isValid\":true");

        return new DocumentValidationResult(
                isValid, confidence, "UNKNOWN",
                Map.of(), List.of(), recommendation, response
        );
    }

    private String determineRecommendation(double confidence) {
        if (confidence >= 0.95) {
            return "AUTO_APPROVE";
        } else if (confidence >= 0.85) {
            return "QUICK_REVIEW";
        } else {
            return "FULL_REVIEW";
        }
    }

    private double parseConfidenceScore(String response) {
        try {
            int idx = response.indexOf("\"confidenceScore\"");
            if (idx == -1) idx = response.indexOf("confidenceScore");
            if (idx >= 0) {
                String sub = response.substring(idx);
                int colonIdx = sub.indexOf(':');
                if (colonIdx >= 0) {
                    String numStr = sub.substring(colonIdx + 1).trim();
                    StringBuilder num = new StringBuilder();
                    for (char c : numStr.toCharArray()) {
                        if (Character.isDigit(c) || c == '.') {
                            num.append(c);
                        } else if (!num.isEmpty()) {
                            break;
                        }
                    }
                    if (!num.isEmpty()) {
                        return Double.parseDouble(num.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse confidence score from AI response");
        }
        return 0.5;
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
