package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.CampaignOptimizationResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class CampaignOptimizationUseCase extends CampaignOptimizationUseCaseBase {

    private final CampaignRepository campaignRepository;
    private final ChatModel chatModel;

    public CampaignOptimizationUseCase(CampaignRepository campaignRepository, ChatModel chatModel) {
        this.campaignRepository = campaignRepository;
        this.chatModel = chatModel;
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignOptimizationResponse analyzeCampaign(UUID campaignId, UUID issuerId) {
        log.info("AI analyzing campaign {} for issuer {}", campaignId, issuerId);

        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not own this campaign");
        }

        // Build campaign summary for AI analysis
        String campaignSummary = buildCampaignSummary(campaign);

        String aiAnalysis;
        try {
            aiAnalysis = ChatClient.builder(chatModel).build()
                    .prompt()
                    .system("You are CampaignPro AI, a campaign optimization advisor for an East African equity crowdfunding platform. " +
                            "Analyze campaigns and provide actionable suggestions to improve their success rate. " +
                            "Be specific, practical, and concise. Focus on the East African market context.")
                    .user("Analyze this campaign and provide specific improvement suggestions:\n\n" + campaignSummary)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI analysis failed for campaign {}: {}", campaignId, e.getMessage());
            aiAnalysis = null;
        }

        return buildOptimizationResponse(campaign, aiAnalysis);
    }

    private String buildCampaignSummary(Campaign campaign) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(campaign.getTitle()).append("\n");
        sb.append("Company: ").append(campaign.getCompanyName()).append("\n");
        sb.append("Industry: ").append(campaign.getIndustry()).append("\n");
        sb.append("Target: KES ").append(campaign.getTargetAmount()).append("\n");
        sb.append("Raised: KES ").append(campaign.getRaisedAmount()).append("\n");
        sb.append("Min Investment: KES ").append(campaign.getMinInvestment()).append("\n");
        sb.append("Share Price: KES ").append(campaign.getSharePrice()).append("\n");
        sb.append("Investors: ").append(campaign.getInvestorCount()).append("\n");
        sb.append("Status: ").append(campaign.getStatus()).append("\n");
        if (campaign.getDescription() != null) {
            sb.append("Description: ").append(campaign.getDescription(), 0,
                    Math.min(500, campaign.getDescription().length())).append("\n");
        }
        if (campaign.getTagline() != null) {
            sb.append("Tagline: ").append(campaign.getTagline()).append("\n");
        }
        return sb.toString();
    }

    private CampaignOptimizationResponse buildOptimizationResponse(Campaign campaign, String aiAnalysis) {
        List<CampaignOptimizationResponse.Suggestion> suggestions = new ArrayList<>();

        // Content suggestions
        if (campaign.getDescription() == null || campaign.getDescription().length() < 200) {
            suggestions.add(CampaignOptimizationResponse.Suggestion.builder()
                    .category("CONTENT").priority("HIGH")
                    .title("Expand campaign description")
                    .description("A detailed description (500+ words) significantly improves investor confidence. Include market opportunity, competitive advantages, and growth strategy.")
                    .build());
        }
        if (campaign.getTagline() == null || campaign.getTagline().isEmpty()) {
            suggestions.add(CampaignOptimizationResponse.Suggestion.builder()
                    .category("CONTENT").priority("MEDIUM")
                    .title("Add a compelling tagline")
                    .description("A clear tagline helps investors quickly understand your value proposition.")
                    .build());
        }
        if (campaign.getPitchVideoUrl() == null) {
            suggestions.add(CampaignOptimizationResponse.Suggestion.builder()
                    .category("CONTENT").priority("HIGH")
                    .title("Add a pitch video")
                    .description("Campaigns with videos raise 150% more on average. A 2-3 minute video explaining your vision is highly recommended.")
                    .build());
        }
        if (campaign.getRiskFactors() == null || campaign.getRiskFactors().isEmpty()) {
            suggestions.add(CampaignOptimizationResponse.Suggestion.builder()
                    .category("CONTENT").priority("MEDIUM")
                    .title("Add risk factors")
                    .description("Transparent risk disclosure builds investor trust and is required by CMA regulations.")
                    .build());
        }

        // Pricing suggestions
        if (campaign.getMinInvestment().compareTo(new BigDecimal("10000")) > 0) {
            suggestions.add(CampaignOptimizationResponse.Suggestion.builder()
                    .category("PRICING").priority("MEDIUM")
                    .title("Consider lowering minimum investment")
                    .description("A lower minimum investment (KES 5,000-10,000) attracts more retail investors and increases your investor count.")
                    .build());
        }

        // Calculate success probability
        double successProb = calculateSuccessProbability(campaign);

        // Content analysis scores
        int descScore = scoreDescription(campaign);
        int pitchScore = scorePitch(campaign);
        int financialsScore = scoreFinancials(campaign);

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        if (descScore >= 70) strengths.add("Good campaign description");
        else improvements.add("Campaign description needs more detail");
        if (pitchScore >= 70) strengths.add("Strong pitch content");
        else improvements.add("Add video and improve tagline");
        if (financialsScore >= 70) strengths.add("Financial projections included");
        else improvements.add("Add or improve financial projections");

        String overallAssessment = aiAnalysis != null ? aiAnalysis :
                String.format("Campaign has a %.0f%% estimated success probability. %d suggestions identified.",
                        successProb * 100, suggestions.size());

        return CampaignOptimizationResponse.builder()
                .campaignId(campaign.getId())
                .successProbability(successProb)
                .overallAssessment(overallAssessment)
                .suggestions(suggestions)
                .pricingAnalysis(CampaignOptimizationResponse.PricingAnalysis.builder()
                        .assessment(campaign.getSharePrice() != null ? "Share price is set" : "Share price not yet configured")
                        .suggestedMinInvestment(new BigDecimal("5000"))
                        .suggestedSharePrice(campaign.getSharePrice())
                        .rationale("KES 5,000 minimum aligns with Keza's mission of democratizing investment access")
                        .build())
                .contentAnalysis(CampaignOptimizationResponse.ContentAnalysis.builder()
                        .descriptionScore(descScore)
                        .pitchScore(pitchScore)
                        .financialsScore(financialsScore)
                        .strengths(strengths)
                        .improvements(improvements)
                        .build())
                .build();
    }

    private double calculateSuccessProbability(Campaign campaign) {
        double score = 0.3; // base

        if (campaign.getDescription() != null && campaign.getDescription().length() > 200) score += 0.1;
        if (campaign.getPitchVideoUrl() != null) score += 0.15;
        if (campaign.getFinancialProjections() != null) score += 0.1;
        if (campaign.getTeamMembers() != null) score += 0.1;
        if (campaign.getRiskFactors() != null) score += 0.05;
        if (campaign.getTagline() != null) score += 0.05;
        if (campaign.getCompanyWebsite() != null) score += 0.05;
        if (campaign.getInvestorCount() > 0) score += 0.1;

        return Math.min(0.95, score);
    }

    private int scoreDescription(Campaign campaign) {
        if (campaign.getDescription() == null) return 10;
        int len = campaign.getDescription().length();
        if (len > 1000) return 90;
        if (len > 500) return 70;
        if (len > 200) return 50;
        return 30;
    }

    private int scorePitch(Campaign campaign) {
        int score = 20;
        if (campaign.getTagline() != null) score += 30;
        if (campaign.getPitchVideoUrl() != null) score += 50;
        return Math.min(100, score);
    }

    private int scoreFinancials(Campaign campaign) {
        int score = 20;
        if (campaign.getFinancialProjections() != null) score += 40;
        if (campaign.getUseOfFunds() != null) score += 20;
        if (campaign.getRiskFactors() != null) score += 20;
        return Math.min(100, score);
    }
}
