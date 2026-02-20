package com.keza.ai.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class AiDocumentValidationService {

    private final ChatModel chatModel;

    private static final String DOCUMENT_VALIDATION_PROMPT = """
            You are a KYC document validation assistant for a financial platform in East Africa.
            Analyze the following document metadata and extracted text content.

            Evaluate the document on these criteria:
            1. Document type matches the expected type (%s)
            2. Document appears authentic (not obviously tampered)
            3. Text is legible and extractable
            4. Required fields are present (name, ID number, dates)
            5. Document is not expired

            Document metadata:
            - File name: %s
            - Content type: %s
            - File size: %d bytes
            - Extracted text: %s

            Respond in JSON format:
            {
              "isValid": true/false,
              "confidenceScore": 0.0-1.0,
              "documentType": "detected type",
              "extractedFields": {"name": "...", "idNumber": "...", "expiryDate": "..."},
              "issues": ["issue1", "issue2"],
              "recommendation": "APPROVE" | "MANUAL_REVIEW" | "REJECT"
            }
            """;

    /**
     * Validates a KYC document using AI analysis.
     *
     * @param expectedDocType the expected document type (e.g., NATIONAL_ID, PASSPORT)
     * @param fileName        the original file name
     * @param contentType     the MIME content type
     * @param fileSize        the file size in bytes
     * @param extractedText   OCR-extracted text from the document (may be empty for images)
     * @return a map containing validation results
     */
    public Map<String, Object> validateDocument(String expectedDocType, String fileName,
                                                  String contentType, long fileSize,
                                                  String extractedText) {
        log.info("AI document validation for type: {}, file: {}", expectedDocType, fileName);

        try {
            String prompt = String.format(DOCUMENT_VALIDATION_PROMPT,
                    expectedDocType, fileName, contentType, fileSize,
                    extractedText != null ? extractedText : "(no text extracted)");

            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("AI document validation response: {}", response);

            // Parse the confidence score from the response to determine action
            double confidence = parseConfidenceScore(response);

            String recommendation;
            if (confidence >= 0.95) {
                recommendation = "AUTO_APPROVE";
            } else if (confidence >= 0.85) {
                recommendation = "QUICK_REVIEW";
            } else {
                recommendation = "FULL_REVIEW";
            }

            return Map.of(
                    "aiResponse", response,
                    "confidenceScore", confidence,
                    "recommendation", recommendation,
                    "validated", true
            );
        } catch (Exception e) {
            log.error("AI document validation failed: {}", e.getMessage(), e);
            return Map.of(
                    "aiResponse", "AI validation unavailable",
                    "confidenceScore", 0.0,
                    "recommendation", "FULL_REVIEW",
                    "validated", false,
                    "error", e.getMessage()
            );
        }
    }

    private double parseConfidenceScore(String response) {
        try {
            // Simple extraction of confidenceScore from JSON-like response
            int idx = response.indexOf("\"confidenceScore\"");
            if (idx == -1) idx = response.indexOf("confidenceScore");
            if (idx >= 0) {
                String sub = response.substring(idx);
                // Find the number after the colon
                int colonIdx = sub.indexOf(':');
                if (colonIdx >= 0) {
                    String numStr = sub.substring(colonIdx + 1).trim();
                    // Extract the number (might be followed by comma, bracket, etc.)
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
        return 0.5; // Default to middle confidence if parsing fails
    }
}
