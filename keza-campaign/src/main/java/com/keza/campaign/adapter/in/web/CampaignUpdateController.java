package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.CampaignUpdateRequest;
import com.keza.campaign.application.dto.CampaignUpdateResponse;
import com.keza.campaign.application.usecase.CampaignUpdateUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/updates")
@RequiredArgsConstructor
public class CampaignUpdateController {

    private final CampaignUpdateUseCase campaignUpdateUseCase;

    @PostMapping
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<CampaignUpdateResponse>> createUpdate(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignUpdateRequest request,
            Authentication authentication) {
        UUID issuerId = (UUID) authentication.getPrincipal();
        CampaignUpdateResponse response = campaignUpdateUseCase.createUpdate(campaignId, issuerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Campaign update created"));
    }

    @PutMapping("/{updateId}")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<CampaignUpdateResponse>> editUpdate(
            @PathVariable UUID campaignId,
            @PathVariable UUID updateId,
            @Valid @RequestBody CampaignUpdateRequest request,
            Authentication authentication) {
        UUID issuerId = (UUID) authentication.getPrincipal();
        CampaignUpdateResponse response = campaignUpdateUseCase.editUpdate(campaignId, updateId, issuerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign update edited"));
    }

    @DeleteMapping("/{updateId}")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<Void>> deleteUpdate(
            @PathVariable UUID campaignId,
            @PathVariable UUID updateId,
            Authentication authentication) {
        UUID issuerId = (UUID) authentication.getPrincipal();
        campaignUpdateUseCase.deleteUpdate(campaignId, updateId, issuerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Campaign update deleted"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CampaignUpdateResponse>>> getUpdates(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "true") boolean publishedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<CampaignUpdateResponse> updates = campaignUpdateUseCase.getUpdatesForCampaign(campaignId, publishedOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(updates)));
    }
}
