package com.keza.user.domain.model;

import com.keza.common.domain.model.BaseEntity;
import com.keza.common.enums.AccreditationStatus;
import com.keza.common.enums.AccreditationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accreditation_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccreditationVerification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accreditation_type", nullable = false, length = 20)
    private AccreditationType accreditationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccreditationStatus status = AccreditationStatus.PENDING;

    @Column(name = "verification_method", length = 100)
    private String verificationMethod;

    @Column(name = "supporting_document_id")
    private UUID supportingDocumentId;

    @Column(name = "finra_crd_number", length = 50)
    private String finraCrdNumber;

    @Column(name = "verified_income", precision = 15, scale = 2)
    private BigDecimal verifiedIncome;

    @Column(name = "verified_net_worth", precision = 15, scale = 2)
    private BigDecimal verifiedNetWorth;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;
}
