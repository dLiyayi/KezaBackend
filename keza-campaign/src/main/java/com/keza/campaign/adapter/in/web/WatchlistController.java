package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.CampaignResponse;
import com.keza.campaign.application.usecase.WatchlistUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistUseCase watchlistUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addToWatchlist(
            Authentication authentication,
            @RequestBody Map<String, UUID> request) {
        UUID userId = UUID.fromString(authentication.getName());
        UUID campaignId = request.get("campaign_id");
        watchlistUseCase.addToWatchlist(userId, campaignId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Campaign added to watchlist"));
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWatchlist(
            Authentication authentication,
            @PathVariable UUID campaignId) {
        UUID userId = UUID.fromString(authentication.getName());
        watchlistUseCase.removeFromWatchlist(userId, campaignId);
        return ResponseEntity.ok(ApiResponse.success(null, "Campaign removed from watchlist"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CampaignResponse>>> getUserWatchlist(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(authentication.getName());
        size = Math.min(size, 100);
        Page<CampaignResponse> watchlist = watchlistUseCase.getUserWatchlist(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(watchlist)));
    }

    @GetMapping("/check/{campaignId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isWatchlisted(
            Authentication authentication,
            @PathVariable UUID campaignId) {
        UUID userId = UUID.fromString(authentication.getName());
        boolean watchlisted = watchlistUseCase.isWatchlisted(userId, campaignId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("watchlisted", watchlisted)));
    }
}
