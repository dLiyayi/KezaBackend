package com.keza.campaign.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.campaign.domain.service.CampaignStateMachine;
import com.keza.common.enums.CampaignStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final CampaignRepository campaignRepository;
    private final CampaignStateMachine stateMachine;

    private static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCloseExpired() {
        List<Campaign> expired = campaignRepository.findByStatusAndEndDateBefore(
                CampaignStatus.LIVE, Instant.now());

        for (Campaign campaign : expired) {
            try {
                stateMachine.transition(campaign, CampaignStatus.CLOSED, SYSTEM_USER);
                campaignRepository.save(campaign);
                log.info("Auto-closed expired campaign: {}", campaign.getId());
            } catch (Exception e) {
                log.error("Failed to auto-close campaign {}: {}", campaign.getId(), e.getMessage());
            }
        }

        if (!expired.isEmpty()) {
            log.info("Auto-close check complete: {} campaigns closed", expired.size());
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoFund() {
        List<Campaign> liveCampaigns = campaignRepository.findByStatusAndEndDateBefore(
                CampaignStatus.LIVE, Instant.now().plusSeconds(86400 * 365));

        int funded = 0;
        for (Campaign campaign : liveCampaigns) {
            if (campaign.getRaisedAmount().compareTo(campaign.getTargetAmount()) >= 0) {
                try {
                    campaign.setFundedAt(Instant.now());
                    stateMachine.transition(campaign, CampaignStatus.FUNDED, SYSTEM_USER);
                    campaignRepository.save(campaign);
                    funded++;
                    log.info("Auto-funded campaign: {} (raised: {}, target: {})",
                            campaign.getId(), campaign.getRaisedAmount(), campaign.getTargetAmount());
                } catch (Exception e) {
                    log.error("Failed to auto-fund campaign {}: {}", campaign.getId(), e.getMessage());
                }
            }
        }

        if (funded > 0) {
            log.info("Auto-fund check complete: {} campaigns funded", funded);
        }
    }
}
