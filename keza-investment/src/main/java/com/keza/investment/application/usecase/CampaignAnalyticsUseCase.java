package com.keza.investment.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.investment.application.dto.CampaignAnalyticsResponse;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignAnalyticsUseCase {

    private final CampaignRepository campaignRepository;
    private final InvestmentRepository investmentRepository;

    @Transactional(readOnly = true)
    public CampaignAnalyticsResponse getCampaignAnalytics(UUID campaignId, UUID issuerId) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not own this campaign");
        }

        Page<Investment> investmentsPage = investmentRepository.findByCampaignIdOrderByCreatedAtDesc(
                campaignId, PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Investment> investments = investmentsPage.getContent();

        BigDecimal fundingPercentage = BigDecimal.ZERO;
        if (campaign.getTargetAmount().signum() > 0) {
            fundingPercentage = campaign.getRaisedAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(campaign.getTargetAmount(), 2, RoundingMode.HALF_UP);
        }

        BigDecimal averageInvestment = BigDecimal.ZERO;
        BigDecimal largestInvestment = BigDecimal.ZERO;
        BigDecimal smallestInvestment = BigDecimal.ZERO;

        List<Investment> activeInvestments = investments.stream()
                .filter(inv -> inv.getStatus() != InvestmentStatus.CANCELLED
                        && inv.getStatus() != InvestmentStatus.REFUNDED)
                .collect(Collectors.toList());

        if (!activeInvestments.isEmpty()) {
            BigDecimal totalAmount = activeInvestments.stream()
                    .map(Investment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageInvestment = totalAmount.divide(
                    BigDecimal.valueOf(activeInvestments.size()), 2, RoundingMode.HALF_UP);
            largestInvestment = activeInvestments.stream()
                    .map(Investment::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            smallestInvestment = activeInvestments.stream()
                    .map(Investment::getAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        int daysRemaining = 0;
        int totalDays = 0;
        if (campaign.getStartDate() != null && campaign.getEndDate() != null) {
            totalDays = (int) ChronoUnit.DAYS.between(
                    campaign.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate(),
                    campaign.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate());
            long remaining = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    campaign.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate());
            daysRemaining = (int) Math.max(0, remaining);
        }

        int elapsedDays = Math.max(1, totalDays - daysRemaining);
        BigDecimal dailyVelocity = campaign.getRaisedAmount()
                .divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> investmentsByStatus = new LinkedHashMap<>();
        for (InvestmentStatus status : InvestmentStatus.values()) {
            BigDecimal statusTotal = investments.stream()
                    .filter(inv -> inv.getStatus() == status)
                    .map(Investment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (statusTotal.signum() > 0) {
                investmentsByStatus.put(status.name(), statusTotal);
            }
        }

        List<CampaignAnalyticsResponse.DailyInvestmentData> dailyInvestments = buildDailyInvestments(activeInvestments);

        return CampaignAnalyticsResponse.builder()
                .totalRaised(campaign.getRaisedAmount())
                .targetAmount(campaign.getTargetAmount())
                .fundingPercentage(fundingPercentage)
                .totalInvestors(campaign.getInvestorCount())
                .totalShares(campaign.getTotalShares() != null ? campaign.getTotalShares() : 0)
                .soldShares(campaign.getSoldShares())
                .averageInvestment(averageInvestment)
                .largestInvestment(largestInvestment)
                .smallestInvestment(smallestInvestment)
                .daysRemaining(daysRemaining)
                .totalDays(totalDays)
                .dailyFundingVelocity(dailyVelocity)
                .investmentsByStatus(investmentsByStatus)
                .dailyInvestments(dailyInvestments)
                .build();
    }

    private List<CampaignAnalyticsResponse.DailyInvestmentData> buildDailyInvestments(List<Investment> investments) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, BigDecimal> dailyAmounts = new TreeMap<>();
        Map<String, Integer> dailyCounts = new TreeMap<>();

        for (Investment inv : investments) {
            String date = inv.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter);
            dailyAmounts.merge(date, inv.getAmount(), BigDecimal::add);
            dailyCounts.merge(date, 1, Integer::sum);
        }

        return dailyAmounts.entrySet().stream()
                .map(entry -> CampaignAnalyticsResponse.DailyInvestmentData.builder()
                        .date(entry.getKey())
                        .amount(entry.getValue())
                        .count(dailyCounts.getOrDefault(entry.getKey(), 0))
                        .build())
                .collect(Collectors.toList());
    }
}
