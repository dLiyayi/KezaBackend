package com.keza.ai.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {

    private List<String> preferredIndustries;
    private BigDecimal budgetAmount;
    private String riskTolerance; // LOW, MEDIUM, HIGH
    private String language; // en, sw, fr
}
