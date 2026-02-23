package com.keza.investment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoInvestPreferenceResponse {

    private UUID id;
    private UUID userId;
    private boolean enabled;
    private BigDecimal budgetAmount;
    private BigDecimal remainingBudget;
    private BigDecimal maxPerCampaign;
    private List<String> industries;
    private BigDecimal minTargetAmount;
    private BigDecimal maxTargetAmount;
    private List<String> offeringTypes;
    private Instant createdAt;
    private Instant updatedAt;
}
