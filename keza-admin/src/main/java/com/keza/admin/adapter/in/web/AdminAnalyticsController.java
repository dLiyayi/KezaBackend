package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.*;
import com.keza.admin.application.usecase.AdminAnalyticsUseCase;
import com.keza.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsUseCase adminAnalyticsUseCase;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PlatformOverviewResponse>> getPlatformOverview() {
        PlatformOverviewResponse response = adminAnalyticsUseCase.getPlatformOverview();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/investments")
    public ResponseEntity<ApiResponse<InvestmentAnalyticsResponse>> getInvestmentAnalytics() {
        InvestmentAnalyticsResponse response = adminAnalyticsUseCase.getInvestmentAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<CampaignAnalyticsResponse>> getCampaignAnalytics() {
        CampaignAnalyticsResponse response = adminAnalyticsUseCase.getCampaignAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUserAnalytics() {
        UserAnalyticsResponse response = adminAnalyticsUseCase.getUserAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
