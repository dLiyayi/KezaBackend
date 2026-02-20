package com.keza.admin.domain.model;

import com.keza.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "due_diligence_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DueDiligenceCheck extends BaseEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "check_name", nullable = false)
    private String checkName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DDCheckStatus status = DDCheckStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "checked_by")
    private UUID checkedBy;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "ai_result", length = 20)
    private String aiResult;

    @Column(name = "ai_confidence", precision = 5, scale = 4)
    private BigDecimal aiConfidence;

    @Column(precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    public void markAs(DDCheckStatus newStatus, String reviewNotes, UUID reviewerId) {
        this.status = newStatus;
        this.notes = reviewNotes;
        this.checkedBy = reviewerId;
        this.checkedAt = Instant.now();
    }
}
