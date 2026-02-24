package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.FundDisbursementRequest;
import com.keza.campaign.application.dto.FundDisbursementResponse;
import com.keza.campaign.application.usecase.FundDisbursementUseCase;
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
@RequestMapping("/api/v1/campaigns/{campaignId}/disbursements")
@RequiredArgsConstructor
public class FundDisbursementController {

    private final FundDisbursementUseCase disbursementUseCase;

    @PostMapping("/request")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<FundDisbursementResponse>> requestRollingClose(
            @PathVariable UUID campaignId,
            Authentication authentication,
            @Valid @RequestBody FundDisbursementRequest request) {
        UUID issuerId = UUID.fromString(authentication.getName());
        FundDisbursementResponse response = disbursementUseCase.requestRollingClose(campaignId, issuerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Rolling close requested"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ISSUER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FundDisbursementResponse>>> getDisbursements(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<FundDisbursementResponse> disbursements = disbursementUseCase.getDisbursements(campaignId, userId);
        return ResponseEntity.ok(ApiResponse.success(disbursements));
    }

    @PutMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FundDisbursementResponse>> processDisbursement(
            @PathVariable UUID campaignId,
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam boolean approved) {
        UUID adminId = UUID.fromString(authentication.getName());
        FundDisbursementResponse response = disbursementUseCase.processDisbursement(id, adminId, approved);
        return ResponseEntity.ok(ApiResponse.success(response, approved ? "Disbursement approved" : "Disbursement rejected"));
    }
}
