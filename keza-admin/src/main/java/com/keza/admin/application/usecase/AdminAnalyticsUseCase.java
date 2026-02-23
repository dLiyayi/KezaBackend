package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.*;
import com.keza.admin.domain.port.out.AdminAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsUseCase {

    private final AdminAnalyticsRepository analyticsRepository;

    /**
     * Returns a platform overview with key metrics.
     * Cached for 15 minutes to reduce database load.
     */
    @Cacheable(value = "analytics", key = "'platformOverview'")
    @Transactional(readOnly = true)
    public PlatformOverviewResponse getPlatformOverview() {
        log.info("Computing platform overview analytics (cache miss)");

        return PlatformOverviewResponse.builder()
                .totalUsers(analyticsRepository.countTotalUsers())
                .totalInvestors(analyticsRepository.countInvestors())
                .totalIssuers(analyticsRepository.countIssuers())
                .totalCampaigns(analyticsRepository.countTotalCampaigns())
                .activeCampaigns(analyticsRepository.countActiveCampaigns())
                .totalInvested(analyticsRepository.sumTotalInvested())
                .pendingKycCount(analyticsRepository.countPendingKyc())
                .pendingCampaignsCount(analyticsRepository.countPendingCampaigns())
                .build();
    }

    @Cacheable(value = "analytics", key = "'investmentAnalytics'")
    @Transactional(readOnly = true)
    public InvestmentAnalyticsResponse getInvestmentAnalytics() {
        log.info("Computing investment analytics (cache miss)");

        return InvestmentAnalyticsResponse.builder()
                .totalInvestments(analyticsRepository.countTotalInvestments())
                .completedInvestments(analyticsRepository.countInvestmentsByStatus("COMPLETED"))
                .pendingInvestments(analyticsRepository.countInvestmentsByStatus("PENDING"))
                .cancelledInvestments(analyticsRepository.countInvestmentsByStatus("CANCELLED"))
                .totalInvestedAmount(analyticsRepository.sumTotalInvested())
                .averageInvestmentAmount(analyticsRepository.averageInvestmentAmount())
                .uniqueInvestors(analyticsRepository.countUniqueInvestors())
                .build();
    }

    @Cacheable(value = "analytics", key = "'campaignAnalytics'")
    @Transactional(readOnly = true)
    public CampaignAnalyticsResponse getCampaignAnalytics() {
        log.info("Computing campaign analytics (cache miss)");

        return CampaignAnalyticsResponse.builder()
                .totalCampaigns(analyticsRepository.countTotalCampaigns())
                .draftCampaigns(analyticsRepository.countCampaignsByStatus("DRAFT"))
                .reviewCampaigns(analyticsRepository.countCampaignsByStatus("REVIEW"))
                .liveCampaigns(analyticsRepository.countCampaignsByStatus("LIVE"))
                .fundedCampaigns(analyticsRepository.countCampaignsByStatus("FUNDED"))
                .closedCampaigns(analyticsRepository.countCampaignsByStatus("CLOSED"))
                .totalRaisedAmount(analyticsRepository.sumTotalRaisedAmount())
                .averageFundingPercentage(analyticsRepository.averageFundingPercentage())
                .build();
    }

    @Cacheable(value = "analytics", key = "'userAnalytics'")
    @Transactional(readOnly = true)
    public UserAnalyticsResponse getUserAnalytics() {
        log.info("Computing user analytics (cache miss)");

        return UserAnalyticsResponse.builder()
                .totalUsers(analyticsRepository.countTotalUsers())
                .totalInvestors(analyticsRepository.countInvestors())
                .totalIssuers(analyticsRepository.countIssuers())
                .verifiedUsers(analyticsRepository.countVerifiedUsers())
                .unverifiedUsers(analyticsRepository.countTotalUsers() - analyticsRepository.countVerifiedUsers())
                .lockedUsers(analyticsRepository.countLockedUsers())
                .kycPending(analyticsRepository.countUsersByKycStatus("PENDING"))
                .kycSubmitted(analyticsRepository.countUsersByKycStatus("SUBMITTED"))
                .kycApproved(analyticsRepository.countUsersByKycStatus("APPROVED"))
                .kycRejected(analyticsRepository.countUsersByKycStatus("REJECTED"))
                .registrationsLast7Days(analyticsRepository.countRegistrationsSince(7))
                .registrationsLast30Days(analyticsRepository.countRegistrationsSince(30))
                .build();
    }
}
