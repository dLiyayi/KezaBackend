package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.DueDiligenceCheckResponse;
import com.keza.admin.application.dto.DueDiligenceReportResponse;
import com.keza.admin.application.dto.UpdateCheckRequest;
import com.keza.admin.application.usecase.DueDiligenceUseCase;
import com.keza.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/due-diligence")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class DueDiligenceController {

    private final DueDiligenceUseCase dueDiligenceUseCase;

    @GetMapping("/campaigns/{campaignId}/checks")
    public ResponseEntity<ApiResponse<List<DueDiligenceCheckResponse>>> getChecksForCampaign(
            @PathVariable UUID campaignId) {
        List<DueDiligenceCheckResponse> checks = dueDiligenceUseCase.getChecksForCampaign(campaignId);
        return ResponseEntity.ok(ApiResponse.success(checks));
    }

    @PutMapping("/checks/{checkId}")
    public ResponseEntity<ApiResponse<DueDiligenceCheckResponse>> updateCheck(
            @PathVariable UUID checkId,
            @Valid @RequestBody UpdateCheckRequest request,
            Authentication authentication) {
        UUID reviewerId = (UUID) authentication.getPrincipal();
        DueDiligenceCheckResponse response = dueDiligenceUseCase.updateCheck(checkId, request, reviewerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Check updated successfully"));
    }

    @PostMapping("/campaigns/{campaignId}/report")
    public ResponseEntity<ApiResponse<DueDiligenceReportResponse>> generateReport(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        DueDiligenceReportResponse response = dueDiligenceUseCase.generateReport(campaignId, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Report generated successfully"));
    }

    @GetMapping("/campaigns/{campaignId}/report")
    public ResponseEntity<ApiResponse<DueDiligenceReportResponse>> getReport(
            @PathVariable UUID campaignId) {
        DueDiligenceReportResponse response = dueDiligenceUseCase.getReport(campaignId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
