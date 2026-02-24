package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.CampaignOptimizationResponse;

import java.util.UUID;

public abstract class CampaignOptimizationUseCaseBase {

    public abstract CampaignOptimizationResponse analyzeCampaign(UUID campaignId, UUID issuerId);
}
