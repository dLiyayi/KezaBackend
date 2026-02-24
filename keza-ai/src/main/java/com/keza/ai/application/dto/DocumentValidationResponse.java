package com.keza.ai.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentValidationResponse {

    private UUID documentId;
    private String status; // VALID, INVALID, NEEDS_REVIEW
    private double confidenceScore;
    private List<String> issues;
    private List<String> suggestions;
    private QualityCheck qualityCheck;
    private Map<String, String> extractedData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityCheck {
        private boolean imageClarity;
        private boolean documentComplete;
        private boolean tamperDetected;
        private boolean correctDocumentType;
        private String overallQuality; // GOOD, ACCEPTABLE, POOR
    }
}
