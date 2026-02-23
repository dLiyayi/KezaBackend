package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.AdminCampaignResponse;
import com.keza.admin.application.usecase.AdminCampaignUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminCampaignController {

    private final AdminCampaignUseCase adminCampaignUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminCampaignResponse>>> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<AdminCampaignResponse> response = adminCampaignUseCase.listCampaigns(
                status, industry, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCampaignResponse>> getCampaign(@PathVariable UUID id) {
        AdminCampaignResponse response = adminCampaignUseCase.getCampaign(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/assign-reviewer")
    public ResponseEntity<ApiResponse<AdminCampaignResponse>> assignReviewer(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID reviewerId = UUID.fromString(authentication.getName());
        AdminCampaignResponse response = adminCampaignUseCase.assignReviewer(id, reviewerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Reviewer assigned successfully"));
    }
}
