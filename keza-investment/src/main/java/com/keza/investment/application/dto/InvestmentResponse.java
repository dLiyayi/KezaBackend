package com.keza.investment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvestmentResponse {

    private UUID id;
    private UUID investorId;
    private UUID campaignId;
    private BigDecimal amount;
    private long shares;
    private BigDecimal sharePrice;
    private String status;
    private String paymentMethod;
    private Instant coolingOffExpiresAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;
}
