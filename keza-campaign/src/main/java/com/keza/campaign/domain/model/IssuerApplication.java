package com.keza.campaign.domain.model;

import com.keza.common.domain.model.SoftDeletableEntity;
import com.keza.common.enums.BusinessStage;
import com.keza.common.enums.IssuerApplicationStatus;
import com.keza.common.enums.RegulationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "issuer_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuerApplication extends SoftDeletableEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_registration_number", length = 100)
    private String companyRegistrationNumber;

    @Column(name = "company_website", length = 500)
    private String companyWebsite;

    @Column(length = 100)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_stage", nullable = false, length = 30)
    private BusinessStage businessStage;

    @Column(name = "funding_goal", nullable = false, precision = 15, scale = 2)
    private BigDecimal fundingGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "regulation_type", nullable = false, length = 20)
    private RegulationType regulationType;

    @Column(name = "pitch_summary", columnDefinition = "TEXT")
    private String pitchSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private IssuerApplicationStatus status = IssuerApplicationStatus.SUBMITTED;

    @Column(name = "account_manager_id")
    private UUID accountManagerId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "eligible_at")
    private Instant eligibleAt;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @OneToMany(mappedBy = "applicationId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<IssuerOnboardingStep> onboardingSteps = new ArrayList<>();
}
