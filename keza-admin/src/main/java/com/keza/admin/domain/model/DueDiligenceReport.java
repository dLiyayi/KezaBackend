package com.keza.admin.domain.model;

import com.keza.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "due_diligence_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DueDiligenceReport extends BaseEntity {

    @Column(name = "campaign_id", nullable = false, unique = true)
    private UUID campaignId;

    @Column(name = "total_checks", nullable = false)
    @Builder.Default
    private int totalChecks = 0;

    @Column(name = "passed_checks", nullable = false)
    @Builder.Default
    private int passedChecks = 0;

    @Column(name = "failed_checks", nullable = false)
    @Builder.Default
    private int failedChecks = 0;

    @Column(name = "na_checks", nullable = false)
    @Builder.Default
    private int naChecks = 0;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(length = 20)
    private String recommendation;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "generated_by")
    private UUID generatedBy;

    @Column(name = "generated_at")
    private Instant generatedAt;
}
