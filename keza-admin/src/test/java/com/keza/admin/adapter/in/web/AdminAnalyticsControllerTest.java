package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.PlatformOverviewResponse;
import com.keza.admin.application.usecase.AdminAnalyticsUseCase;
import com.keza.infrastructure.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAnalyticsController")
class AdminAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAnalyticsUseCase adminAnalyticsUseCase;

    @InjectMocks
    private AdminAnalyticsController adminAnalyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAnalyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/admin/analytics/overview")
    class GetPlatformOverview {

        @Test
        @DisplayName("should return 200 with platform overview")
        void shouldReturnPlatformOverview() throws Exception {
            PlatformOverviewResponse overview = PlatformOverviewResponse.builder()
                    .totalUsers(1500)
                    .totalInvestors(1200)
                    .totalIssuers(300)
                    .totalCampaigns(50)
                    .activeCampaigns(12)
                    .totalInvested(BigDecimal.valueOf(25_000_000))
                    .pendingKycCount(45)
                    .pendingCampaignsCount(8)
                    .build();

            when(adminAnalyticsUseCase.getPlatformOverview()).thenReturn(overview);

            mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalUsers").value(1500))
                    .andExpect(jsonPath("$.data.totalInvestors").value(1200))
                    .andExpect(jsonPath("$.data.totalIssuers").value(300))
                    .andExpect(jsonPath("$.data.totalCampaigns").value(50))
                    .andExpect(jsonPath("$.data.activeCampaigns").value(12))
                    .andExpect(jsonPath("$.data.totalInvested").value(25000000))
                    .andExpect(jsonPath("$.data.pendingKycCount").value(45))
                    .andExpect(jsonPath("$.data.pendingCampaignsCount").value(8));
        }

        @Test
        @DisplayName("should return 200 with zero metrics for empty platform")
        void shouldReturnZeroMetrics() throws Exception {
            PlatformOverviewResponse overview = PlatformOverviewResponse.builder()
                    .totalUsers(0)
                    .totalInvestors(0)
                    .totalIssuers(0)
                    .totalCampaigns(0)
                    .activeCampaigns(0)
                    .totalInvested(BigDecimal.ZERO)
                    .pendingKycCount(0)
                    .pendingCampaignsCount(0)
                    .build();

            when(adminAnalyticsUseCase.getPlatformOverview()).thenReturn(overview);

            mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalUsers").value(0))
                    .andExpect(jsonPath("$.data.totalInvested").value(0));
        }
    }
}
