package com.keza.ai.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignOptimizationResponse {

    private UUID campaignId;
    private double successProbability;
    private String overallAssessment;
    private List<Suggestion> suggestions;
    private PricingAnalysis pricingAnalysis;
    private ContentAnalysis contentAnalysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String category; // CONTENT, PRICING, MARKETING, TIMING
        private String priority; // HIGH, MEDIUM, LOW
        private String title;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingAnalysis {
        private String assessment;
        private BigDecimal suggestedMinInvestment;
        private BigDecimal suggestedSharePrice;
        private String rationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentAnalysis {
        private int descriptionScore;
        private int pitchScore;
        private int financialsScore;
        private List<String> strengths;
        private List<String> improvements;
    }
}
