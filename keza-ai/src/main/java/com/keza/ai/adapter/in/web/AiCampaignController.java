package com.keza.ai.adapter.in.web;

import com.keza.ai.application.dto.CampaignOptimizationResponse;
import com.keza.ai.application.usecase.CampaignOptimizationUseCaseBase;
import com.keza.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/campaigns")
@RequiredArgsConstructor
public class AiCampaignController {

    private final CampaignOptimizationUseCaseBase campaignOptimizationUseCase;

    @GetMapping("/{campaignId}/optimize")
    @PreAuthorize("hasRole('ISSUER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CampaignOptimizationResponse>> optimizeCampaign(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        UUID issuerId = UUID.fromString(authentication.getName());
        CampaignOptimizationResponse response = campaignOptimizationUseCase.analyzeCampaign(campaignId, issuerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
