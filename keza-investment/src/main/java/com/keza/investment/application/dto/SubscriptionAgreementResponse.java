package com.keza.investment.application.dto;

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
public class SubscriptionAgreementResponse {

    private UUID id;
    private UUID investmentId;
    private UUID userId;
    private UUID campaignId;
    private String agreementVersion;
    private Instant signedAt;
    private boolean riskAcknowledged;
    private boolean countersigned;
    private Instant countersignedAt;
    private String documentUrl;
    private Instant createdAt;
}
