package com.keza.investment.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.InvestmentStatus;
import com.keza.investment.application.dto.InvestmentResponse;
import com.keza.investment.application.dto.PortfolioResponse;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioUseCase {

    private final InvestmentRepository investmentRepository;
    private final CampaignRepository campaignRepository;
    private final InvestmentUseCase investmentUseCase;

    @Transactional(readOnly = true)
    @Cacheable(value = "portfolio", key = "#userId")
    public PortfolioResponse getPortfolio(UUID userId) {
        log.info("Building portfolio for user {}", userId);

        Page<Investment> investmentsPage = investmentRepository.findByInvestorIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Investment> allInvestments = investmentsPage.getContent();

        BigDecimal totalInvested = allInvestments.stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.COMPLETED
                        || inv.getStatus() == InvestmentStatus.COOLING_OFF
                        || inv.getStatus() == InvestmentStatus.PENDING)
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int activeInvestments = (int) allInvestments.stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.COMPLETED
                        || inv.getStatus() == InvestmentStatus.COOLING_OFF)
                .count();

        int pendingInvestments = (int) allInvestments.stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.PENDING
                        || inv.getStatus() == InvestmentStatus.PAYMENT_INITIATED)
                .count();

        int cancelledInvestments = (int) allInvestments.stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.CANCELLED
                        || inv.getStatus() == InvestmentStatus.REFUNDED)
                .count();

        // Batch lookup campaigns for all investments
        Set<UUID> campaignIds = allInvestments.stream()
                .map(Investment::getCampaignId)
                .collect(Collectors.toSet());

        Map<UUID, Campaign> campaignMap = campaignIds.isEmpty()
                ? Collections.emptyMap()
                : campaignRepository.findAllById(campaignIds).stream()
                        .collect(Collectors.toMap(Campaign::getId, c -> c));

        Map<String, BigDecimal> sectorDistribution = buildSectorDistribution(allInvestments, campaignMap);

        List<InvestmentResponse> investmentResponses = allInvestments.stream()
                .map(inv -> investmentUseCase.mapToResponse(inv, campaignMap.get(inv.getCampaignId())))
                .collect(Collectors.toList());

        return PortfolioResponse.builder()
                .totalInvested(totalInvested)
                .activeInvestments(activeInvestments)
                .pendingInvestments(pendingInvestments)
                .cancelledInvestments(cancelledInvestments)
                .totalInvestmentCount(allInvestments.size())
                .sectorDistribution(sectorDistribution)
                .investments(investmentResponses)
                .build();
    }

    private Map<String, BigDecimal> buildSectorDistribution(List<Investment> investments,
                                                             Map<UUID, Campaign> campaignMap) {
        List<Investment> activeInvestments = investments.stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.COMPLETED
                        || inv.getStatus() == InvestmentStatus.COOLING_OFF)
                .collect(Collectors.toList());

        if (activeInvestments.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> sectorTotals = new HashMap<>();

        for (Investment investment : activeInvestments) {
            Campaign campaign = campaignMap.get(investment.getCampaignId());
            String industry = (campaign != null && campaign.getIndustry() != null)
                    ? campaign.getIndustry() : "Other";
            sectorTotals.merge(industry, investment.getAmount(), BigDecimal::add);
        }

        BigDecimal total = sectorTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> sectorPercentages = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : sectorTotals.entrySet()) {
            BigDecimal percentage = entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, java.math.RoundingMode.HALF_UP);
            sectorPercentages.put(entry.getKey(), percentage);
        }

        return sectorPercentages;
    }
}
