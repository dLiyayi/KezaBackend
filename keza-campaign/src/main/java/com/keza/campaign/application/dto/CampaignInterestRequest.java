package com.keza.campaign.application.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignInterestRequest {

    @DecimalMin(value = "0.01", message = "Intended amount must be positive")
    private BigDecimal intendedAmount;
}
