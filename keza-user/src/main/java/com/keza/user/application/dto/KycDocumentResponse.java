package com.keza.user.application.dto;

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
public class KycDocumentResponse {

    private UUID id;
    private UUID userId;
    private String documentType;
    private String fileName;
    private long fileSize;
    private String contentType;
    private String status;
    private String rejectionReason;
    private String extractedData;
    private BigDecimal aiConfidenceScore;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
