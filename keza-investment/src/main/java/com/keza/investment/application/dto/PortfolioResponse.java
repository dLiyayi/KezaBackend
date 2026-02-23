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
public class PortfolioResponse {

    private BigDecimal totalInvested;
    private int activeInvestments;
    private int pendingInvestments;
    private int cancelledInvestments;
    private int totalInvestmentCount;
    private Map<String, BigDecimal> sectorDistribution;
    private List<InvestmentResponse> investments;
}
