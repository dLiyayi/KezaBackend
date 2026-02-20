package com.keza.campaign.domain.service;

import com.keza.campaign.domain.event.CampaignStatusChangedEvent;
import com.keza.campaign.domain.model.Campaign;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignStateMachine")
class CampaignStateMachineTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CampaignStateMachine stateMachine;

    @Captor
    private ArgumentCaptor<CampaignStatusChangedEvent> eventCaptor;

    private UUID campaignId;
    private UUID triggeredBy;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        triggeredBy = UUID.randomUUID();
    }

    private Campaign buildCampaignWithStatus(CampaignStatus status) {
        Campaign campaign = Campaign.builder()
                .issuerId(UUID.randomUUID())
                .title("Test Campaign")
                .targetAmount(new BigDecimal("100000"))
                .status(status)
                .wizardStep(1)
                .build();
        campaign.setId(campaignId);
        return campaign;
    }

    @Nested
    @DisplayName("Valid transitions from DRAFT")
    class DraftTransitions {

        @Test
        @DisplayName("DRAFT -> REVIEW should succeed")
        void draftToReview() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            stateMachine.transition(campaign, CampaignStatus.REVIEW, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.REVIEW);
        }

        @Test
        @DisplayName("DRAFT -> CANCELLED should succeed")
        void draftToCancelled() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            stateMachine.transition(campaign, CampaignStatus.CANCELLED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        }

        @Test
        @DisplayName("DRAFT -> LIVE should fail")
        void draftToLive() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.LIVE, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from DRAFT to LIVE");
        }

        @Test
        @DisplayName("DRAFT -> FUNDED should fail")
        void draftToFunded() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.FUNDED, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from DRAFT to FUNDED");
        }

        @Test
        @DisplayName("DRAFT -> CLOSED should fail")
        void draftToClosed() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.CLOSED, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from DRAFT to CLOSED");
        }
    }

    @Nested
    @DisplayName("Valid transitions from REVIEW")
    class ReviewTransitions {

        @Test
        @DisplayName("REVIEW -> LIVE should succeed")
        void reviewToLive() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.REVIEW);

            stateMachine.transition(campaign, CampaignStatus.LIVE, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.LIVE);
        }

        @Test
        @DisplayName("REVIEW -> DRAFT should succeed (rejection)")
        void reviewToDraft() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.REVIEW);

            stateMachine.transition(campaign, CampaignStatus.DRAFT, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        }

        @Test
        @DisplayName("REVIEW -> CANCELLED should succeed")
        void reviewToCancelled() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.REVIEW);

            stateMachine.transition(campaign, CampaignStatus.CANCELLED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        }

        @Test
        @DisplayName("REVIEW -> FUNDED should fail")
        void reviewToFunded() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.REVIEW);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.FUNDED, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from REVIEW to FUNDED");
        }

        @Test
        @DisplayName("REVIEW -> CLOSED should fail")
        void reviewToClosed() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.REVIEW);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.CLOSED, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from REVIEW to CLOSED");
        }
    }

    @Nested
    @DisplayName("Valid transitions from LIVE")
    class LiveTransitions {

        @Test
        @DisplayName("LIVE -> FUNDED should succeed")
        void liveToFunded() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.LIVE);

            stateMachine.transition(campaign, CampaignStatus.FUNDED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.FUNDED);
        }

        @Test
        @DisplayName("LIVE -> CLOSED should succeed")
        void liveToClosed() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.LIVE);

            stateMachine.transition(campaign, CampaignStatus.CLOSED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        }

        @Test
        @DisplayName("LIVE -> CANCELLED should succeed")
        void liveToCancelled() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.LIVE);

            stateMachine.transition(campaign, CampaignStatus.CANCELLED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        }

        @Test
        @DisplayName("LIVE -> DRAFT should fail")
        void liveToDraft() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.LIVE);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.DRAFT, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from LIVE to DRAFT");
        }

        @Test
        @DisplayName("LIVE -> REVIEW should fail")
        void liveToReview() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.LIVE);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.REVIEW, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from LIVE to REVIEW");
        }
    }

    @Nested
    @DisplayName("Valid transitions from FUNDED")
    class FundedTransitions {

        @Test
        @DisplayName("FUNDED -> CANCELLED should succeed")
        void fundedToCancelled() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.FUNDED);

            stateMachine.transition(campaign, CampaignStatus.CANCELLED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        }

        @Test
        @DisplayName("FUNDED -> DRAFT should fail")
        void fundedToDraft() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.FUNDED);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.DRAFT, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from FUNDED to DRAFT");
        }

        @Test
        @DisplayName("FUNDED -> LIVE should fail")
        void fundedToLive() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.FUNDED);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.LIVE, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from FUNDED to LIVE");
        }
    }

    @Nested
    @DisplayName("Valid transitions from CLOSED")
    class ClosedTransitions {

        @Test
        @DisplayName("CLOSED -> CANCELLED should succeed")
        void closedToCancelled() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.CLOSED);

            stateMachine.transition(campaign, CampaignStatus.CANCELLED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        }

        @Test
        @DisplayName("CLOSED -> DRAFT should fail")
        void closedToDraft() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.CLOSED);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.DRAFT, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from CLOSED to DRAFT");
        }

        @Test
        @DisplayName("CLOSED -> LIVE should fail")
        void closedToLive() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.CLOSED);

            assertThatThrownBy(() -> stateMachine.transition(campaign, CampaignStatus.LIVE, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from CLOSED to LIVE");
        }
    }

    @Nested
    @DisplayName("Transitions from CANCELLED (terminal state)")
    class CancelledTransitions {

        @ParameterizedTest(name = "CANCELLED -> {0} should fail")
        @EnumSource(value = CampaignStatus.class, names = {"DRAFT", "REVIEW", "LIVE", "FUNDED", "CLOSED"})
        @DisplayName("CANCELLED to any other status should fail")
        void cancelledToAny(CampaignStatus target) {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.CANCELLED);

            assertThatThrownBy(() -> stateMachine.transition(campaign, target, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition from CANCELLED to " + target);
        }
    }

    @Nested
    @DisplayName("Same-status transition (no-op guard)")
    class SameStatusTransition {

        @ParameterizedTest(name = "{0} -> {0} should fail with already-in-status message")
        @EnumSource(CampaignStatus.class)
        @DisplayName("Transitioning to the same status should throw")
        void sameStatusShouldThrow(CampaignStatus status) {
            Campaign campaign = buildCampaignWithStatus(status);

            assertThatThrownBy(() -> stateMachine.transition(campaign, status, triggeredBy))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already in " + status + " status");
        }
    }

    @Nested
    @DisplayName("Event publishing")
    class EventPublishing {

        @Test
        @DisplayName("should publish CampaignStatusChangedEvent on valid transition")
        void shouldPublishEvent() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            stateMachine.transition(campaign, CampaignStatus.REVIEW, triggeredBy);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            CampaignStatusChangedEvent event = eventCaptor.getValue();

            assertThat(event.campaignId()).isEqualTo(campaignId);
            assertThat(event.oldStatus()).isEqualTo(CampaignStatus.DRAFT);
            assertThat(event.newStatus()).isEqualTo(CampaignStatus.REVIEW);
            assertThat(event.triggeredBy()).isEqualTo(triggeredBy);
        }

        @Test
        @DisplayName("should not publish event on invalid transition")
        void shouldNotPublishEventOnFailure() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            try {
                stateMachine.transition(campaign, CampaignStatus.FUNDED, triggeredBy);
            } catch (BusinessRuleException ignored) {
                // expected
            }

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should not publish event on same-status transition")
        void shouldNotPublishEventOnSameStatus() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.LIVE);

            try {
                stateMachine.transition(campaign, CampaignStatus.LIVE, triggeredBy);
            } catch (BusinessRuleException ignored) {
                // expected
            }

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("Status update on campaign entity")
    class StatusUpdate {

        @Test
        @DisplayName("should update campaign status on valid transition")
        void shouldUpdateStatus() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.LIVE);

            stateMachine.transition(campaign, CampaignStatus.FUNDED, triggeredBy);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.FUNDED);
        }

        @Test
        @DisplayName("should not update campaign status on invalid transition")
        void shouldNotUpdateStatusOnFailure() {
            Campaign campaign = buildCampaignWithStatus(CampaignStatus.DRAFT);

            try {
                stateMachine.transition(campaign, CampaignStatus.FUNDED, triggeredBy);
            } catch (BusinessRuleException ignored) {
                // expected
            }

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        }
    }
}
