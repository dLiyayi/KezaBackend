package com.keza.ai.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.ai.domain.model.FraudAlert;
import com.keza.ai.domain.model.FraudAlertStatus;
import com.keza.ai.domain.model.FraudSeverity;
import com.keza.ai.domain.port.out.FraudAlertRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService")
class FraudDetectionServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Captor
    private ArgumentCaptor<FraudAlert> alertCaptor;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("checkRegistration")
    class CheckRegistration {

        @Test
        @DisplayName("should skip check if a recent RAPID_REGISTRATION alert already exists")
        void shouldSkipIfRecentAlertExists() {
            when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("RAPID_REGISTRATION"), any(Instant.class)))
                    .thenReturn(1L);

            fraudDetectionService.checkRegistration(userId);

            verify(fraudAlertRepository).countRecentAlerts(eq(userId), eq("RAPID_REGISTRATION"), any(Instant.class));
            verify(fraudAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("should pass if no recent RAPID_REGISTRATION alert exists")
        void shouldPassIfNoRecentAlertExists() {
            when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("RAPID_REGISTRATION"), any(Instant.class)))
                    .thenReturn(0L);

            fraudDetectionService.checkRegistration(userId);

            verify(fraudAlertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("checkInvestment")
    class CheckInvestment {

        @Nested
        @DisplayName("velocity checks")
        class VelocityChecks {

            @Test
            @DisplayName("should create VELOCITY_VIOLATION alert when too many investments in last hour")
            void shouldCreateAlertWhenVelocityExceeded() throws Exception {
                // No existing velocity alert
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("VELOCITY_VIOLATION"), any(Instant.class)))
                        .thenReturn(0L);
                // 10 or more recent investment activity alerts
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("INVESTMENT_ACTIVITY"), any(Instant.class)))
                        .thenReturn(10L);
                // For amount check - below threshold
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(UUID.randomUUID());
                    return alert;
                });

                fraudDetectionService.checkInvestment(userId, new BigDecimal("100"));

                verify(fraudAlertRepository, atLeastOnce()).save(alertCaptor.capture());
                FraudAlert velocityAlert = alertCaptor.getAllValues().stream()
                        .filter(a -> "VELOCITY_VIOLATION".equals(a.getAlertType()))
                        .findFirst()
                        .orElse(null);

                assertThat(velocityAlert).isNotNull();
                assertThat(velocityAlert.getSeverity()).isEqualTo(FraudSeverity.HIGH);
                assertThat(velocityAlert.getUserId()).isEqualTo(userId);
                assertThat(velocityAlert.getStatus()).isEqualTo(FraudAlertStatus.NEW);
                assertThat(velocityAlert.getDescription()).contains("investments in the last hour");
            }

            @Test
            @DisplayName("should skip velocity alert if one was already raised in the last hour")
            void shouldSkipVelocityAlertIfAlreadyRaised() {
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("VELOCITY_VIOLATION"), any(Instant.class)))
                        .thenReturn(1L);

                fraudDetectionService.checkInvestment(userId, new BigDecimal("100"));

                verify(fraudAlertRepository, never()).save(any());
            }

            @Test
            @DisplayName("should not trigger velocity alert when investment count is below threshold")
            void shouldNotTriggerWhenBelowThreshold() {
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("VELOCITY_VIOLATION"), any(Instant.class)))
                        .thenReturn(0L);
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("INVESTMENT_ACTIVITY"), any(Instant.class)))
                        .thenReturn(5L);

                fraudDetectionService.checkInvestment(userId, new BigDecimal("100"));

                verify(fraudAlertRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("amount anomaly checks")
        class AmountAnomalyChecks {

            @BeforeEach
            void setUp() {
                // No velocity issues for these tests
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("VELOCITY_VIOLATION"), any(Instant.class)))
                        .thenReturn(0L);
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("INVESTMENT_ACTIVITY"), any(Instant.class)))
                        .thenReturn(0L);
            }

            @Test
            @DisplayName("should create CRITICAL alert for very high investment amount (>= 5M KES)")
            void shouldCreateCriticalAlertForVeryHighAmount() throws Exception {
                BigDecimal veryHighAmount = new BigDecimal("5000000");
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(UUID.randomUUID());
                    return alert;
                });

                fraudDetectionService.checkInvestment(userId, veryHighAmount);

                verify(fraudAlertRepository).save(alertCaptor.capture());
                FraudAlert alert = alertCaptor.getValue();
                assertThat(alert.getAlertType()).isEqualTo("AMOUNT_ANOMALY");
                assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.CRITICAL);
                assertThat(alert.getDescription()).contains("Very high investment amount");
                assertThat(alert.getDescription()).contains("5000000");
            }

            @Test
            @DisplayName("should create HIGH alert for high investment amount (>= 1M KES, < 5M KES)")
            void shouldCreateHighAlertForHighAmount() throws Exception {
                BigDecimal highAmount = new BigDecimal("1500000");
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(UUID.randomUUID());
                    return alert;
                });

                fraudDetectionService.checkInvestment(userId, highAmount);

                verify(fraudAlertRepository).save(alertCaptor.capture());
                FraudAlert alert = alertCaptor.getValue();
                assertThat(alert.getAlertType()).isEqualTo("AMOUNT_ANOMALY");
                assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.HIGH);
                assertThat(alert.getDescription()).contains("High investment amount");
            }

            @Test
            @DisplayName("should not create alert for normal investment amount (< 1M KES)")
            void shouldNotCreateAlertForNormalAmount() {
                fraudDetectionService.checkInvestment(userId, new BigDecimal("500000"));

                verify(fraudAlertRepository, never()).save(any());
            }

            @Test
            @DisplayName("should not create alert when amount is null")
            void shouldNotCreateAlertWhenAmountIsNull() {
                fraudDetectionService.checkInvestment(userId, null);

                verify(fraudAlertRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("createAlert")
    class CreateAlert {

        @Test
        @DisplayName("should create alert with valid severity string")
        void shouldCreateAlertWithValidSeverity() {
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            FraudAlert result = fraudDetectionService.createAlert(userId, "TEST_ALERT", "HIGH", "Test description");

            assertThat(result).isNotNull();
            assertThat(result.getSeverity()).isEqualTo(FraudSeverity.HIGH);
            assertThat(result.getAlertType()).isEqualTo("TEST_ALERT");
            assertThat(result.getDescription()).isEqualTo("Test description");
            assertThat(result.getStatus()).isEqualTo(FraudAlertStatus.NEW);
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("should default to MEDIUM severity for invalid severity string")
        void shouldDefaultToMediumSeverityForInvalidString() {
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            FraudAlert result = fraudDetectionService.createAlert(userId, "TEST", "INVALID_SEVERITY", "desc");

            assertThat(result.getSeverity()).isEqualTo(FraudSeverity.MEDIUM);
        }

        @Test
        @DisplayName("should serialize details map to JSON when provided")
        void shouldSerializeDetailsToJson() throws Exception {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            var details = java.util.Map.of("key", (Object) "value");
            FraudAlert result = fraudDetectionService.createAlert(userId, "TEST", "LOW", "desc", details);

            assertThat(result.getDetails()).isEqualTo("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("should handle JSON serialization failure gracefully")
        void shouldHandleJsonSerializationFailure() throws Exception {
            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("fail") {});
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            var details = java.util.Map.of("key", (Object) "value");
            FraudAlert result = fraudDetectionService.createAlert(userId, "TEST", "LOW", "desc", details);

            assertThat(result).isNotNull();
            assertThat(result.getDetails()).isNull();
        }

        @Test
        @DisplayName("should set details to null when no details map is provided")
        void shouldSetDetailsNullWhenNotProvided() {
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            FraudAlert result = fraudDetectionService.createAlert(userId, "TEST", "LOW", "desc");

            assertThat(result.getDetails()).isNull();
        }
    }
}
