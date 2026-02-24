package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.IssuerApplicationRequest;
import com.keza.campaign.application.dto.IssuerApplicationResponse;
import com.keza.campaign.application.dto.IssuerOnboardingStepResponse;
import com.keza.campaign.application.usecase.IssuerOnboardingUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import com.keza.common.enums.IssuerApplicationStatus;
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
@RequestMapping("/api/v1/issuer/onboarding")
@RequiredArgsConstructor
public class IssuerOnboardingController {

    private final IssuerOnboardingUseCase onboardingUseCase;

    @PostMapping("/apply")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<IssuerApplicationResponse>> submitApplication(
            Authentication authentication,
            @Valid @RequestBody IssuerApplicationRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        IssuerApplicationResponse response = onboardingUseCase.submitApplication(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application submitted successfully"));
    }

    @GetMapping("/my-application")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<IssuerApplicationResponse>> getMyApplication(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        IssuerApplicationResponse response = onboardingUseCase.getMyApplication(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<IssuerApplicationResponse>>> getApplications(
            @RequestParam(required = false) IssuerApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var applications = onboardingUseCase.getApplicationsForReview(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(applications)));
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IssuerApplicationResponse>> getApplicationDetail(@PathVariable UUID id) {
        IssuerApplicationResponse response = onboardingUseCase.getApplicationDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/applications/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IssuerApplicationResponse>> reviewApplication(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam boolean approved,
            @RequestParam(required = false) String notes) {
        UUID adminId = UUID.fromString(authentication.getName());
        IssuerApplicationResponse response = onboardingUseCase.reviewApplication(id, adminId, approved, notes);
        return ResponseEntity.ok(ApiResponse.success(response, approved ? "Application approved" : "Application rejected"));
    }

    @PutMapping("/applications/{id}/assign-manager")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IssuerApplicationResponse>> assignAccountManager(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam UUID managerId) {
        UUID adminId = UUID.fromString(authentication.getName());
        IssuerApplicationResponse response = onboardingUseCase.assignAccountManager(id, adminId, managerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Account manager assigned"));
    }

    @PutMapping("/applications/{id}/steps/{stepId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IssuerOnboardingStepResponse>> updateOnboardingStep(
            @PathVariable UUID id,
            @PathVariable UUID stepId,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        IssuerOnboardingStepResponse response = onboardingUseCase.updateOnboardingStep(id, stepId, status, notes);
        return ResponseEntity.ok(ApiResponse.success(response, "Step updated"));
    }

    @GetMapping("/applications/{id}/progress")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<List<IssuerOnboardingStepResponse>>> getOnboardingProgress(@PathVariable UUID id) {
        List<IssuerOnboardingStepResponse> progress = onboardingUseCase.getOnboardingProgress(id);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    @DeleteMapping("/my-application")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<Void>> withdrawApplication(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        onboardingUseCase.withdrawApplication(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Application withdrawn"));
    }
}
