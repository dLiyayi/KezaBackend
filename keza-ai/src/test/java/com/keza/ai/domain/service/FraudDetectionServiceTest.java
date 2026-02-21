package com.keza.ai.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.ai.domain.model.FraudAlert;
import com.keza.ai.domain.model.FraudAlertStatus;
import com.keza.ai.domain.model.FraudSeverity;
import com.keza.ai.domain.port.out.FraudAlertRepository;
import com.keza.ai.domain.port.out.FraudDataPort;
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
    private FraudDataPort fraudDataPort;

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
        @DisplayName("should create DUPLICATE_PHONE alert when multiple users share the same phone")
        void shouldCreateAlertForDuplicatePhone() throws Exception {
            when(fraudDataPort.countUsersWithPhone("+254700000000")).thenReturn(2L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            fraudDetectionService.checkRegistration(userId, "+254700000000", null);

            verify(fraudAlertRepository).save(alertCaptor.capture());
            FraudAlert alert = alertCaptor.getValue();
            assertThat(alert.getAlertType()).isEqualTo("DUPLICATE_PHONE");
            assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.HIGH);
            assertThat(alert.getUserId()).isEqualTo(userId);
            assertThat(alert.getDescription()).contains("Duplicate phone number");
        }

        @Test
        @DisplayName("should create DUPLICATE_NATIONAL_ID alert when multiple users share the same national ID")
        void shouldCreateAlertForDuplicateNationalId() throws Exception {
            when(fraudDataPort.countUsersWithNationalId("12345678")).thenReturn(3L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            fraudDetectionService.checkRegistration(userId, null, "12345678");

            verify(fraudAlertRepository).save(alertCaptor.capture());
            FraudAlert alert = alertCaptor.getValue();
            assertThat(alert.getAlertType()).isEqualTo("DUPLICATE_NATIONAL_ID");
            assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.CRITICAL);
            assertThat(alert.getDescription()).contains("Duplicate national ID");
        }

        @Test
        @DisplayName("should not create alert when phone and nationalId are unique")
        void shouldNotCreateAlertWhenUnique() {
            when(fraudDataPort.countUsersWithPhone("+254700000000")).thenReturn(1L);
            when(fraudDataPort.countUsersWithNationalId("12345678")).thenReturn(1L);

            fraudDetectionService.checkRegistration(userId, "+254700000000", "12345678");

            verify(fraudAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not create alert when phone and nationalId are null")
        void shouldNotCreateAlertWhenNullParams() {
            fraudDetectionService.checkRegistration(userId, null, null);

            verify(fraudAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("backward-compatible overload should work without phone/nationalId")
        void backwardCompatibleOverloadShouldWork() {
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
            @DisplayName("should create VELOCITY_VIOLATION alert when 5+ investments in last hour")
            void shouldCreateAlertWhenVelocityExceeded() throws Exception {
                // No existing velocity alert
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("VELOCITY_VIOLATION"), any(Instant.class)))
                        .thenReturn(0L);
                // 5 recent investments (meets threshold of 5)
                when(fraudDataPort.countInvestmentsByUserSince(eq(userId), any(Instant.class)))
                        .thenReturn(5L);
                // For amount anomaly - no relative check triggered (below minimum suspicious)
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
                when(fraudDataPort.countInvestmentsByUserSince(eq(userId), any(Instant.class)))
                        .thenReturn(4L);

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
                when(fraudDataPort.countInvestmentsByUserSince(eq(userId), any(Instant.class)))
                        .thenReturn(0L);
            }

            @Test
            @DisplayName("should create CRITICAL alert for very high investment amount (>= 5M KES)")
            void shouldCreateCriticalAlertForVeryHighAmount() throws Exception {
                BigDecimal veryHighAmount = new BigDecimal("5000000");
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                when(fraudDataPort.getAverageInvestmentAmount(userId)).thenReturn(BigDecimal.ZERO);
                when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(UUID.randomUUID());
                    return alert;
                });

                fraudDetectionService.checkInvestment(userId, veryHighAmount);

                verify(fraudAlertRepository, atLeastOnce()).save(alertCaptor.capture());
                FraudAlert alert = alertCaptor.getAllValues().stream()
                        .filter(a -> "AMOUNT_ANOMALY".equals(a.getAlertType()))
                        .findFirst()
                        .orElse(null);
                assertThat(alert).isNotNull();
                assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.CRITICAL);
                assertThat(alert.getDescription()).contains("Very high investment amount");
                assertThat(alert.getDescription()).contains("5000000");
            }

            @Test
            @DisplayName("should create HIGH alert for high investment amount (>= 1M KES, < 5M KES)")
            void shouldCreateHighAlertForHighAmount() throws Exception {
                BigDecimal highAmount = new BigDecimal("1500000");
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                when(fraudDataPort.getAverageInvestmentAmount(userId)).thenReturn(BigDecimal.ZERO);
                when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(UUID.randomUUID());
                    return alert;
                });

                fraudDetectionService.checkInvestment(userId, highAmount);

                verify(fraudAlertRepository, atLeastOnce()).save(alertCaptor.capture());
                FraudAlert alert = alertCaptor.getAllValues().stream()
                        .filter(a -> "AMOUNT_ANOMALY".equals(a.getAlertType()))
                        .findFirst()
                        .orElse(null);
                assertThat(alert).isNotNull();
                assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.HIGH);
                assertThat(alert.getDescription()).contains("High investment amount");
            }

            @Test
            @DisplayName("should not create alert for normal investment amount (< 1M KES)")
            void shouldNotCreateAlertForNormalAmount() {
                when(fraudDataPort.getAverageInvestmentAmount(userId)).thenReturn(BigDecimal.ZERO);

                fraudDetectionService.checkInvestment(userId, new BigDecimal("500000"));

                verify(fraudAlertRepository, never()).save(any());
            }

            @Test
            @DisplayName("should not create alert when amount is null")
            void shouldNotCreateAlertWhenAmountIsNull() {
                fraudDetectionService.checkInvestment(userId, null);

                verify(fraudAlertRepository, never()).save(any());
            }

            @Test
            @DisplayName("should create AMOUNT_ANOMALY_RELATIVE alert when amount > 3x user average and above minimum")
            void shouldCreateRelativeAnomalyAlert() throws Exception {
                BigDecimal amount = new BigDecimal("200000"); // well above 3x average of 50000
                when(fraudDataPort.getAverageInvestmentAmount(userId)).thenReturn(new BigDecimal("50000"));
                when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("AMOUNT_ANOMALY_RELATIVE"), any(Instant.class)))
                        .thenReturn(0L);
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");
                when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(UUID.randomUUID());
                    return alert;
                });

                fraudDetectionService.checkInvestment(userId, amount);

                verify(fraudAlertRepository, atLeastOnce()).save(alertCaptor.capture());
                FraudAlert relativeAlert = alertCaptor.getAllValues().stream()
                        .filter(a -> "AMOUNT_ANOMALY_RELATIVE".equals(a.getAlertType()))
                        .findFirst()
                        .orElse(null);

                assertThat(relativeAlert).isNotNull();
                assertThat(relativeAlert.getSeverity()).isEqualTo(FraudSeverity.MEDIUM);
                assertThat(relativeAlert.getDescription()).contains("user's average");
            }

            @Test
            @DisplayName("should not create relative anomaly alert when amount is below minimum suspicious amount")
            void shouldNotCreateRelativeAnomalyWhenBelowMinimum() {
                BigDecimal amount = new BigDecimal("30000"); // below MINIMUM_SUSPICIOUS_AMOUNT of 50000

                fraudDetectionService.checkInvestment(userId, amount);

                verify(fraudAlertRepository, never()).save(any());
                // getAverageInvestmentAmount should NOT be called since amount < MINIMUM_SUSPICIOUS_AMOUNT
                verify(fraudDataPort, never()).getAverageInvestmentAmount(any());
            }

            @Test
            @DisplayName("should not create relative anomaly alert when user has no investment history")
            void shouldNotCreateRelativeAnomalyWhenNoHistory() {
                BigDecimal amount = new BigDecimal("100000");
                when(fraudDataPort.getAverageInvestmentAmount(userId)).thenReturn(BigDecimal.ZERO);

                fraudDetectionService.checkInvestment(userId, amount);

                verify(fraudAlertRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("checkPayment")
    class CheckPayment {

        @Test
        @DisplayName("should create FAILED_PAYMENT_PATTERN alert when too many failed payments")
        void shouldCreateAlertForHighFailureRate() throws Exception {
            UUID transactionId = UUID.randomUUID();
            when(fraudDataPort.countFailedPaymentsByUserSince(eq(userId), any(Instant.class)))
                    .thenReturn(6L);
            when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("FAILED_PAYMENT_PATTERN"), any(Instant.class)))
                    .thenReturn(0L);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                FraudAlert alert = invocation.getArgument(0);
                alert.setId(UUID.randomUUID());
                return alert;
            });

            fraudDetectionService.checkPayment(userId, transactionId);

            verify(fraudAlertRepository).save(alertCaptor.capture());
            FraudAlert alert = alertCaptor.getValue();
            assertThat(alert.getAlertType()).isEqualTo("FAILED_PAYMENT_PATTERN");
            assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.HIGH);
            assertThat(alert.getDescription()).contains("failed payments");
        }

        @Test
        @DisplayName("should not create alert when failed payments are below threshold")
        void shouldNotCreateAlertWhenBelowThreshold() {
            UUID transactionId = UUID.randomUUID();
            when(fraudDataPort.countFailedPaymentsByUserSince(eq(userId), any(Instant.class)))
                    .thenReturn(3L);

            fraudDetectionService.checkPayment(userId, transactionId);

            verify(fraudAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip alert if one was already raised in last 24 hours")
        void shouldSkipIfAlertAlreadyRaised() {
            UUID transactionId = UUID.randomUUID();
            when(fraudDataPort.countFailedPaymentsByUserSince(eq(userId), any(Instant.class)))
                    .thenReturn(10L);
            when(fraudAlertRepository.countRecentAlerts(eq(userId), eq("FAILED_PAYMENT_PATTERN"), any(Instant.class)))
                    .thenReturn(1L);

            fraudDetectionService.checkPayment(userId, transactionId);

            verify(fraudAlertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("runDailyBatchCheck")
    class RunDailyBatchCheck {

        @Test
        @DisplayName("should run without error")
        void shouldRunWithoutError() {
            fraudDetectionService.runDailyBatchCheck();

            // Currently a placeholder - just verify it does not throw
            verifyNoInteractions(fraudAlertRepository);
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
