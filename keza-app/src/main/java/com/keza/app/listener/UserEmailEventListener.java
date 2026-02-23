package com.keza.app.listener;

import com.keza.notification.domain.model.NotificationChannel;
import com.keza.notification.domain.service.NotificationService;
import com.keza.user.domain.event.EmailVerificationRequestedEvent;
import com.keza.user.domain.event.PasswordResetRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for user-related Spring application events and triggers
 * email notifications via the notification module.
 *
 * <p>This listener lives in keza-app because it bridges the keza-user module
 * (event source) and the keza-notification module (email sending), which
 * do not directly depend on each other.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEmailEventListener {

    private final NotificationService notificationService;

    @Value("${keza.notification.frontend-url:https://keza.com}")
    private String frontendUrl;

    @Async
    @EventListener
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("Handling email verification request for user: {}", event.email());

        String verificationUrl = frontendUrl + "/verify-email?token=" + event.token();
        String htmlBody = buildEmailVerificationHtml(event.firstName(), verificationUrl);

        notificationService.sendEmailNotification(
                event.userId(),
                "EMAIL_VERIFICATION",
                "Verify Your Email - Keza",
                "Please verify your email address to complete your Keza registration.",
                event.email(),
                htmlBody,
                null
        );

        log.info("Email verification email sent to {}", event.email());
    }

    @Async
    @EventListener
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Handling password reset request for user: {}", event.email());

        String resetUrl = frontendUrl + "/reset-password?token=" + event.token();
        String htmlBody = buildPasswordResetHtml(event.firstName(), resetUrl);

        notificationService.sendEmailNotification(
                event.userId(),
                "PASSWORD_RESET",
                "Reset Your Password - Keza",
                "You requested a password reset for your Keza account.",
                event.email(),
                htmlBody,
                null
        );

        log.info("Password reset email sent to {}", event.email());
    }

    private String buildEmailVerificationHtml(String firstName, String verificationUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                    <div style="background-color: #1a5632; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="color: #ffffff; margin: 0;">Verify Your Email</h1>
                    </div>
                    <div style="background-color: #ffffff; padding: 30px; border-radius: 0 0 8px 8px;">
                        <p>Hello %s,</p>
                        <p>Thank you for registering with <strong>Keza</strong>. Please verify your email address by clicking the button below:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s"
                               style="background-color: #1a5632; color: white; padding: 14px 35px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">
                                Verify Email
                            </a>
                        </div>
                        <p style="color: #666; font-size: 14px;">If the button doesn't work, copy and paste this link into your browser:</p>
                        <p style="color: #1a5632; font-size: 14px; word-break: break-all;">%s</p>
                        <p style="color: #666; font-size: 14px;">This link will expire in 24 hours.</p>
                        <p>If you didn't create an account on Keza, you can safely ignore this email.</p>
                        <p>Best regards,<br><strong>The Keza Team</strong></p>
                    </div>
                    <div style="text-align: center; padding: 15px; color: #888888; font-size: 12px;">
                        <p>&copy; 2026 Keza. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(firstName, verificationUrl, verificationUrl);
    }

    private String buildPasswordResetHtml(String firstName, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                    <div style="background-color: #1a5632; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="color: #ffffff; margin: 0;">Reset Your Password</h1>
                    </div>
                    <div style="background-color: #ffffff; padding: 30px; border-radius: 0 0 8px 8px;">
                        <p>Hello %s,</p>
                        <p>We received a request to reset the password for your <strong>Keza</strong> account. Click the button below to set a new password:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s"
                               style="background-color: #1a5632; color: white; padding: 14px 35px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">
                                Reset Password
                            </a>
                        </div>
                        <p style="color: #666; font-size: 14px;">If the button doesn't work, copy and paste this link into your browser:</p>
                        <p style="color: #1a5632; font-size: 14px; word-break: break-all;">%s</p>
                        <p style="color: #666; font-size: 14px;">This link will expire in 1 hour.</p>
                        <div style="background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 5px; padding: 15px; margin: 20px 0;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">
                                <strong>Didn't request this?</strong> If you didn't request a password reset, please ignore this email. Your password will remain unchanged.
                            </p>
                        </div>
                        <p>Best regards,<br><strong>The Keza Team</strong></p>
                    </div>
                    <div style="text-align: center; padding: 15px; color: #888888; font-size: 12px;">
                        <p>&copy; 2026 Keza. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(firstName, resetUrl, resetUrl);
    }
}
