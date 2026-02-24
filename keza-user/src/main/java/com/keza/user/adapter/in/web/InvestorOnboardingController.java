package com.keza.user.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.user.application.dto.*;
import com.keza.user.application.usecase.InvestorOnboardingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investor/onboarding")
@RequiredArgsConstructor
public class InvestorOnboardingController {

    private final InvestorOnboardingUseCase onboardingUseCase;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvestorOnboardingStatusResponse>> getOnboardingStatus(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        InvestorOnboardingStatusResponse status = onboardingUseCase.getOnboardingStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    // --- Investment Account ---

    @PostMapping("/accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvestmentAccountResponse>> openInvestmentAccount(
            Authentication authentication,
            @Valid @RequestBody OpenInvestmentAccountRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        InvestmentAccountResponse response = onboardingUseCase.openInvestmentAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Investment account created"));
    }

    @GetMapping("/accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<InvestmentAccountResponse>>> getMyAccounts(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<InvestmentAccountResponse> accounts = onboardingUseCase.getMyAccounts(userId);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/accounts/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvestmentAccountResponse>> getAccountDetail(@PathVariable UUID id) {
        InvestmentAccountResponse response = onboardingUseCase.getAccountDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/admin/accounts/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InvestmentAccountResponse>> adminReviewAccount(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam boolean approved,
            @RequestParam(required = false) String notes) {
        UUID adminId = UUID.fromString(authentication.getName());
        InvestmentAccountResponse response = onboardingUseCase.adminReviewAccount(id, adminId, approved, notes);
        return ResponseEntity.ok(ApiResponse.success(response, approved ? "Account approved" : "Account suspended"));
    }

    // --- Accreditation ---

    @PostMapping("/accreditation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AccreditationResponse>> submitAccreditation(
            Authentication authentication,
            @Valid @RequestBody AccreditationRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        AccreditationResponse response = onboardingUseCase.submitAccreditation(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Accreditation submitted for review"));
    }

    @GetMapping("/accreditation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AccreditationResponse>>> getMyAccreditations(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<AccreditationResponse> accreditations = onboardingUseCase.getMyAccreditations(userId);
        return ResponseEntity.ok(ApiResponse.success(accreditations));
    }

    @PutMapping("/admin/accreditation/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccreditationResponse>> adminReviewAccreditation(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam boolean approved,
            @RequestParam(required = false) String notes) {
        UUID adminId = UUID.fromString(authentication.getName());
        AccreditationResponse response = onboardingUseCase.adminReviewAccreditation(id, adminId, approved, notes);
        return ResponseEntity.ok(ApiResponse.success(response, approved ? "Accreditation verified" : "Accreditation rejected"));
    }

    @GetMapping("/admin/accreditation/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccreditationResponse>>> getPendingAccreditations() {
        List<AccreditationResponse> pending = onboardingUseCase.getPendingAccreditations();
        return ResponseEntity.ok(ApiResponse.success(pending));
    }
}
