package com.keza.admin.application.dto;

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
public class DueDiligenceCheckResponse {

    private UUID id;
    private UUID campaignId;
    private String category;
    private String checkName;
    private String description;
    private String status;
    private String notes;
    private UUID checkedBy;
    private Instant checkedAt;
    private String aiResult;
    private BigDecimal aiConfidence;
    private BigDecimal weight;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
