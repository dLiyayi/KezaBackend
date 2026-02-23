package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.CampaignResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.Watchlist;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.campaign.domain.port.out.WatchlistRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistUseCase {

    private final WatchlistRepository watchlistRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUseCase campaignUseCase;

    @Transactional
    public void addToWatchlist(UUID userId, UUID campaignId) {
        campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (watchlistRepository.existsByUserIdAndCampaignId(userId, campaignId)) {
            throw new BusinessRuleException("ALREADY_WATCHLISTED", "Campaign is already in your watchlist");
        }

        Watchlist watchlist = Watchlist.builder()
                .userId(userId)
                .campaignId(campaignId)
                .build();

        watchlistRepository.save(watchlist);
        log.info("User {} added campaign {} to watchlist", userId, campaignId);
    }

    @Transactional
    public void removeFromWatchlist(UUID userId, UUID campaignId) {
        if (!watchlistRepository.existsByUserIdAndCampaignId(userId, campaignId)) {
            throw new ResourceNotFoundException("Watchlist entry", "campaignId", campaignId.toString());
        }

        watchlistRepository.deleteByUserIdAndCampaignId(userId, campaignId);
        log.info("User {} removed campaign {} from watchlist", userId, campaignId);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> getUserWatchlist(UUID userId, Pageable pageable) {
        Page<Watchlist> watchlistPage = watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<UUID> campaignIds = watchlistPage.getContent().stream()
                .map(Watchlist::getCampaignId)
                .collect(Collectors.toList());

        if (campaignIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Map<UUID, Campaign> campaignMap = campaignRepository.findAllById(campaignIds).stream()
                .collect(Collectors.toMap(Campaign::getId, c -> c));

        List<CampaignResponse> responses = watchlistPage.getContent().stream()
                .map(w -> campaignMap.get(w.getCampaignId()))
                .filter(Objects::nonNull)
                .map(campaignUseCase::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, watchlistPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public boolean isWatchlisted(UUID userId, UUID campaignId) {
        return watchlistRepository.existsByUserIdAndCampaignId(userId, campaignId);
    }
}
