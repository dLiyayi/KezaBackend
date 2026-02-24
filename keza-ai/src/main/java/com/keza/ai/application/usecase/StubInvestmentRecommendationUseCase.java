package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.RecommendationRequest;
import com.keza.ai.application.dto.RecommendationResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.CampaignStatus;
import com.keza.investment.domain.port.out.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubInvestmentRecommendationUseCase extends InvestmentRecommendationUseCaseBase {

    private final CampaignRepository campaignRepository;

    public StubInvestmentRecommendationUseCase(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(UUID userId, RecommendationRequest request) {
        log.info("Generating stub recommendations for user {} (AI disabled)", userId);

        Specification<Campaign> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), CampaignStatus.LIVE),
                cb.equal(root.get("deleted"), false));

        List<Campaign> campaigns = campaignRepository.findAll(spec,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "raisedAmount"))).getContent();

        List<RecommendationResponse.CampaignRecommendation> recommendations = campaigns.stream()
                .map(c -> {
                    BigDecimal fp = BigDecimal.ZERO;
                    if (c.getTargetAmount().signum() > 0) {
                        fp = c.getRaisedAmount().multiply(BigDecimal.valueOf(100))
                                .divide(c.getTargetAmount(), 2, RoundingMode.HALF_UP);
                    }
                    return RecommendationResponse.CampaignRecommendation.builder()
                            .campaignId(c.getId())
                            .title(c.getTitle())
                            .companyName(c.getCompanyName())
                            .industry(c.getIndustry())
                            .targetAmount(c.getTargetAmount())
                            .raisedAmount(c.getRaisedAmount())
                            .fundingPercentage(fp)
                            .suggestedAmount(c.getMinInvestment())
                            .reason("Popular campaign in " + c.getIndustry())
                            .matchScore(0.5)
                            .build();
                })
                .collect(Collectors.toList());

        return RecommendationResponse.builder()
                .recommendations(recommendations)
                .portfolioInsight("AI recommendations are currently unavailable. Showing top campaigns by funding.")
                .marketInsight(campaigns.size() + " campaigns are currently live on the platform.")
                .build();
    }
}
