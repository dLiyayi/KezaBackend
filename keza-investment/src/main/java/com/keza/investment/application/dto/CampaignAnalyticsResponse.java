package com.keza.investment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignAnalyticsResponse {

    private BigDecimal totalRaised;
    private BigDecimal targetAmount;
    private BigDecimal fundingPercentage;
    private int totalInvestors;
    private long totalShares;
    private long soldShares;
    private BigDecimal averageInvestment;
    private BigDecimal largestInvestment;
    private BigDecimal smallestInvestment;
    private int daysRemaining;
    private int totalDays;
    private BigDecimal dailyFundingVelocity;
    private Map<String, BigDecimal> investmentsByStatus;
    private List<DailyInvestmentData> dailyInvestments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyInvestmentData {
        private String date;
        private BigDecimal amount;
        private int count;
    }
}
