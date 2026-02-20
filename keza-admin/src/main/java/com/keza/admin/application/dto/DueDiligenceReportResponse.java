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
public class DueDiligenceReportResponse {

    private UUID id;
    private UUID campaignId;
    private int totalChecks;
    private int passedChecks;
    private int failedChecks;
    private int naChecks;
    private BigDecimal overallScore;
    private String riskLevel;
    private String recommendation;
    private String summary;
    private UUID generatedBy;
    private Instant generatedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
