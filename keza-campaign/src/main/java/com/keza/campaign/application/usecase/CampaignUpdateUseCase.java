package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.CampaignUpdateRequest;
import com.keza.campaign.application.dto.CampaignUpdateResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.CampaignUpdate;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.campaign.domain.port.out.CampaignUpdateRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignUpdateUseCase {

    private final CampaignUpdateRepository campaignUpdateRepository;
    private final CampaignRepository campaignRepository;

    @Transactional
    public CampaignUpdateResponse createUpdate(UUID campaignId, UUID authorId, CampaignUpdateRequest request) {
        Campaign campaign = findCampaignOrThrow(campaignId);
        validateOwnership(campaign, authorId);

        CampaignUpdate update = CampaignUpdate.builder()
                .campaignId(campaignId)
                .authorId(authorId)
                .title(request.getTitle())
                .content(request.getContent())
                .published(request.isPublished())
                .build();

        update = campaignUpdateRepository.save(update);
        log.info("Campaign update created: {} for campaign: {}", update.getId(), campaignId);
        return mapToResponse(update);
    }

    @Transactional
    public CampaignUpdateResponse editUpdate(UUID campaignId, UUID updateId, UUID authorId, CampaignUpdateRequest request) {
        Campaign campaign = findCampaignOrThrow(campaignId);
        validateOwnership(campaign, authorId);

        CampaignUpdate update = campaignUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignUpdate", updateId));

        if (!update.getCampaignId().equals(campaignId)) {
            throw new BusinessRuleException("INVALID_UPDATE", "Update does not belong to this campaign");
        }

        update.setTitle(request.getTitle());
        update.setContent(request.getContent());
        update.setPublished(request.isPublished());

        update = campaignUpdateRepository.save(update);
        log.info("Campaign update edited: {} for campaign: {}", updateId, campaignId);
        return mapToResponse(update);
    }

    @Transactional
    public void deleteUpdate(UUID campaignId, UUID updateId, UUID authorId) {
        Campaign campaign = findCampaignOrThrow(campaignId);
        validateOwnership(campaign, authorId);

        CampaignUpdate update = campaignUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignUpdate", updateId));

        if (!update.getCampaignId().equals(campaignId)) {
            throw new BusinessRuleException("INVALID_UPDATE", "Update does not belong to this campaign");
        }

        campaignUpdateRepository.delete(update);
        log.info("Campaign update deleted: {} for campaign: {}", updateId, campaignId);
    }

    @Transactional(readOnly = true)
    public Page<CampaignUpdateResponse> getUpdatesForCampaign(UUID campaignId, boolean publishedOnly, Pageable pageable) {
        findCampaignOrThrow(campaignId);

        Page<CampaignUpdate> updates;
        if (publishedOnly) {
            updates = campaignUpdateRepository.findByCampaignIdAndPublishedTrueOrderByCreatedAtDesc(campaignId, pageable);
        } else {
            updates = campaignUpdateRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable);
        }

        return updates.map(this::mapToResponse);
    }

    private Campaign findCampaignOrThrow(UUID campaignId) {
        return campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }

    private void validateOwnership(Campaign campaign, UUID issuerId) {
        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not own this campaign");
        }
    }

    private CampaignUpdateResponse mapToResponse(CampaignUpdate update) {
        return CampaignUpdateResponse.builder()
                .id(update.getId())
                .campaignId(update.getCampaignId())
                .title(update.getTitle())
                .content(update.getContent())
                .authorId(update.getAuthorId())
                .published(update.isPublished())
                .createdAt(update.getCreatedAt())
                .updatedAt(update.getUpdatedAt())
                .build();
    }
}
