package com.keza.campaign.domain.model;

import com.keza.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issuer_onboarding_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuerOnboardingStep extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(nullable = false)
    private Integer phase;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
