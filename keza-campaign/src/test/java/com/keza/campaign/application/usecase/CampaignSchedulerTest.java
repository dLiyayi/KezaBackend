package com.keza.campaign.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.campaign.domain.service.CampaignStateMachine;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignScheduler")
class CampaignSchedulerTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignStateMachine stateMachine;

    @InjectMocks
    private CampaignScheduler campaignScheduler;

    @Captor
    private ArgumentCaptor<CampaignStatus> statusCaptor;

    @Captor
    private ArgumentCaptor<UUID> userCaptor;

    private static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private Campaign buildLiveCampaign(BigDecimal target, BigDecimal raised, Instant endDate) {
        Campaign campaign = Campaign.builder()
                .issuerId(UUID.randomUUID())
                .title("Test Campaign")
                .targetAmount(target)
                .raisedAmount(raised)
                .status(CampaignStatus.LIVE)
                .wizardStep(6)
                .endDate(endDate)
                .build();
        campaign.setId(UUID.randomUUID());
        return campaign;
    }

    @Nested
    @DisplayName("autoCloseExpired")
    class AutoCloseExpired {

        @Test
        @DisplayName("should close expired LIVE campaigns")
        void shouldCloseExpiredCampaigns() {
            Campaign expired1 = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("50000"),
                    Instant.now().minus(1, ChronoUnit.DAYS));
            Campaign expired2 = buildLiveCampaign(
                    new BigDecimal("200000"), new BigDecimal("100000"),
                    Instant.now().minus(2, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(expired1, expired2));

            campaignScheduler.autoCloseExpired();

            verify(stateMachine).transition(expired1, CampaignStatus.CLOSED, SYSTEM_USER);
            verify(stateMachine).transition(expired2, CampaignStatus.CLOSED, SYSTEM_USER);
            verify(campaignRepository).save(expired1);
            verify(campaignRepository).save(expired2);
        }

        @Test
        @DisplayName("should do nothing when no expired campaigns exist")
        void shouldDoNothingWhenNoExpiredCampaigns() {
            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            campaignScheduler.autoCloseExpired();

            verify(stateMachine, never()).transition(any(), any(), any());
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("should continue processing remaining campaigns when one fails")
        void shouldContinueOnFailure() {
            Campaign expired1 = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("50000"),
                    Instant.now().minus(1, ChronoUnit.DAYS));
            Campaign expired2 = buildLiveCampaign(
                    new BigDecimal("200000"), new BigDecimal("100000"),
                    Instant.now().minus(2, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(expired1, expired2));
            doThrow(new RuntimeException("DB error"))
                    .when(stateMachine).transition(expired1, CampaignStatus.CLOSED, SYSTEM_USER);

            campaignScheduler.autoCloseExpired();

            verify(stateMachine).transition(expired1, CampaignStatus.CLOSED, SYSTEM_USER);
            verify(stateMachine).transition(expired2, CampaignStatus.CLOSED, SYSTEM_USER);
            verify(campaignRepository, never()).save(expired1);
            verify(campaignRepository).save(expired2);
        }
    }

    @Nested
    @DisplayName("autoFund")
    class AutoFund {

        @Test
        @DisplayName("should fund campaign that reached target amount")
        void shouldFundCampaignAtTarget() {
            Campaign fullyFunded = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("100000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(fullyFunded));

            campaignScheduler.autoFund();

            verify(stateMachine).transition(fullyFunded, CampaignStatus.FUNDED, SYSTEM_USER);
            verify(campaignRepository).save(fullyFunded);
            assertThat(fullyFunded.getFundedAt()).isNotNull();
        }

        @Test
        @DisplayName("should fund campaign that exceeded target amount")
        void shouldFundCampaignExceedingTarget() {
            Campaign overFunded = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("150000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(overFunded));

            campaignScheduler.autoFund();

            verify(stateMachine).transition(overFunded, CampaignStatus.FUNDED, SYSTEM_USER);
            verify(campaignRepository).save(overFunded);
        }

        @Test
        @DisplayName("should not fund campaign below target amount")
        void shouldNotFundBelowTarget() {
            Campaign underFunded = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("50000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(underFunded));

            campaignScheduler.autoFund();

            verify(stateMachine, never()).transition(any(), any(), any());
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("should do nothing when no live campaigns exist")
        void shouldDoNothingWhenNoLiveCampaigns() {
            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            campaignScheduler.autoFund();

            verify(stateMachine, never()).transition(any(), any(), any());
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("should fund only campaigns that reached target in a mixed list")
        void shouldFundOnlyReachedTarget() {
            Campaign fullyFunded = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("100000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));
            Campaign underFunded = buildLiveCampaign(
                    new BigDecimal("200000"), new BigDecimal("50000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(fullyFunded, underFunded));

            campaignScheduler.autoFund();

            verify(stateMachine).transition(fullyFunded, CampaignStatus.FUNDED, SYSTEM_USER);
            verify(stateMachine, never()).transition(eq(underFunded), any(), any());
            verify(campaignRepository).save(fullyFunded);
            verify(campaignRepository, never()).save(underFunded);
        }

        @Test
        @DisplayName("should continue processing when one campaign fails to fund")
        void shouldContinueOnFailure() {
            Campaign fullyFunded1 = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("100000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));
            Campaign fullyFunded2 = buildLiveCampaign(
                    new BigDecimal("200000"), new BigDecimal("200000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(fullyFunded1, fullyFunded2));
            doThrow(new RuntimeException("DB error"))
                    .when(stateMachine).transition(fullyFunded1, CampaignStatus.FUNDED, SYSTEM_USER);

            campaignScheduler.autoFund();

            verify(stateMachine).transition(fullyFunded1, CampaignStatus.FUNDED, SYSTEM_USER);
            verify(stateMachine).transition(fullyFunded2, CampaignStatus.FUNDED, SYSTEM_USER);
            verify(campaignRepository, never()).save(fullyFunded1);
            verify(campaignRepository).save(fullyFunded2);
        }

        @Test
        @DisplayName("should set fundedAt timestamp when funding a campaign")
        void shouldSetFundedAtTimestamp() {
            Instant before = Instant.now();
            Campaign fullyFunded = buildLiveCampaign(
                    new BigDecimal("100000"), new BigDecimal("100000"),
                    Instant.now().plus(30, ChronoUnit.DAYS));

            when(campaignRepository.findByStatusAndEndDateBefore(eq(CampaignStatus.LIVE), any(Instant.class)))
                    .thenReturn(List.of(fullyFunded));

            campaignScheduler.autoFund();

            assertThat(fullyFunded.getFundedAt()).isNotNull();
            assertThat(fullyFunded.getFundedAt()).isAfterOrEqualTo(before);
        }
    }
}
