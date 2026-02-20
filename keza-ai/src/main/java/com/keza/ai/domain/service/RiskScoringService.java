package com.keza.ai.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for calculating risk scores for investment campaigns.
 * Uses a weighted scoring model across multiple dimensions to produce
 * a risk score between 1 (very low risk) and 10 (very high risk).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoringService {

    // Weight factors for each scoring dimension
    private static final double WEIGHT_CAMPAIGN_COMPLETENESS = 0.15;
    private static final double WEIGHT_FOUNDER_CREDIBILITY = 0.20;
    private static final double WEIGHT_FINANCIAL_HEALTH = 0.25;
    private static final double WEIGHT_MARKET_POTENTIAL = 0.15;
    private static final double WEIGHT_REGULATORY_COMPLIANCE = 0.15;
    private static final double WEIGHT_COMMUNITY_TRUST = 0.10;

    /**
     * Calculates a comprehensive risk score for a campaign.
     * The score ranges from 1 (very low risk) to 10 (very high risk).
     *
     * @param campaignId the campaign to score
     * @return a RiskScoreResult containing the score, level, analysis, and recommendation
     */
    public RiskScoreResult calculateRiskScore(UUID campaignId) {
        log.info("Calculating risk score for campaign: {}", campaignId);

        // In a full implementation, each scorer would query real data.
        // For now, we demonstrate the weighted scoring structure with placeholder scoring.

        double campaignCompletenessScore = scoreCampaignCompleteness(campaignId);
        double founderCredibilityScore = scoreFounderCredibility(campaignId);
        double financialHealthScore = scoreFinancialHealth(campaignId);
        double marketPotentialScore = scoreMarketPotential(campaignId);
        double regulatoryComplianceScore = scoreRegulatoryCompliance(campaignId);
        double communityTrustScore = scoreCommunityTrust(campaignId);

        // Weighted average
        double rawScore = (campaignCompletenessScore * WEIGHT_CAMPAIGN_COMPLETENESS)
                + (founderCredibilityScore * WEIGHT_FOUNDER_CREDIBILITY)
                + (financialHealthScore * WEIGHT_FINANCIAL_HEALTH)
                + (marketPotentialScore * WEIGHT_MARKET_POTENTIAL)
                + (regulatoryComplianceScore * WEIGHT_REGULATORY_COMPLIANCE)
                + (communityTrustScore * WEIGHT_COMMUNITY_TRUST);

        // Clamp to 1-10 range
        int score = (int) Math.round(Math.max(1, Math.min(10, rawScore)));

        String riskLevel = determineRiskLevel(score);
        List<String> strengths = identifyStrengths(campaignCompletenessScore, founderCredibilityScore,
                financialHealthScore, marketPotentialScore, regulatoryComplianceScore, communityTrustScore);
        List<String> risks = identifyRisks(campaignCompletenessScore, founderCredibilityScore,
                financialHealthScore, marketPotentialScore, regulatoryComplianceScore, communityTrustScore);
        String recommendation = generateRecommendation(score, riskLevel);

        log.info("Risk score for campaign {}: {} ({})", campaignId, score, riskLevel);

        return new RiskScoreResult(score, riskLevel, strengths, risks, recommendation);
    }

    // ---- Scoring dimensions (placeholders for real data integration) ----

    private double scoreCampaignCompleteness(UUID campaignId) {
        // Check: description length, images, financial projections, team info, milestones
        // Placeholder: returns a moderate score
        return 5.0;
    }

    private double scoreFounderCredibility(UUID campaignId) {
        // Check: KYC verified, previous campaigns, social proof, LinkedIn, track record
        return 5.0;
    }

    private double scoreFinancialHealth(UUID campaignId) {
        // Check: revenue data, burn rate, valuation reasonableness, use of funds clarity
        return 5.0;
    }

    private double scoreMarketPotential(UUID campaignId) {
        // Check: market size, competition analysis, traction metrics, growth rate
        return 5.0;
    }

    private double scoreRegulatoryCompliance(UUID campaignId) {
        // Check: business registration, tax compliance, required licenses, legal structure
        return 5.0;
    }

    private double scoreCommunityTrust(UUID campaignId) {
        // Check: investor count, social shares, comments, repeat investors
        return 5.0;
    }

    // ---- Analysis helpers ----

    private String determineRiskLevel(int score) {
        if (score <= 2) return "VERY_LOW";
        if (score <= 4) return "LOW";
        if (score <= 6) return "MEDIUM";
        if (score <= 8) return "HIGH";
        return "VERY_HIGH";
    }

    private List<String> identifyStrengths(double... scores) {
        List<String> strengths = new ArrayList<>();
        String[] dimensions = {
                "Campaign completeness", "Founder credibility", "Financial health",
                "Market potential", "Regulatory compliance", "Community trust"
        };

        for (int i = 0; i < scores.length; i++) {
            if (scores[i] <= 3.0) { // Lower score = lower risk = strength
                strengths.add(dimensions[i] + " is strong (low risk)");
            }
        }

        if (strengths.isEmpty()) {
            strengths.add("No standout strengths identified - further due diligence recommended");
        }

        return strengths;
    }

    private List<String> identifyRisks(double... scores) {
        List<String> risks = new ArrayList<>();
        String[] dimensions = {
                "Campaign completeness", "Founder credibility", "Financial health",
                "Market potential", "Regulatory compliance", "Community trust"
        };

        for (int i = 0; i < scores.length; i++) {
            if (scores[i] >= 7.0) { // Higher score = higher risk
                risks.add(dimensions[i] + " is a concern (high risk)");
            }
        }

        if (risks.isEmpty()) {
            risks.add("No critical risks identified at this time");
        }

        return risks;
    }

    private String generateRecommendation(int score, String riskLevel) {
        return switch (riskLevel) {
            case "VERY_LOW" -> "This campaign shows strong fundamentals across all dimensions. Standard due diligence is sufficient.";
            case "LOW" -> "This campaign presents a favorable risk profile. Proceed with standard review procedures.";
            case "MEDIUM" -> "This campaign has a moderate risk profile. Enhanced due diligence is recommended before approval.";
            case "HIGH" -> "This campaign presents elevated risk. Thorough review and additional documentation should be requested before approval.";
            case "VERY_HIGH" -> "This campaign presents significant risk factors. Senior review and possible rejection recommended unless compelling evidence is provided.";
            default -> "Unable to determine recommendation. Manual review required.";
        };
    }

    /**
     * Immutable result record for a campaign risk score assessment.
     */
    public record RiskScoreResult(
            int score,
            String riskLevel,
            List<String> strengths,
            List<String> risks,
            String recommendation
    ) {}
}
