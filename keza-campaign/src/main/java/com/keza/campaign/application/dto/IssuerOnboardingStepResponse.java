package com.keza.campaign.application.dto;

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
public class IssuerOnboardingStepResponse {

    private UUID id;
    private Integer phase;
    private String stepName;
    private String status;
    private Instant completedAt;
    private UUID completedBy;
    private String notes;
}
