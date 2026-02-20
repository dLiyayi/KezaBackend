package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.PlatformOverviewResponse;
import com.keza.admin.domain.port.out.AdminAnalyticsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAnalyticsUseCase")
class AdminAnalyticsUseCaseTest {

    @Mock
    private AdminAnalyticsRepository analyticsRepository;

    @InjectMocks
    private AdminAnalyticsUseCase adminAnalyticsUseCase;

    @Nested
    @DisplayName("getPlatformOverview")
    class GetPlatformOverview {

        @Test
        @DisplayName("should aggregate all metrics from repository")
        void shouldAggregateAllMetrics() {
            when(analyticsRepository.countTotalUsers()).thenReturn(1500L);
            when(analyticsRepository.countInvestors()).thenReturn(1200L);
            when(analyticsRepository.countIssuers()).thenReturn(300L);
            when(analyticsRepository.countTotalCampaigns()).thenReturn(50L);
            when(analyticsRepository.countActiveCampaigns()).thenReturn(12L);
            when(analyticsRepository.sumTotalInvested()).thenReturn(BigDecimal.valueOf(25_000_000));
            when(analyticsRepository.countPendingKyc()).thenReturn(45L);
            when(analyticsRepository.countPendingCampaigns()).thenReturn(8L);

            PlatformOverviewResponse result = adminAnalyticsUseCase.getPlatformOverview();

            assertThat(result.getTotalUsers()).isEqualTo(1500L);
            assertThat(result.getTotalInvestors()).isEqualTo(1200L);
            assertThat(result.getTotalIssuers()).isEqualTo(300L);
            assertThat(result.getTotalCampaigns()).isEqualTo(50L);
            assertThat(result.getActiveCampaigns()).isEqualTo(12L);
            assertThat(result.getTotalInvested()).isEqualByComparingTo(BigDecimal.valueOf(25_000_000));
            assertThat(result.getPendingKycCount()).isEqualTo(45L);
            assertThat(result.getPendingCampaignsCount()).isEqualTo(8L);
        }

        @Test
        @DisplayName("should handle zero counts gracefully")
        void shouldHandleZeroCounts() {
            when(analyticsRepository.countTotalUsers()).thenReturn(0L);
            when(analyticsRepository.countInvestors()).thenReturn(0L);
            when(analyticsRepository.countIssuers()).thenReturn(0L);
            when(analyticsRepository.countTotalCampaigns()).thenReturn(0L);
            when(analyticsRepository.countActiveCampaigns()).thenReturn(0L);
            when(analyticsRepository.sumTotalInvested()).thenReturn(BigDecimal.ZERO);
            when(analyticsRepository.countPendingKyc()).thenReturn(0L);
            when(analyticsRepository.countPendingCampaigns()).thenReturn(0L);

            PlatformOverviewResponse result = adminAnalyticsUseCase.getPlatformOverview();

            assertThat(result.getTotalUsers()).isZero();
            assertThat(result.getTotalInvestors()).isZero();
            assertThat(result.getTotalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle null total invested as returned by repository")
        void shouldHandleNullTotalInvested() {
            when(analyticsRepository.countTotalUsers()).thenReturn(10L);
            when(analyticsRepository.countInvestors()).thenReturn(5L);
            when(analyticsRepository.countIssuers()).thenReturn(5L);
            when(analyticsRepository.countTotalCampaigns()).thenReturn(2L);
            when(analyticsRepository.countActiveCampaigns()).thenReturn(1L);
            when(analyticsRepository.sumTotalInvested()).thenReturn(null);
            when(analyticsRepository.countPendingKyc()).thenReturn(3L);
            when(analyticsRepository.countPendingCampaigns()).thenReturn(1L);

            PlatformOverviewResponse result = adminAnalyticsUseCase.getPlatformOverview();

            assertThat(result.getTotalInvested()).isNull();
            assertThat(result.getTotalUsers()).isEqualTo(10L);
        }

        @Test
        @DisplayName("should call each repository method exactly once")
        void shouldCallEachRepositoryMethodOnce() {
            when(analyticsRepository.countTotalUsers()).thenReturn(0L);
            when(analyticsRepository.countInvestors()).thenReturn(0L);
            when(analyticsRepository.countIssuers()).thenReturn(0L);
            when(analyticsRepository.countTotalCampaigns()).thenReturn(0L);
            when(analyticsRepository.countActiveCampaigns()).thenReturn(0L);
            when(analyticsRepository.sumTotalInvested()).thenReturn(BigDecimal.ZERO);
            when(analyticsRepository.countPendingKyc()).thenReturn(0L);
            when(analyticsRepository.countPendingCampaigns()).thenReturn(0L);

            adminAnalyticsUseCase.getPlatformOverview();

            verify(analyticsRepository, times(1)).countTotalUsers();
            verify(analyticsRepository, times(1)).countInvestors();
            verify(analyticsRepository, times(1)).countIssuers();
            verify(analyticsRepository, times(1)).countTotalCampaigns();
            verify(analyticsRepository, times(1)).countActiveCampaigns();
            verify(analyticsRepository, times(1)).sumTotalInvested();
            verify(analyticsRepository, times(1)).countPendingKyc();
            verify(analyticsRepository, times(1)).countPendingCampaigns();
        }

        @Test
        @DisplayName("should handle large numbers correctly")
        void shouldHandleLargeNumbers() {
            when(analyticsRepository.countTotalUsers()).thenReturn(10_000_000L);
            when(analyticsRepository.countInvestors()).thenReturn(8_000_000L);
            when(analyticsRepository.countIssuers()).thenReturn(2_000_000L);
            when(analyticsRepository.countTotalCampaigns()).thenReturn(500_000L);
            when(analyticsRepository.countActiveCampaigns()).thenReturn(100_000L);
            when(analyticsRepository.sumTotalInvested()).thenReturn(new BigDecimal("999999999999.99"));
            when(analyticsRepository.countPendingKyc()).thenReturn(50_000L);
            when(analyticsRepository.countPendingCampaigns()).thenReturn(10_000L);

            PlatformOverviewResponse result = adminAnalyticsUseCase.getPlatformOverview();

            assertThat(result.getTotalUsers()).isEqualTo(10_000_000L);
            assertThat(result.getTotalInvested()).isEqualByComparingTo(new BigDecimal("999999999999.99"));
        }
    }
}
