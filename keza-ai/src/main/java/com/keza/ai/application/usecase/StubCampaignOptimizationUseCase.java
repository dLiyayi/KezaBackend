package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.CampaignOptimizationResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubCampaignOptimizationUseCase extends CampaignOptimizationUseCaseBase {

    private final CampaignRepository campaignRepository;

    public StubCampaignOptimizationUseCase(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignOptimizationResponse analyzeCampaign(UUID campaignId, UUID issuerId) {
        log.info("Stub campaign optimization for {} (AI disabled)", campaignId);

        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not own this campaign");
        }

        return CampaignOptimizationResponse.builder()
                .campaignId(campaignId)
                .successProbability(0.0)
                .overallAssessment("AI campaign optimization is currently unavailable. Enable AI features for detailed analysis.")
                .suggestions(Collections.emptyList())
                .contentAnalysis(CampaignOptimizationResponse.ContentAnalysis.builder()
                        .descriptionScore(0)
                        .pitchScore(0)
                        .financialsScore(0)
                        .strengths(Collections.emptyList())
                        .improvements(List.of("Enable AI for content analysis"))
                        .build())
                .build();
    }
}
