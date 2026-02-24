package com.keza.investment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.investment.application.dto.CampaignAnalyticsResponse;
import com.keza.investment.application.usecase.CampaignAnalyticsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignAnalyticsController {

    private final CampaignAnalyticsUseCase campaignAnalyticsUseCase;

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasRole('ISSUER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CampaignAnalyticsResponse>> getCampaignAnalytics(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID issuerId = UUID.fromString(authentication.getName());
        CampaignAnalyticsResponse response = campaignAnalyticsUseCase.getCampaignAnalytics(id, issuerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
