package com.keza.campaign.domain.model;

import com.keza.common.domain.model.SoftDeletableEntity;
import com.keza.common.enums.DisbursementStatus;
import com.keza.common.enums.DisbursementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fund_disbursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundDisbursement extends SoftDeletableEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_type", nullable = false, length = 20)
    private DisbursementType disbursementType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisbursementStatus status = DisbursementStatus.PENDING;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
