package com.keza.notification.domain.service;

import com.keza.notification.domain.model.Notification;
import com.keza.notification.domain.model.NotificationChannel;
import com.keza.notification.domain.model.NotificationPreference;
import com.keza.notification.domain.port.out.EmailSender;
import com.keza.notification.domain.port.out.NotificationPreferenceRepository;
import com.keza.notification.domain.port.out.NotificationRepository;
import com.keza.notification.domain.port.out.SmsSender;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private SmsSender smsSender;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private Notification savedNotification(Notification notification) {
        notification.setId(UUID.randomUUID());
        return notification;
    }

    @Nested
    @DisplayName("sendNotification")
    class SendNotification {

        @Nested
        @DisplayName("with user preferences")
        class WithUserPreferences {

            @Test
            @DisplayName("should send notification when channel is enabled in preferences")
            void shouldSendWhenChannelEnabled() {
                NotificationPreference pref = NotificationPreference.builder()
                        .userId(userId)
                        .emailEnabled(true)
                        .smsEnabled(true)
                        .pushEnabled(true)
                        .build();

                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
                when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(UUID.randomUUID());
                    return n;
                });

                Notification result = notificationService.sendNotification(
                        userId, "INVESTMENT_CONFIRMED", "Investment Done", "Your investment is confirmed",
                        NotificationChannel.IN_APP);

                assertThat(result).isNotNull();
                assertThat(result.getType()).isEqualTo("INVESTMENT_CONFIRMED");
                assertThat(result.getChannel()).isEqualTo(NotificationChannel.IN_APP);
                verify(notificationRepository, times(2)).save(any(Notification.class));
            }

            @Test
            @DisplayName("should skip notification when channel is disabled by user preference")
            void shouldSkipWhenChannelDisabled() {
                NotificationPreference pref = NotificationPreference.builder()
                        .userId(userId)
                        .emailEnabled(false)
                        .smsEnabled(false)
                        .pushEnabled(false)
                        .build();

                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

                Notification result = notificationService.sendNotification(
                        userId, "CAMPAIGN_FUNDED", "Campaign Funded!", "A campaign was funded",
                        NotificationChannel.EMAIL);

                assertThat(result).isNull();
                verify(notificationRepository, never()).save(any());
            }

            @Test
            @DisplayName("should skip SMS notification when SMS is disabled")
            void shouldSkipSmsWhenDisabled() {
                NotificationPreference pref = NotificationPreference.builder()
                        .userId(userId)
                        .emailEnabled(true)
                        .smsEnabled(false)
                        .pushEnabled(true)
                        .build();

                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

                Notification result = notificationService.sendNotification(
                        userId, "PAYMENT_RECEIVED", "Payment", "Payment received",
                        NotificationChannel.SMS);

                assertThat(result).isNull();
                verify(notificationRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("critical notifications")
        class CriticalNotifications {

            @Test
            @DisplayName("should send PASSWORD_RESET even when email is disabled")
            void shouldSendPasswordResetRegardlessOfPreference() {
                NotificationPreference pref = NotificationPreference.builder()
                        .userId(userId)
                        .emailEnabled(false)
                        .build();

                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
                when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(UUID.randomUUID());
                    return n;
                });

                Notification result = notificationService.sendNotification(
                        userId, "PASSWORD_RESET", "Reset Password", "Click here to reset",
                        NotificationChannel.EMAIL);

                assertThat(result).isNotNull();
                verify(notificationRepository, times(2)).save(any());
            }

            @Test
            @DisplayName("should send SECURITY_ALERT even when all channels are disabled")
            void shouldSendSecurityAlertRegardless() {
                NotificationPreference pref = NotificationPreference.builder()
                        .userId(userId)
                        .emailEnabled(false)
                        .smsEnabled(false)
                        .pushEnabled(false)
                        .build();

                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
                when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(UUID.randomUUID());
                    return n;
                });

                Notification result = notificationService.sendNotification(
                        userId, "SECURITY_ALERT", "Security Alert", "Suspicious login detected",
                        NotificationChannel.PUSH);

                assertThat(result).isNotNull();
            }

            @Test
            @DisplayName("should send ACCOUNT_LOCKED bypass preferences")
            void shouldSendAccountLockedBypassPreferences() {
                NotificationPreference pref = NotificationPreference.builder()
                        .userId(userId)
                        .emailEnabled(false)
                        .smsEnabled(false)
                        .pushEnabled(false)
                        .build();

                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
                when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(UUID.randomUUID());
                    return n;
                });

                Notification result = notificationService.sendNotification(
                        userId, "ACCOUNT_LOCKED", "Account Locked", "Your account is locked",
                        NotificationChannel.SMS);

                assertThat(result).isNotNull();
            }

            @Test
            @DisplayName("should send EMAIL_VERIFICATION bypass preferences")
            void shouldSendEmailVerificationBypassPreferences() {
                NotificationPreference pref = NotificationPreference.builder()
                        .userId(userId)
                        .emailEnabled(false)
                        .build();

                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
                when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(UUID.randomUUID());
                    return n;
                });

                Notification result = notificationService.sendNotification(
                        userId, "EMAIL_VERIFICATION", "Verify Email", "Please verify your email",
                        NotificationChannel.EMAIL);

                assertThat(result).isNotNull();
            }
        }

        @Nested
        @DisplayName("default preferences")
        class DefaultPreferences {

            @Test
            @DisplayName("should default to enabled when no preferences exist")
            void shouldDefaultToEnabledWhenNoPreferences() {
                when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
                when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(UUID.randomUUID());
                    return n;
                });

                Notification result = notificationService.sendNotification(
                        userId, "CAMPAIGN_FUNDED", "Funded!", "Campaign funded",
                        NotificationChannel.EMAIL);

                assertThat(result).isNotNull();
                verify(notificationRepository, times(2)).save(any());
            }
        }

        @Test
        @DisplayName("should store notification with correct fields")
        void shouldStoreNotificationWithCorrectFields() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.sendNotification(
                    userId, "TEST_TYPE", "Test Title", "Test Message",
                    NotificationChannel.IN_APP, "{\"key\":\"value\"}");

            verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getAllValues().get(0);

            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getType()).isEqualTo("TEST_TYPE");
            assertThat(saved.getTitle()).isEqualTo("Test Title");
            assertThat(saved.getMessage()).isEqualTo("Test Message");
            assertThat(saved.getChannel()).isEqualTo(NotificationChannel.IN_APP);
            assertThat(saved.getData()).isEqualTo("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("should mark notification as sent after successful dispatch")
        void shouldMarkAsSentAfterDispatch() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                if (n.getId() == null) n.setId(UUID.randomUUID());
                return n;
            });

            Notification result = notificationService.sendNotification(
                    userId, "TEST", "Title", "Message", NotificationChannel.IN_APP);

            assertThat(result).isNotNull();
            assertThat(result.isSent()).isTrue();
            assertThat(result.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("should always enable IN_APP channel regardless of preferences")
        void shouldAlwaysEnableInAppChannel() {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId)
                    .emailEnabled(false)
                    .smsEnabled(false)
                    .pushEnabled(false)
                    .build();

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            Notification result = notificationService.sendNotification(
                    userId, "CAMPAIGN_FUNDED", "Funded", "Campaign funded",
                    NotificationChannel.IN_APP);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("sendEmailNotification")
    class SendEmailNotification {

        @Test
        @DisplayName("should send email when email channel is enabled")
        void shouldSendEmailWhenEnabled() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                if (n.getId() == null) n.setId(UUID.randomUUID());
                return n;
            });

            Notification result = notificationService.sendEmailNotification(
                    userId, "INVESTMENT_CONFIRMED", "Investment Done", "Confirmed",
                    "user@example.com", "<h1>Hello</h1>", null);

            assertThat(result).isNotNull();
            verify(emailSender).send("user@example.com", "Investment Done", "<h1>Hello</h1>");
        }

        @Test
        @DisplayName("should skip email when email channel is disabled for non-critical type")
        void shouldSkipEmailWhenDisabled() {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId)
                    .emailEnabled(false)
                    .build();

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            Notification result = notificationService.sendEmailNotification(
                    userId, "CAMPAIGN_FUNDED", "Funded", "Funded",
                    "user@example.com", "<h1>Hello</h1>", null);

            assertThat(result).isNull();
            verify(emailSender, never()).send(any(), any(), any());
        }

        @Test
        @DisplayName("should send critical email even when email is disabled")
        void shouldSendCriticalEmailRegardless() {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId)
                    .emailEnabled(false)
                    .build();

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                if (n.getId() == null) n.setId(UUID.randomUUID());
                return n;
            });

            Notification result = notificationService.sendEmailNotification(
                    userId, "PASSWORD_RESET", "Reset", "Reset your password",
                    "user@example.com", "<h1>Reset</h1>", null);

            assertThat(result).isNotNull();
            verify(emailSender).send("user@example.com", "Reset", "<h1>Reset</h1>");
        }

        @Test
        @DisplayName("should handle email sender failure gracefully")
        void shouldHandleEmailSenderFailure() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                if (n.getId() == null) n.setId(UUID.randomUUID());
                return n;
            });
            doThrow(new RuntimeException("SMTP error")).when(emailSender).send(any(), any(), any());

            Notification result = notificationService.sendEmailNotification(
                    userId, "TEST", "Title", "Body",
                    "user@example.com", "<h1>Body</h1>", null);

            // Should not throw, notification is still returned (but not marked as sent)
            assertThat(result).isNotNull();
            assertThat(result.isSent()).isFalse();
        }
    }

    @Nested
    @DisplayName("sendSmsNotification")
    class SendSmsNotification {

        @Test
        @DisplayName("should send SMS when SMS channel is enabled")
        void shouldSendSmsWhenEnabled() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                if (n.getId() == null) n.setId(UUID.randomUUID());
                return n;
            });

            Notification result = notificationService.sendSmsNotification(
                    userId, "PAYMENT_RECEIVED", "Payment", "Payment received",
                    "+254712345678", null);

            assertThat(result).isNotNull();
            verify(smsSender).send("+254712345678", "Payment received");
        }

        @Test
        @DisplayName("should skip SMS when SMS channel is disabled for non-critical type")
        void shouldSkipSmsWhenDisabled() {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId)
                    .smsEnabled(false)
                    .build();

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            Notification result = notificationService.sendSmsNotification(
                    userId, "CAMPAIGN_FUNDED", "Funded", "Funded",
                    "+254712345678", null);

            assertThat(result).isNull();
            verify(smsSender, never()).send(any(), any());
        }

        @Test
        @DisplayName("should handle SMS sender failure gracefully")
        void shouldHandleSmsSenderFailure() {
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                if (n.getId() == null) n.setId(UUID.randomUUID());
                return n;
            });
            doThrow(new RuntimeException("SMS gateway error")).when(smsSender).send(any(), any());

            Notification result = notificationService.sendSmsNotification(
                    userId, "TEST", "Title", "Body",
                    "+254712345678", null);

            assertThat(result).isNotNull();
            assertThat(result.isSent()).isFalse();
        }
    }
}
