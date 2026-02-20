package com.keza.notification.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.infrastructure.config.RabbitMQConfig;
import com.keza.notification.domain.model.NotificationChannel;
import com.keza.notification.domain.port.out.EmailSender;
import com.keza.notification.domain.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            JsonNode event = objectMapper.readTree(message.getBody());

            String eventType = event.path("type").asText();
            String userId = event.path("userId").asText();

            log.info("Received notification event: type={}, userId={}", eventType, userId);

            switch (eventType) {
                case "USER_REGISTERED" -> handleUserRegistered(event);
                case "KYC_APPROVED" -> handleKycStatusChange(event, "approved");
                case "KYC_REJECTED" -> handleKycStatusChange(event, "rejected");
                case "KYC_PENDING_REVIEW" -> handleKycStatusChange(event, "pending_review");
                case "INVESTMENT_CONFIRMED" -> handleInvestmentConfirmed(event);
                case "PAYMENT_RECEIVED" -> handlePaymentReceived(event);
                case "CAMPAIGN_FUNDED" -> handleCampaignFunded(event);
                default -> {
                    log.warn("Unknown notification event type: {}", eventType);
                    handleGenericEvent(event);
                }
            }

            // Manual ACK
            channel.basicAck(deliveryTag, false);
            log.debug("Acknowledged notification event: type={}, deliveryTag={}", eventType, deliveryTag);

        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
            // Reject and do not requeue (send to DLQ)
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void handleUserRegistered(JsonNode event) {
        UUID userId = UUID.fromString(event.path("userId").asText());
        String email = event.path("email").asText();
        String firstName = event.path("firstName").asText();

        // Send welcome in-app notification
        notificationService.sendNotification(
                userId,
                "USER_REGISTERED",
                "Welcome to Keza!",
                "Welcome to Keza, " + firstName + "! Start exploring investment opportunities.",
                NotificationChannel.IN_APP
        );

        // Send welcome email
        if (!email.isEmpty()) {
            String htmlBody = buildWelcomeEmailHtml(firstName);
            notificationService.sendEmailNotification(
                    userId,
                    "USER_REGISTERED",
                    "Welcome to Keza!",
                    "Welcome to Keza, " + firstName + "!",
                    email,
                    htmlBody,
                    null
            );
        }

        log.info("Processed USER_REGISTERED event for user {}", userId);
    }

    private void handleKycStatusChange(JsonNode event, String status) {
        UUID userId = UUID.fromString(event.path("userId").asText());
        String email = event.path("email").asText("");
        String firstName = event.path("firstName").asText("User");

        String title;
        String message;

        switch (status) {
            case "approved" -> {
                title = "KYC Verification Approved";
                message = "Congratulations " + firstName + "! Your identity verification has been approved. You can now invest in campaigns.";
            }
            case "rejected" -> {
                title = "KYC Verification Update";
                message = "Hello " + firstName + ", your identity verification requires attention. Please review and resubmit your documents.";
            }
            case "pending_review" -> {
                title = "KYC Documents Under Review";
                message = "Hello " + firstName + ", your documents have been received and are under review. We'll notify you once the review is complete.";
            }
            default -> {
                title = "KYC Status Update";
                message = "Hello " + firstName + ", there has been an update to your KYC verification status.";
            }
        }

        // In-app notification
        notificationService.sendNotification(userId, "KYC_" + status.toUpperCase(), title, message, NotificationChannel.IN_APP);

        // Email notification
        if (!email.isEmpty()) {
            String htmlBody = buildKycStatusEmailHtml(firstName, status, message);
            notificationService.sendEmailNotification(
                    userId, "KYC_" + status.toUpperCase(), title, message, email, htmlBody, null
            );
        }

        log.info("Processed KYC status change ({}) for user {}", status, userId);
    }

    private void handleInvestmentConfirmed(JsonNode event) {
        UUID userId = UUID.fromString(event.path("userId").asText());
        String email = event.path("email").asText("");
        String firstName = event.path("firstName").asText("User");
        String campaignName = event.path("campaignName").asText("a campaign");
        String amount = event.path("amount").asText("0");
        String currency = event.path("currency").asText("KES");

        String title = "Investment Confirmed";
        String message = "Your investment of " + currency + " " + amount + " in " + campaignName + " has been confirmed.";

        String data = String.format("{\"campaignName\":\"%s\",\"amount\":\"%s\",\"currency\":\"%s\"}", campaignName, amount, currency);

        // In-app notification
        notificationService.sendNotification(userId, "INVESTMENT_CONFIRMED", title, message, NotificationChannel.IN_APP, data);

        // Email notification
        if (!email.isEmpty()) {
            String htmlBody = buildInvestmentConfirmationEmailHtml(firstName, campaignName, amount, currency);
            notificationService.sendEmailNotification(
                    userId, "INVESTMENT_CONFIRMED", title, message, email, htmlBody, data
            );
        }

        log.info("Processed INVESTMENT_CONFIRMED event for user {}", userId);
    }

    private void handlePaymentReceived(JsonNode event) {
        UUID userId = UUID.fromString(event.path("userId").asText());
        String amount = event.path("amount").asText("0");
        String currency = event.path("currency").asText("KES");

        String title = "Payment Received";
        String message = "Your payment of " + currency + " " + amount + " has been received and is being processed.";

        notificationService.sendNotification(userId, "PAYMENT_RECEIVED", title, message, NotificationChannel.IN_APP);
        log.info("Processed PAYMENT_RECEIVED event for user {}", userId);
    }

    private void handleCampaignFunded(JsonNode event) {
        UUID userId = UUID.fromString(event.path("userId").asText());
        String campaignName = event.path("campaignName").asText("a campaign");

        String title = "Campaign Fully Funded!";
        String message = "Great news! The campaign '" + campaignName + "' you invested in has been fully funded.";

        notificationService.sendNotification(userId, "CAMPAIGN_FUNDED", title, message, NotificationChannel.IN_APP);
        log.info("Processed CAMPAIGN_FUNDED event for user {}", userId);
    }

    private void handleGenericEvent(JsonNode event) {
        String userId = event.path("userId").asText();
        String type = event.path("type").asText("GENERIC");
        String title = event.path("title").asText("Notification");
        String message = event.path("message").asText("");

        if (!userId.isEmpty() && !message.isEmpty()) {
            notificationService.sendNotification(
                    UUID.fromString(userId), type, title, message, NotificationChannel.IN_APP
            );
        }
    }

    // --- HTML email builders ---

    private String buildWelcomeEmailHtml(String firstName) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #1a5632; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="color: #ffffff; margin: 0;">Welcome to Keza!</h1>
                    </div>
                    <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                        <p>Hello %s,</p>
                        <p>Thank you for joining Keza, East Africa's premier equity crowdfunding platform.</p>
                        <p>Here's what you can do next:</p>
                        <ul>
                            <li>Complete your KYC verification to start investing</li>
                            <li>Browse available investment opportunities</li>
                            <li>Set up your notification preferences</li>
                        </ul>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="https://keza.com/dashboard" style="background-color: #1a5632; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Go to Dashboard</a>
                        </div>
                        <p>If you have any questions, our support team is here to help.</p>
                        <p>Best regards,<br>The Keza Team</p>
                    </div>
                    <div style="text-align: center; padding: 15px; color: #888; font-size: 12px;">
                        <p>&copy; 2026 Keza. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(firstName);
    }

    private String buildKycStatusEmailHtml(String firstName, String status, String messageText) {
        String statusColor = switch (status) {
            case "approved" -> "#28a745";
            case "rejected" -> "#dc3545";
            default -> "#ffc107";
        };

        String statusLabel = switch (status) {
            case "approved" -> "Approved";
            case "rejected" -> "Needs Attention";
            case "pending_review" -> "Under Review";
            default -> "Updated";
        };

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #1a5632; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="color: #ffffff; margin: 0;">KYC Verification Update</h1>
                    </div>
                    <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                        <p>Hello %s,</p>
                        <div style="background-color: %s; color: white; padding: 10px 20px; border-radius: 5px; text-align: center; margin: 20px 0;">
                            <strong>Status: %s</strong>
                        </div>
                        <p>%s</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="https://keza.com/kyc" style="background-color: #1a5632; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">View KYC Status</a>
                        </div>
                        <p>Best regards,<br>The Keza Team</p>
                    </div>
                    <div style="text-align: center; padding: 15px; color: #888; font-size: 12px;">
                        <p>&copy; 2026 Keza. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(firstName, statusColor, statusLabel, messageText);
    }

    private String buildInvestmentConfirmationEmailHtml(String firstName, String campaignName, String amount, String currency) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #1a5632; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="color: #ffffff; margin: 0;">Investment Confirmed</h1>
                    </div>
                    <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                        <p>Hello %s,</p>
                        <p>Your investment has been confirmed! Here are the details:</p>
                        <table style="width: 100%%; background-color: white; border-radius: 5px; padding: 15px; margin: 20px 0;">
                            <tr>
                                <td style="padding: 8px; color: #666;">Campaign:</td>
                                <td style="padding: 8px; font-weight: bold;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px; color: #666;">Amount:</td>
                                <td style="padding: 8px; font-weight: bold;">%s %s</td>
                            </tr>
                        </table>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="https://keza.com/investments" style="background-color: #1a5632; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">View Your Investments</a>
                        </div>
                        <p>Thank you for investing with Keza!</p>
                        <p>Best regards,<br>The Keza Team</p>
                    </div>
                    <div style="text-align: center; padding: 15px; color: #888; font-size: 12px;">
                        <p>&copy; 2026 Keza. All rights reserved.</p>
                    </div>
                </body>
                </html>
                """.formatted(firstName, campaignName, currency, amount);
    }
}
