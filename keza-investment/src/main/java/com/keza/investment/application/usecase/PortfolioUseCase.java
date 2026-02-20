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

        Map<String, BigDecimal> sectorDistribution = buildSectorDistribution(allInvestments);

        List<InvestmentResponse> investmentResponses = allInvestments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PortfolioResponse.builder()
                .totalInvested(totalInvested)
                .activeInvestments(activeInvestments)
                .sectorDistribution(sectorDistribution)
                .investments(investmentResponses)
                .build();
    }

    private Map<String, BigDecimal> buildSectorDistribution(List<Investment> investments) {
        List<Investment> activeInvestments = investments.stream()
                .filter(inv -> inv.getStatus() == InvestmentStatus.COMPLETED
                        || inv.getStatus() == InvestmentStatus.COOLING_OFF)
                .collect(Collectors.toList());

        if (activeInvestments.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<UUID> campaignIds = activeInvestments.stream()
                .map(Investment::getCampaignId)
                .collect(Collectors.toSet());

        Map<UUID, String> campaignIndustryMap = campaignRepository.findAllById(campaignIds).stream()
                .collect(Collectors.toMap(Campaign::getId, c -> c.getIndustry() != null ? c.getIndustry() : "Other"));

        Map<String, BigDecimal> sectorTotals = new HashMap<>();

        for (Investment investment : activeInvestments) {
            String industry = campaignIndustryMap.getOrDefault(investment.getCampaignId(), "Other");
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

    private InvestmentResponse mapToResponse(Investment investment) {
        return InvestmentResponse.builder()
                .id(investment.getId())
                .investorId(investment.getInvestorId())
                .campaignId(investment.getCampaignId())
                .amount(investment.getAmount())
                .shares(investment.getShares())
                .sharePrice(investment.getSharePrice())
                .status(investment.getStatus().name())
                .paymentMethod(investment.getPaymentMethod() != null
                        ? investment.getPaymentMethod().name() : null)
                .coolingOffExpiresAt(investment.getCoolingOffExpiresAt())
                .completedAt(investment.getCompletedAt())
                .cancelledAt(investment.getCancelledAt())
                .cancellationReason(investment.getCancellationReason())
                .createdAt(investment.getCreatedAt())
                .updatedAt(investment.getUpdatedAt())
                .build();
    }
}
