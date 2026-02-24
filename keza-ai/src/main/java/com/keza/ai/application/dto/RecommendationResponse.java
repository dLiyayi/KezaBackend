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
public class RecommendationResponse {

    private List<CampaignRecommendation> recommendations;
    private String portfolioInsight;
    private String marketInsight;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignRecommendation {
        private UUID campaignId;
        private String title;
        private String companyName;
        private String industry;
        private BigDecimal targetAmount;
        private BigDecimal raisedAmount;
        private BigDecimal fundingPercentage;
        private BigDecimal suggestedAmount;
        private String reason;
        private double matchScore;
    }
}
