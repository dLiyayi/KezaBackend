package com.keza.ai.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreResponse {

    private UUID campaignId;
    private int score;
    private String riskLevel;
    private List<String> strengths;
    private List<String> risks;
    private String recommendation;
}
