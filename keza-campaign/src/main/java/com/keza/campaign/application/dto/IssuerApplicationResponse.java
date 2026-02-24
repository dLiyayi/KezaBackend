package com.keza.campaign.application.dto;

import com.keza.common.enums.BusinessStage;
import com.keza.common.enums.IssuerApplicationStatus;
import com.keza.common.enums.RegulationType;
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
public class IssuerApplicationResponse {

    private UUID id;
    private UUID userId;
    private String companyName;
    private String companyRegistrationNumber;
    private String companyWebsite;
    private String industry;
    private BusinessStage businessStage;
    private BigDecimal fundingGoal;
    private RegulationType regulationType;
    private String pitchSummary;
    private IssuerApplicationStatus status;
    private UUID accountManagerId;
    private UUID reviewerId;
    private String reviewNotes;
    private Instant reviewedAt;
    private Instant eligibleAt;
    private String rejectedReason;
    private List<IssuerOnboardingStepResponse> onboardingSteps;
    private Instant createdAt;
    private Instant updatedAt;
}
