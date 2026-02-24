package com.keza.investment.domain.model;

import com.keza.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAgreement extends BaseEntity {

    @Column(name = "investment_id", nullable = false)
    private UUID investmentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "agreement_version", nullable = false, length = 50)
    private String agreementVersion;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "risk_acknowledged", nullable = false)
    @Builder.Default
    private boolean riskAcknowledged = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean countersigned = false;

    @Column(name = "countersigned_at")
    private Instant countersignedAt;

    @Column(name = "document_url", length = 500)
    private String documentUrl;
}
