package com.keza.admin.application.dto;

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
public class AdminKycDocumentResponse {

    private UUID id;
    private UUID userId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String documentType;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String status;
    private String rejectionReason;
    private BigDecimal aiConfidenceScore;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
