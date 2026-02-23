package com.keza.admin.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvestmentAnalyticsResponse {

    private long totalInvestments;
    private long completedInvestments;
    private long pendingInvestments;
    private long cancelledInvestments;
    private BigDecimal totalInvestedAmount;
    private BigDecimal averageInvestmentAmount;
    private long uniqueInvestors;
}
