package com.keza.campaign.application.dto;

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
public class CampaignUpdateResponse {

    private UUID id;
    private UUID campaignId;
    private String title;
    private String content;
    private UUID authorId;
    private boolean published;
    private Instant createdAt;
    private Instant updatedAt;
}
