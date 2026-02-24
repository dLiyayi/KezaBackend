package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.CampaignInterestRequest;
import com.keza.campaign.application.dto.CampaignInterestResponse;
import com.keza.campaign.application.dto.CampaignInterestSummary;
import com.keza.campaign.application.usecase.CampaignInterestUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/interest")
@RequiredArgsConstructor
public class CampaignInterestController {

    private final CampaignInterestUseCase interestUseCase;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CampaignInterestResponse>> registerInterest(
            @PathVariable UUID campaignId,
            Authentication authentication,
            @Valid @RequestBody CampaignInterestRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        CampaignInterestResponse response = interestUseCase.registerInterest(campaignId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Interest registered successfully"));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<CampaignInterestSummary>> getInterestSummary(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        UUID issuerId = UUID.fromString(authentication.getName());
        CampaignInterestSummary summary = interestUseCase.getInterestSummary(campaignId, issuerId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<PagedResponse<CampaignInterestResponse>>> getInterestRegistrations(
            @PathVariable UUID campaignId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID issuerId = UUID.fromString(authentication.getName());
        size = Math.min(size, 100);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var registrations = interestUseCase.getInterestRegistrations(campaignId, issuerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(registrations)));
    }

    @PostMapping("/notify")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<List<CampaignInterestResponse>>> notifyInterestedInvestors(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        UUID issuerId = UUID.fromString(authentication.getName());
        List<CampaignInterestResponse> notified = interestUseCase.notifyInterestedInvestors(campaignId, issuerId);
        return ResponseEntity.ok(ApiResponse.success(notified, "Notifications sent to " + notified.size() + " investors"));
    }
}
