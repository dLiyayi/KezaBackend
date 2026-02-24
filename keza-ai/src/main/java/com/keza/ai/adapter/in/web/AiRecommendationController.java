package com.keza.ai.adapter.in.web;

import com.keza.ai.application.dto.RecommendationRequest;
import com.keza.ai.application.dto.RecommendationResponse;
import com.keza.ai.application.usecase.InvestmentRecommendationUseCaseBase;
import com.keza.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/recommendations")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final InvestmentRecommendationUseCaseBase investmentRecommendationUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendations(
            Authentication authentication,
            @RequestBody(required = false) RecommendationRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        if (request == null) {
            request = new RecommendationRequest();
        }
        RecommendationResponse response = investmentRecommendationUseCase.getRecommendations(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
