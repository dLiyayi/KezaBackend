package com.keza.campaign.application.dto;

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
public class CampaignInterestResponse {

    private UUID id;
    private UUID campaignId;
    private UUID userId;
    private String email;
    private BigDecimal intendedAmount;
    private Instant registeredAt;
}
