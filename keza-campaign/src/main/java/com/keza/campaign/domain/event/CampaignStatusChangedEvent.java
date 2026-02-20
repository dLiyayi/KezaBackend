package com.keza.campaign.domain.event;

import com.keza.common.enums.CampaignStatus;

import java.util.UUID;

public record CampaignStatusChangedEvent(
        UUID campaignId,
        CampaignStatus oldStatus,
        CampaignStatus newStatus,
        UUID triggeredBy
) {}
