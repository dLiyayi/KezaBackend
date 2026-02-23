package com.keza.investment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class AutoInvestPreferenceRequest {

    @NotNull(message = "Budget amount is required")
    @DecimalMin(value = "1000", message = "Budget must be at least 1,000")
    private BigDecimal budgetAmount;

    @DecimalMin(value = "1000", message = "Max per campaign must be at least 1,000")
    private BigDecimal maxPerCampaign;

    private List<String> industries;

    private BigDecimal minTargetAmount;

    private BigDecimal maxTargetAmount;

    private List<String> offeringTypes;
}
