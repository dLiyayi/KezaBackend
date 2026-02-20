package com.keza.campaign.domain.service;

import com.keza.campaign.domain.event.CampaignStatusChangedEvent;
import com.keza.campaign.domain.model.Campaign;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignStateMachine {

    private final ApplicationEventPublisher eventPublisher;

    private static final Map<CampaignStatus, Set<CampaignStatus>> VALID_TRANSITIONS;

    static {
        Map<CampaignStatus, Set<CampaignStatus>> transitions = new EnumMap<>(CampaignStatus.class);
        transitions.put(CampaignStatus.DRAFT, EnumSet.of(CampaignStatus.REVIEW, CampaignStatus.CANCELLED));
        transitions.put(CampaignStatus.REVIEW, EnumSet.of(CampaignStatus.LIVE, CampaignStatus.DRAFT, CampaignStatus.CANCELLED));
        transitions.put(CampaignStatus.LIVE, EnumSet.of(CampaignStatus.FUNDED, CampaignStatus.CLOSED, CampaignStatus.CANCELLED));
        transitions.put(CampaignStatus.FUNDED, EnumSet.of(CampaignStatus.CANCELLED));
        transitions.put(CampaignStatus.CLOSED, EnumSet.of(CampaignStatus.CANCELLED));
        transitions.put(CampaignStatus.CANCELLED, EnumSet.noneOf(CampaignStatus.class));
        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    public void transition(Campaign campaign, CampaignStatus newStatus, UUID triggeredBy) {
        CampaignStatus oldStatus = campaign.getStatus();

        if (oldStatus == newStatus) {
            throw new BusinessRuleException("INVALID_TRANSITION",
                    String.format("Campaign is already in %s status", oldStatus));
        }

        Set<CampaignStatus> allowed = VALID_TRANSITIONS.getOrDefault(oldStatus, EnumSet.noneOf(CampaignStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new BusinessRuleException("INVALID_TRANSITION",
                    String.format("Cannot transition from %s to %s", oldStatus, newStatus));
        }

        campaign.setStatus(newStatus);
        log.info("Campaign {} transitioned from {} to {} by {}",
                campaign.getId(), oldStatus, newStatus, triggeredBy);

        eventPublisher.publishEvent(new CampaignStatusChangedEvent(
                campaign.getId(), oldStatus, newStatus, triggeredBy));
    }
}
