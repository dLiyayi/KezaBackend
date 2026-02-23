package com.keza.investment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.investment.application.dto.AutoInvestPreferenceRequest;
import com.keza.investment.application.dto.AutoInvestPreferenceResponse;
import com.keza.investment.application.usecase.AutoInvestUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments/auto-invest")
@RequiredArgsConstructor
public class AutoInvestController {

    private final AutoInvestUseCase autoInvestUseCase;

    @PutMapping
    public ResponseEntity<ApiResponse<AutoInvestPreferenceResponse>> createOrUpdatePreference(
            Authentication authentication,
            @Valid @RequestBody AutoInvestPreferenceRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        AutoInvestPreferenceResponse response = autoInvestUseCase.createOrUpdatePreference(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Auto-invest preferences saved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AutoInvestPreferenceResponse>> getPreference(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        AutoInvestPreferenceResponse response = autoInvestUseCase.getPreference(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/enable")
    public ResponseEntity<ApiResponse<AutoInvestPreferenceResponse>> enableAutoInvest(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        AutoInvestPreferenceResponse response = autoInvestUseCase.toggleAutoInvest(userId, true);
        return ResponseEntity.ok(ApiResponse.success(response, "Auto-invest enabled"));
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<AutoInvestPreferenceResponse>> disableAutoInvest(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        AutoInvestPreferenceResponse response = autoInvestUseCase.toggleAutoInvest(userId, false);
        return ResponseEntity.ok(ApiResponse.success(response, "Auto-invest disabled"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deletePreference(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        autoInvestUseCase.deletePreference(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Auto-invest preferences deleted"));
    }
}
