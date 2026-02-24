package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.RecommendationRequest;
import com.keza.ai.application.dto.RecommendationResponse;

import java.util.UUID;

public abstract class InvestmentRecommendationUseCaseBase {

    public abstract RecommendationResponse getRecommendations(UUID userId, RecommendationRequest request);
}
