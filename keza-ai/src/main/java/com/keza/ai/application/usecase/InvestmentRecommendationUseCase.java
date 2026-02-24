package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.RecommendationRequest;
import com.keza.ai.application.dto.RecommendationResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.CampaignStatus;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class InvestmentRecommendationUseCase extends InvestmentRecommendationUseCaseBase {

    private final CampaignRepository campaignRepository;
    private final InvestmentRepository investmentRepository;
    private final ChatModel chatModel;

    public InvestmentRecommendationUseCase(CampaignRepository campaignRepository,
                                            InvestmentRepository investmentRepository,
                                            ChatModel chatModel) {
        this.campaignRepository = campaignRepository;
        this.investmentRepository = investmentRepository;
        this.chatModel = chatModel;
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(UUID userId, RecommendationRequest request) {
        log.info("Generating investment recommendations for user {}", userId);

        // Get user's existing investments to understand preferences
        Page<Investment> existingInvestments = investmentRepository.findByInvestorIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")));

        Set<UUID> existingCampaignIds = existingInvestments.getContent().stream()
                .map(Investment::getCampaignId)
                .collect(Collectors.toSet());

        // Find live campaigns the user hasn't invested in
        Specification<Campaign> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"), CampaignStatus.LIVE));
            predicates.add(cb.equal(root.get("deleted"), false));
            if (!existingCampaignIds.isEmpty()) {
                predicates.add(cb.not(root.get("id").in(existingCampaignIds)));
            }
            if (request.getPreferredIndustries() != null && !request.getPreferredIndustries().isEmpty()) {
                predicates.add(root.get("industry").in(request.getPreferredIndustries()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<Campaign> candidates = campaignRepository.findAll(spec,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "raisedAmount"))).getContent();

        List<RecommendationResponse.CampaignRecommendation> recommendations = candidates.stream()
                .map(campaign -> buildRecommendation(campaign, request))
                .sorted(Comparator.comparingDouble(RecommendationResponse.CampaignRecommendation::getMatchScore).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Generate AI insights
        String portfolioInsight = generatePortfolioInsight(existingInvestments.getContent(), request);
        String marketInsight = generateMarketInsight(candidates, request);

        return RecommendationResponse.builder()
                .recommendations(recommendations)
                .portfolioInsight(portfolioInsight)
                .marketInsight(marketInsight)
                .build();
    }

    private RecommendationResponse.CampaignRecommendation buildRecommendation(Campaign campaign, RecommendationRequest request) {
        double matchScore = calculateMatchScore(campaign, request);

        BigDecimal fundingPercentage = BigDecimal.ZERO;
        if (campaign.getTargetAmount().signum() > 0) {
            fundingPercentage = campaign.getRaisedAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(campaign.getTargetAmount(), 2, RoundingMode.HALF_UP);
        }

        BigDecimal suggestedAmount = campaign.getMinInvestment();
        if (request.getBudgetAmount() != null) {
            suggestedAmount = request.getBudgetAmount()
                    .multiply(BigDecimal.valueOf(0.2))
                    .max(campaign.getMinInvestment());
            if (campaign.getMaxInvestment() != null) {
                suggestedAmount = suggestedAmount.min(campaign.getMaxInvestment());
            }
        }

        String reason = generateReason(campaign, matchScore);

        return RecommendationResponse.CampaignRecommendation.builder()
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .companyName(campaign.getCompanyName())
                .industry(campaign.getIndustry())
                .targetAmount(campaign.getTargetAmount())
                .raisedAmount(campaign.getRaisedAmount())
                .fundingPercentage(fundingPercentage)
                .suggestedAmount(suggestedAmount)
                .reason(reason)
                .matchScore(matchScore)
                .build();
    }

    private double calculateMatchScore(Campaign campaign, RecommendationRequest request) {
        double score = 0.5;

        // Industry preference match
        if (request.getPreferredIndustries() != null && campaign.getIndustry() != null
                && request.getPreferredIndustries().contains(campaign.getIndustry())) {
            score += 0.2;
        }

        // Funding momentum (campaigns with more funding are more popular)
        if (campaign.getTargetAmount().signum() > 0) {
            double fundingRatio = campaign.getRaisedAmount()
                    .divide(campaign.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .doubleValue();
            score += Math.min(0.15, fundingRatio * 0.15);
        }

        // Investor count (social proof)
        if (campaign.getInvestorCount() > 10) {
            score += 0.1;
        }

        // Risk tolerance match
        if ("LOW".equalsIgnoreCase(request.getRiskTolerance())) {
            // Prefer campaigns closer to target (lower risk)
            if (campaign.getRaisedAmount().compareTo(campaign.getTargetAmount().multiply(BigDecimal.valueOf(0.5))) > 0) {
                score += 0.05;
            }
        } else if ("HIGH".equalsIgnoreCase(request.getRiskTolerance())) {
            // Prefer newer campaigns with higher potential
            if (campaign.getRaisedAmount().compareTo(campaign.getTargetAmount().multiply(BigDecimal.valueOf(0.3))) < 0) {
                score += 0.05;
            }
        }

        return Math.min(1.0, score);
    }

    private String generateReason(Campaign campaign, double matchScore) {
        if (matchScore > 0.8) {
            return "Strong match based on your investment preferences and this campaign's momentum";
        } else if (matchScore > 0.6) {
            return "Good match - " + campaign.getIndustry() + " sector with solid funding progress";
        } else {
            return "Diversification opportunity in " + campaign.getIndustry();
        }
    }

    private String generatePortfolioInsight(List<Investment> investments, RecommendationRequest request) {
        if (investments.isEmpty()) {
            return "You haven't made any investments yet. Start with campaigns that match your risk profile and interests.";
        }

        try {
            String prompt = String.format(
                    "In 2 sentences, summarize this investor's portfolio: %d investments made. " +
                    "Risk tolerance: %s. Suggest one improvement.",
                    investments.size(),
                    request.getRiskTolerance() != null ? request.getRiskTolerance() : "MEDIUM");

            return ChatClient.builder(chatModel).build()
                    .prompt()
                    .system("You are a brief investment advisor for an East African crowdfunding platform. Keep responses under 50 words.")
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Failed to generate AI portfolio insight: {}", e.getMessage());
            return "Your portfolio has " + investments.size() + " investments. Consider diversifying across different sectors.";
        }
    }

    private String generateMarketInsight(List<Campaign> campaigns, RecommendationRequest request) {
        if (campaigns.isEmpty()) {
            return "No live campaigns currently match your criteria. Check back soon for new opportunities.";
        }

        try {
            Map<String, Long> industryCounts = campaigns.stream()
                    .filter(c -> c.getIndustry() != null)
                    .collect(Collectors.groupingBy(Campaign::getIndustry, Collectors.counting()));

            String prompt = String.format(
                    "In 2 sentences, summarize the current crowdfunding market: %d live campaigns across sectors %s. " +
                    "Note the trending sector.",
                    campaigns.size(), industryCounts);

            return ChatClient.builder(chatModel).build()
                    .prompt()
                    .system("You are a brief market analyst for an East African crowdfunding platform. Keep responses under 50 words.")
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Failed to generate AI market insight: {}", e.getMessage());
            return campaigns.size() + " campaigns are currently live. Explore opportunities across different sectors.";
        }
    }
}
