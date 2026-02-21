package com.keza.app.listener;

import com.keza.notification.domain.service.NotificationService;
import com.keza.user.domain.event.EmailVerificationRequestedEvent;
import com.keza.user.domain.event.PasswordResetRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEmailEventListener")
class UserEmailEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserEmailEventListener listener;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";
    private static final String FIRST_NAME = "Jane";
    private static final String TOKEN = "test-token-uuid";
    private static final String FRONTEND_URL = "https://keza.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "frontendUrl", FRONTEND_URL);
    }

    @Nested
    @DisplayName("handlePasswordResetRequested")
    class HandlePasswordResetRequested {

        @Test
        @DisplayName("should send password reset email with correct parameters")
        void shouldSendPasswordResetEmail() {
            PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                    USER_ID, EMAIL, FIRST_NAME, TOKEN);

            listener.handlePasswordResetRequested(event);

            verify(notificationService).sendEmailNotification(
                    eq(USER_ID),
                    eq("PASSWORD_RESET"),
                    eq("Reset Your Password - Keza"),
                    contains("password reset"),
                    eq(EMAIL),
                    argThat(html -> html.contains("Reset Your Password")
                            && html.contains(FIRST_NAME)
                            && html.contains(FRONTEND_URL + "/reset-password?token=" + TOKEN)),
                    isNull()
            );
        }

        @Test
        @DisplayName("should include reset URL in email body")
        void shouldIncludeResetUrl() {
            PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                    USER_ID, EMAIL, FIRST_NAME, TOKEN);

            listener.handlePasswordResetRequested(event);

            verify(notificationService).sendEmailNotification(
                    any(), any(), any(), any(), any(),
                    argThat(html -> html.contains(FRONTEND_URL + "/reset-password?token=" + TOKEN)),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("handleEmailVerificationRequested")
    class HandleEmailVerificationRequested {

        @Test
        @DisplayName("should send email verification with correct parameters")
        void shouldSendEmailVerification() {
            EmailVerificationRequestedEvent event = new EmailVerificationRequestedEvent(
                    USER_ID, EMAIL, FIRST_NAME, TOKEN);

            listener.handleEmailVerificationRequested(event);

            verify(notificationService).sendEmailNotification(
                    eq(USER_ID),
                    eq("EMAIL_VERIFICATION"),
                    eq("Verify Your Email - Keza"),
                    contains("verify your email"),
                    eq(EMAIL),
                    argThat(html -> html.contains("Verify Your Email")
                            && html.contains(FIRST_NAME)
                            && html.contains(FRONTEND_URL + "/verify-email?token=" + TOKEN)),
                    isNull()
            );
        }

        @Test
        @DisplayName("should include verification URL in email body")
        void shouldIncludeVerificationUrl() {
            EmailVerificationRequestedEvent event = new EmailVerificationRequestedEvent(
                    USER_ID, EMAIL, FIRST_NAME, TOKEN);

            listener.handleEmailVerificationRequested(event);

            verify(notificationService).sendEmailNotification(
                    any(), any(), any(), any(), any(),
                    argThat(html -> html.contains(FRONTEND_URL + "/verify-email?token=" + TOKEN)),
                    any()
            );
        }
    }
}
