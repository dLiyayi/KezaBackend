package com.keza.user.application.dto;

import com.keza.common.enums.AccreditationStatus;
import com.keza.common.enums.AccreditationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccreditationResponse {

    private UUID id;
    private UUID userId;
    private AccreditationType accreditationType;
    private AccreditationStatus status;
    private String verificationMethod;
    private UUID supportingDocumentId;
    private String finraCrdNumber;
    private BigDecimal verifiedIncome;
    private BigDecimal verifiedNetWorth;
    private Instant verifiedAt;
    private Instant expiresAt;
    private String rejectedReason;
    private Instant createdAt;
}
