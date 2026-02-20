package com.keza.ai.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertResponse {

    private UUID id;
    private UUID userId;
    private String alertType;
    private String severity;
    private String description;
    private String details;
    private String status;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private String resolutionNotes;
    private Instant createdAt;
    private Instant updatedAt;
}
