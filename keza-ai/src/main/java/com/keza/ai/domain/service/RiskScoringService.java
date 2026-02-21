package com.keza.ai.domain.service;

import com.keza.ai.domain.port.out.RiskDataPort;
import com.keza.ai.domain.port.out.RiskDataPort.CampaignRiskData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for calculating risk scores for investment campaigns.
 * Uses a weighted scoring model across multiple dimensions to produce
 * a risk score between 1 (very low risk) and 10 (very high risk).
 *
 * <p>Scoring approach: 60% rule-based analysis + 40% LLM qualitative analysis
 * (when AI is enabled). Falls back to 100% rule-based when LLM is unavailable.</p>
 */
@Slf4j
@Service
public class RiskScoringService {

    // Weight factors for each scoring dimension
    private static final double WEIGHT_CAMPAIGN_COMPLETENESS = 0.15;
    private static final double WEIGHT_FOUNDER_CREDIBILITY = 0.20;
    private static final double WEIGHT_FINANCIAL_HEALTH = 0.25;
    private static final double WEIGHT_MARKET_POTENTIAL = 0.15;
    private static final double WEIGHT_REGULATORY_COMPLIANCE = 0.15;
    private static final double WEIGHT_COMMUNITY_TRUST = 0.10;

    // Blending weights for rule-based vs LLM scoring
    private static final double RULE_BASED_WEIGHT = 0.6;
    private static final double LLM_WEIGHT = 0.4;

    // Kenyan Shilling thresholds for target amount reasonableness
    private static final BigDecimal MIN_REASONABLE_TARGET = new BigDecimal("100000");    // KES 100K
    private static final BigDecimal MAX_REASONABLE_TARGET = new BigDecimal("100000000"); // KES 100M

    private final RiskDataPort riskDataPort;
    private final ChatModel chatModel;

    @Autowired
    public RiskScoringService(RiskDataPort riskDataPort,
                              @Autowired(required = false) ChatModel chatModel) {
        this.riskDataPort = riskDataPort;
        this.chatModel = chatModel;
    }

    /**
     * Calculates a comprehensive risk score for a campaign.
     * The score ranges from 1 (very low risk) to 10 (very high risk).
     *
     * @param campaignId the campaign to score
     * @return a RiskScoreResult containing the score, level, analysis, and recommendation
     */
    public RiskScoreResult calculateRiskScore(UUID campaignId) {
        log.info("Calculating risk score for campaign: {}", campaignId);

        CampaignRiskData data = riskDataPort.getCampaignData(campaignId);

        // Rule-based scoring for each dimension
        double campaignCompletenessScore = scoreCampaignCompleteness(data);
        double founderCredibilityScore = scoreFounderCredibility(data);
        double financialHealthScore = scoreFinancialHealth(data);
        double marketPotentialScore = scoreMarketPotential(data);
        double regulatoryComplianceScore = scoreRegulatoryCompliance(data);
        double communityTrustScore = scoreCommunityTrust(data);

        // Weighted average of rule-based scores
        double ruleBasedScore = (campaignCompletenessScore * WEIGHT_CAMPAIGN_COMPLETENESS)
                + (founderCredibilityScore * WEIGHT_FOUNDER_CREDIBILITY)
                + (financialHealthScore * WEIGHT_FINANCIAL_HEALTH)
                + (marketPotentialScore * WEIGHT_MARKET_POTENTIAL)
                + (regulatoryComplianceScore * WEIGHT_REGULATORY_COMPLIANCE)
                + (communityTrustScore * WEIGHT_COMMUNITY_TRUST);

        List<String> strengths = identifyStrengths(campaignCompletenessScore, founderCredibilityScore,
                financialHealthScore, marketPotentialScore, regulatoryComplianceScore, communityTrustScore);
        List<String> risks = identifyRisks(campaignCompletenessScore, founderCredibilityScore,
                financialHealthScore, marketPotentialScore, regulatoryComplianceScore, communityTrustScore);

        // Attempt LLM-enhanced scoring if ChatModel is available
        double finalScore;
        if (chatModel != null) {
            finalScore = blendWithLlmScore(data, ruleBasedScore, strengths, risks);
        } else {
            log.debug("ChatModel not available, using 100% rule-based scoring");
            finalScore = ruleBasedScore;
        }

        // Clamp to 1-10 range
        int score = (int) Math.round(Math.max(1, Math.min(10, finalScore)));

        String riskLevel = determineRiskLevel(score);
        String recommendation = generateRecommendation(score, riskLevel);

        log.info("Risk score for campaign {}: {} ({}) [rule-based={}, llm={}]",
                campaignId, score, riskLevel,
                String.format("%.2f", ruleBasedScore),
                chatModel != null ? "enabled" : "disabled");

        return new RiskScoreResult(score, riskLevel, strengths, risks, recommendation);
    }

    // ---- Scoring dimensions ----

    /**
     * Scores campaign completeness (weight 0.15).
     * Checks how thoroughly the campaign listing has been filled out.
     */
    double scoreCampaignCompleteness(CampaignRiskData data) {
        double score = 10.0;

        if (data.description() != null && data.description().length() > 100) {
            score -= 1;
        }
        if (data.hasFinancialProjections()) {
            score -= 2;
        }
        if (data.hasRiskFactors()) {
            score -= 1;
        }
        if (data.hasUseOfFunds()) {
            score -= 1;
        }
        if (data.hasTeamMembers()) {
            score -= 2;
        }
        if (data.hasPitchVideo()) {
            score -= 1;
        }
        if (data.mediaCount() >= 3) {
            score -= 1;
        }
        if (data.wizardStep() >= 6) {
            score -= 1;
        }

        return Math.max(1.0, score);
    }

    /**
     * Scores founder/issuer credibility (weight 0.20).
     * Evaluates the trustworthiness and track record of the campaign creator.
     */
    double scoreFounderCredibility(CampaignRiskData data) {
        double score = 10.0;

        if (data.issuerKycApproved()) {
            score -= 3;
        }
        if (data.companyRegistrationNumber() != null && !data.companyRegistrationNumber().isBlank()) {
            score -= 2;
        }
        if (data.companyWebsite() != null && !data.companyWebsite().isBlank()) {
            score -= 1;
        }
        if (data.issuerPreviousCampaigns() > 0) {
            score -= 2;
        }

        return Math.max(1.0, score);
    }

    /**
     * Scores financial health and viability (weight 0.25).
     * The most heavily weighted dimension, examining financial indicators.
     */
    double scoreFinancialHealth(CampaignRiskData data) {
        double score = 10.0;

        // Target amount reasonableness (KES 100K - 100M)
        if (data.targetAmount() != null
                && data.targetAmount().compareTo(MIN_REASONABLE_TARGET) >= 0
                && data.targetAmount().compareTo(MAX_REASONABLE_TARGET) <= 0) {
            score -= 2;
        }

        if (data.hasFinancialProjections()) {
            score -= 3;
        }
        if (data.hasUseOfFunds()) {
            score -= 2;
        }

        // Funding progress > 20%
        if (hasFundingProgress(data, 0.20)) {
            score -= 1;
        }

        // Investor count > 10 shows validation
        if (data.investorCount() > 10) {
            score -= 1;
        }

        return Math.max(1.0, score);
    }

    /**
     * Scores market potential and traction (weight 0.15).
     */
    double scoreMarketPotential(CampaignRiskData data) {
        double score = 10.0;

        if (data.industry() != null && !data.industry().isBlank()) {
            score -= 2;
        }
        if (data.description() != null && data.description().length() > 500) {
            score -= 2;
        }
        if (data.companyWebsite() != null && !data.companyWebsite().isBlank()) {
            score -= 1;
        }
        if (data.investorCount() > 20) {
            score -= 2;
        }
        if (data.updateCount() > 0) {
            score -= 1;
        }

        return Math.max(1.0, score);
    }

    /**
     * Scores regulatory compliance posture (weight 0.15).
     */
    double scoreRegulatoryCompliance(CampaignRiskData data) {
        double score = 10.0;

        if (data.companyRegistrationNumber() != null && !data.companyRegistrationNumber().isBlank()) {
            score -= 3;
        }
        if (data.companyAddress() != null && !data.companyAddress().isBlank()) {
            score -= 2;
        }
        if (data.issuerKycApproved()) {
            score -= 2;
        }
        if (data.hasRiskFactors()) {
            score -= 1;
        }

        return Math.max(1.0, score);
    }

    /**
     * Scores community trust and social proof (weight 0.10).
     */
    double scoreCommunityTrust(CampaignRiskData data) {
        double score = 10.0;

        // Tiered investor count scoring
        if (data.investorCount() > 50) {
            score -= 3;
        } else if (data.investorCount() > 20) {
            score -= 2;
        } else if (data.investorCount() > 5) {
            score -= 1;
        }

        if (data.updateCount() > 0) {
            score -= 2;
        }
        if (data.mediaCount() > 0) {
            score -= 1;
        }

        // Funding progress > 30%
        if (hasFundingProgress(data, 0.30)) {
            score -= 2;
        }

        return Math.max(1.0, score);
    }

    // ---- LLM integration ----

    /**
     * Blends the rule-based score with an LLM qualitative assessment.
     * Final score = 60% rule-based + 40% LLM score.
     * If LLM call fails, falls back to 100% rule-based.
     */
    private double blendWithLlmScore(CampaignRiskData data, double ruleBasedScore,
                                     List<String> strengths, List<String> risks) {
        try {
            String prompt = buildLlmPrompt(data, ruleBasedScore);

            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("LLM risk assessment response: {}", response);

            double llmScore = parseLlmScore(response);
            double blendedScore = (RULE_BASED_WEIGHT * ruleBasedScore) + (LLM_WEIGHT * llmScore);

            // Extract LLM insights and add to strengths/risks
            extractLlmInsights(response, strengths, risks);

            log.info("Blended score: rule-based={}, llm={}, final={}",
                    String.format("%.2f", ruleBasedScore),
                    String.format("%.2f", llmScore),
                    String.format("%.2f", blendedScore));

            return blendedScore;
        } catch (Exception e) {
            log.warn("LLM risk scoring failed, falling back to rule-based only: {}", e.getMessage());
            return ruleBasedScore;
        }
    }

    private String buildLlmPrompt(CampaignRiskData data, double ruleBasedScore) {
        return """
                You are a risk assessment analyst for a crowdfunding platform in Kenya.
                Evaluate the following campaign and provide a risk score from 1 (very low risk) to 10 (very high risk).

                Campaign Summary:
                - Title: %s
                - Industry: %s
                - Company: %s
                - Company Registration: %s
                - Company Website: %s
                - Target Amount: KES %s
                - Raised Amount: KES %s
                - Investor Count: %d
                - Has Financial Projections: %s
                - Has Risk Factors Disclosed: %s
                - Has Use of Funds: %s
                - Has Team Members: %s
                - Has Pitch Video: %s
                - Media Count: %d
                - Updates Posted: %d
                - Issuer KYC Approved: %s
                - Issuer Previous Campaigns: %d
                - Description Length: %d characters
                - Wizard Completion Step: %d/6

                Our rule-based analysis produced a score of %.1f/10.

                Respond in this exact format:
                SCORE: [number 1-10]
                STRENGTHS: [comma-separated list of 1-3 key strengths]
                RISKS: [comma-separated list of 1-3 key risks]
                SUMMARY: [one sentence overall assessment]
                """.formatted(
                nullSafe(data.title()),
                nullSafe(data.industry()),
                nullSafe(data.companyName()),
                data.companyRegistrationNumber() != null ? "Yes" : "No",
                data.companyWebsite() != null ? "Yes" : "No",
                data.targetAmount() != null ? data.targetAmount().toPlainString() : "N/A",
                data.raisedAmount() != null ? data.raisedAmount().toPlainString() : "0",
                data.investorCount(),
                data.hasFinancialProjections(),
                data.hasRiskFactors(),
                data.hasUseOfFunds(),
                data.hasTeamMembers(),
                data.hasPitchVideo(),
                data.mediaCount(),
                data.updateCount(),
                data.issuerKycApproved(),
                data.issuerPreviousCampaigns(),
                data.description() != null ? data.description().length() : 0,
                data.wizardStep(),
                ruleBasedScore
        );
    }

    private double parseLlmScore(String response) {
        try {
            for (String line : response.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.toUpperCase().startsWith("SCORE:")) {
                    String scoreStr = trimmed.substring(6).trim();
                    // Extract first number found
                    StringBuilder num = new StringBuilder();
                    for (char c : scoreStr.toCharArray()) {
                        if (Character.isDigit(c) || c == '.') {
                            num.append(c);
                        } else if (!num.isEmpty()) {
                            break;
                        }
                    }
                    if (!num.isEmpty()) {
                        double score = Double.parseDouble(num.toString());
                        return Math.max(1.0, Math.min(10.0, score));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse LLM score from response: {}", e.getMessage());
        }
        return 5.0; // Default to moderate if parsing fails
    }

    private void extractLlmInsights(String response, List<String> strengths, List<String> risks) {
        try {
            for (String line : response.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.toUpperCase().startsWith("STRENGTHS:")) {
                    String insightsStr = trimmed.substring(10).trim();
                    if (!insightsStr.isBlank()) {
                        for (String insight : insightsStr.split(",")) {
                            String clean = insight.trim();
                            if (!clean.isEmpty()) {
                                strengths.add("[AI] " + clean);
                            }
                        }
                    }
                } else if (trimmed.toUpperCase().startsWith("RISKS:")) {
                    String insightsStr = trimmed.substring(6).trim();
                    if (!insightsStr.isBlank()) {
                        for (String insight : insightsStr.split(",")) {
                            String clean = insight.trim();
                            if (!clean.isEmpty()) {
                                risks.add("[AI] " + clean);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract LLM insights: {}", e.getMessage());
        }
    }

    // ---- Utility methods ----

    private boolean hasFundingProgress(CampaignRiskData data, double threshold) {
        if (data.targetAmount() == null || data.targetAmount().compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        if (data.raisedAmount() == null) {
            return false;
        }
        double progress = data.raisedAmount().doubleValue() / data.targetAmount().doubleValue();
        return progress > threshold;
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
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
