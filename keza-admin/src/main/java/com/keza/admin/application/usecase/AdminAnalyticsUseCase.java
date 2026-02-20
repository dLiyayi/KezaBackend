package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.PlatformOverviewResponse;
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
}
