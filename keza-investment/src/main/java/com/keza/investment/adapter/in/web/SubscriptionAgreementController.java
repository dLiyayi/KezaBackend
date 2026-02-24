package com.keza.investment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.investment.application.dto.SignAgreementRequest;
import com.keza.investment.application.dto.SubscriptionAgreementResponse;
import com.keza.investment.application.usecase.SubscriptionAgreementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments/{investmentId}/agreement")
@RequiredArgsConstructor
public class SubscriptionAgreementController {

    private final SubscriptionAgreementUseCase agreementUseCase;

    @PostMapping("/sign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SubscriptionAgreementResponse>> signAgreement(
            @PathVariable UUID investmentId,
            Authentication authentication,
            @Valid @RequestBody SignAgreementRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        SubscriptionAgreementResponse response = agreementUseCase.signAgreement(investmentId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription agreement signed"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SubscriptionAgreementResponse>> getAgreement(
            @PathVariable UUID investmentId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        SubscriptionAgreementResponse response = agreementUseCase.getAgreement(investmentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{agreementId}/countersign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionAgreementResponse>> countersign(
            @PathVariable UUID investmentId,
            @PathVariable UUID agreementId,
            Authentication authentication,
            @RequestParam(required = false) String documentUrl) {
        UUID adminId = UUID.fromString(authentication.getName());
        SubscriptionAgreementResponse response = agreementUseCase.countersign(agreementId, adminId, documentUrl);
        return ResponseEntity.ok(ApiResponse.success(response, "Agreement countersigned"));
    }
}
