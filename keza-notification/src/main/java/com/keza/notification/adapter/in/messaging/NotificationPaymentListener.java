package com.keza.notification.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.notification.domain.model.NotificationChannel;
import com.keza.notification.domain.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static com.keza.infrastructure.config.RabbitMQConfig.NOTIFICATION_PAYMENT_QUEUE;

/**
 * Listens for payment domain events on the notification-payment queue.
 * <p>
 * Sends notifications to users for payment completions, failures, and refunds.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPaymentListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = NOTIFICATION_PAYMENT_QUEUE)
    public void handlePaymentEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            JsonNode event = objectMapper.readTree(message.getBody());

            String eventType = event.path("eventType").asText();
            String providerReference = event.path("providerReference").asText();

            log.info("Received payment notification event: eventType={}, providerReference={}",
                    eventType, providerReference);

            switch (eventType) {
                case "PAYMENT_COMPLETED" -> handlePaymentCompleted(event);
                case "PAYMENT_FAILED" -> handlePaymentFailed(event);
                case "PAYMENT_REFUNDED" -> handlePaymentRefunded(event);
                default -> log.warn("Unknown payment event type for notification: {}", eventType);
            }

            channel.basicAck(deliveryTag, false);
            log.debug("Payment notification event acknowledged: eventType={}, deliveryTag={}",
                    eventType, deliveryTag);

        } catch (Exception e) {
            log.error("Failed to process payment notification event: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void handlePaymentCompleted(JsonNode event) {
        String userIdStr = event.path("userId").asText(null);
        if (userIdStr == null || userIdStr.isEmpty() || "null".equals(userIdStr)) {
            log.warn("No userId in PAYMENT_COMPLETED event, skipping notification");
            return;
        }

        UUID userId = UUID.fromString(userIdStr);
        String amount = event.path("amount").asText("0");
        String currency = event.path("currency").asText("KES");
        String campaignName = event.path("campaignName").asText("your selected campaign");

        String title = "Payment Confirmed";
        String notificationMessage = "Your payment of " + currency + " " + amount
                + " for investment in " + campaignName + " has been confirmed.";

        String data = String.format(
                "{\"providerReference\":\"%s\",\"amount\":\"%s\",\"currency\":\"%s\",\"campaignName\":\"%s\"}",
                event.path("providerReference").asText(""),
                amount, currency, campaignName);

        notificationService.sendNotification(
                userId, "PAYMENT_COMPLETED", title, notificationMessage,
                NotificationChannel.IN_APP, data);

        log.info("Sent PAYMENT_COMPLETED notification to user {}", userId);
    }

    private void handlePaymentFailed(JsonNode event) {
        String userIdStr = event.path("userId").asText(null);
        if (userIdStr == null || userIdStr.isEmpty() || "null".equals(userIdStr)) {
            log.warn("No userId in PAYMENT_FAILED event, skipping notification");
            return;
        }

        UUID userId = UUID.fromString(userIdStr);
        String amount = event.path("amount").asText("0");
        String currency = event.path("currency").asText("KES");
        String failureReason = event.path("failureReason").asText("An error occurred during payment processing");
        String campaignName = event.path("campaignName").asText("your selected campaign");

        String title = "Payment Failed";
        String notificationMessage = "Your payment of " + currency + " " + amount
                + " for investment in " + campaignName + " could not be processed. "
                + "Reason: " + failureReason + ". Please try again or use a different payment method.";

        String data = String.format(
                "{\"providerReference\":\"%s\",\"failureReason\":\"%s\",\"campaignName\":\"%s\"}",
                event.path("providerReference").asText(""),
                failureReason, campaignName);

        notificationService.sendNotification(
                userId, "PAYMENT_FAILED", title, notificationMessage,
                NotificationChannel.IN_APP, data);

        log.info("Sent PAYMENT_FAILED notification to user {}", userId);
    }

    private void handlePaymentRefunded(JsonNode event) {
        String userIdStr = event.path("userId").asText(null);
        if (userIdStr == null || userIdStr.isEmpty() || "null".equals(userIdStr)) {
            log.warn("No userId in PAYMENT_REFUNDED event, skipping notification");
            return;
        }

        UUID userId = UUID.fromString(userIdStr);
        String amount = event.path("amount").asText("0");
        String currency = event.path("currency").asText("KES");
        String refundReference = event.path("refundReference").asText("");
        String campaignName = event.path("campaignName").asText("your selected campaign");

        String title = "Payment Refunded";
        String notificationMessage = "Your payment of " + currency + " " + amount
                + " for investment in " + campaignName + " has been refunded."
                + (refundReference.isEmpty() ? "" : " Refund reference: " + refundReference + ".");

        String data = String.format(
                "{\"providerReference\":\"%s\",\"refundReference\":\"%s\",\"amount\":\"%s\",\"currency\":\"%s\",\"campaignName\":\"%s\"}",
                event.path("providerReference").asText(""),
                refundReference, amount, currency, campaignName);

        notificationService.sendNotification(
                userId, "PAYMENT_REFUNDED", title, notificationMessage,
                NotificationChannel.IN_APP, data);

        log.info("Sent PAYMENT_REFUNDED notification to user {}", userId);
    }
}
