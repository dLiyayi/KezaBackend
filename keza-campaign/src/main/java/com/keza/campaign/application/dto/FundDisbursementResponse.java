package com.keza.campaign.application.dto;

import com.keza.common.enums.DisbursementStatus;
import com.keza.common.enums.DisbursementType;
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
public class FundDisbursementResponse {

    private UUID id;
    private UUID campaignId;
    private BigDecimal amount;
    private DisbursementType disbursementType;
    private DisbursementStatus status;
    private String referenceNumber;
    private Instant requestedAt;
    private Instant processedAt;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
