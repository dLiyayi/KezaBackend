package com.keza.payment.adapter.in.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.keza.infrastructure.config.RabbitMQConfig.*;

/**
 * Listens for payment callback events on the RabbitMQ payment callback queue.
 * <p>
 * This listener consumes payment events published by {@code PaymentUseCase.handlePaymentCallback}
 * and re-publishes enriched domain events to the payment exchange so that downstream modules
 * (investment, notification) can react accordingly via their own dedicated queues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackListener {

    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = PAYMENT_CALLBACK_QUEUE)
    public void handlePaymentCallbackEvent(
            @Payload Map<String, Object> event,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            Channel channel) throws IOException {

        String providerReference = extractString(event, "providerReference");
        String status = extractString(event, "status");

        log.info("Received payment callback event from queue. providerReference={}, status={}", providerReference, status);

        try {
            processPaymentEvent(event);

            // Acknowledge the message
            channel.basicAck(deliveryTag, false);
            log.info("Payment callback event processed and acknowledged. providerReference={}", providerReference);

        } catch (Exception e) {
            log.error("Failed to process payment callback event. providerReference={}, error={}",
                    providerReference, e.getMessage(), e);

            // Reject and send to DLQ (do not requeue)
            channel.basicNack(deliveryTag, false, false);
            log.warn("Payment callback event rejected and sent to DLQ. providerReference={}", providerReference);
        }
    }

    private void processPaymentEvent(Map<String, Object> event) {
        String status = extractString(event, "status");
        String providerReference = extractString(event, "providerReference");

        switch (status) {
            case "COMPLETED" -> handlePaymentCompleted(providerReference, event);
            case "FAILED" -> handlePaymentFailed(providerReference, event);
            case "REFUNDED" -> handlePaymentRefunded(providerReference, event);
            default -> log.warn("Unknown payment status received: {} for providerReference: {}", status, providerReference);
        }
    }

    private void handlePaymentCompleted(String providerReference, Map<String, Object> event) {
        log.info("Processing COMPLETED payment event for providerReference: {}", providerReference);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) event.get("metadata");

        Map<String, Object> domainEvent = new HashMap<>();
        domainEvent.put("eventType", "PAYMENT_COMPLETED");
        domainEvent.put("providerReference", providerReference);
        domainEvent.put("timestamp", System.currentTimeMillis());

        if (metadata != null) {
            domainEvent.put("transactionId", extractString(metadata, "transactionId"));
            domainEvent.put("investmentId", extractString(metadata, "investmentId"));
            domainEvent.put("amount", metadata.get("amount"));
            domainEvent.put("userId", extractString(metadata, "userId"));
            domainEvent.put("campaignName", extractString(metadata, "campaignName"));
            domainEvent.put("currency", metadata.getOrDefault("currency", "KES"));
            log.debug("Payment metadata: {}", metadata);
        }

        publishPaymentDomainEvent(domainEvent);
        log.info("Published PAYMENT_COMPLETED domain event for providerReference: {}", providerReference);
    }

    private void handlePaymentFailed(String providerReference, Map<String, Object> event) {
        log.info("Processing FAILED payment event for providerReference: {}", providerReference);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) event.get("metadata");

        Map<String, Object> domainEvent = new HashMap<>();
        domainEvent.put("eventType", "PAYMENT_FAILED");
        domainEvent.put("providerReference", providerReference);
        domainEvent.put("failureReason", extractString(event, "failureReason"));
        domainEvent.put("timestamp", System.currentTimeMillis());

        if (metadata != null) {
            domainEvent.put("transactionId", extractString(metadata, "transactionId"));
            domainEvent.put("investmentId", extractString(metadata, "investmentId"));
            domainEvent.put("userId", extractString(metadata, "userId"));
            domainEvent.put("campaignName", extractString(metadata, "campaignName"));
            domainEvent.put("currency", metadata.getOrDefault("currency", "KES"));
            domainEvent.put("amount", metadata.get("amount"));
        }

        publishPaymentDomainEvent(domainEvent);
        log.info("Published PAYMENT_FAILED domain event for providerReference: {}", providerReference);
    }

    private void handlePaymentRefunded(String providerReference, Map<String, Object> event) {
        log.info("Processing REFUNDED payment event for providerReference: {}", providerReference);

        String transactionId = extractString(event, "transactionId");
        String refundReference = extractString(event, "refundReference");

        log.info("Refund processed. transactionId={}, refundReference={}", transactionId, refundReference);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) event.get("metadata");

        Map<String, Object> domainEvent = new HashMap<>();
        domainEvent.put("eventType", "PAYMENT_REFUNDED");
        domainEvent.put("providerReference", providerReference);
        domainEvent.put("transactionId", transactionId);
        domainEvent.put("refundReference", refundReference);
        domainEvent.put("amount", event.get("amount"));
        domainEvent.put("timestamp", System.currentTimeMillis());

        if (metadata != null) {
            domainEvent.put("investmentId", extractString(metadata, "investmentId"));
            domainEvent.put("userId", extractString(metadata, "userId"));
            domainEvent.put("campaignName", extractString(metadata, "campaignName"));
            domainEvent.put("currency", metadata.getOrDefault("currency", "KES"));
        }

        publishPaymentDomainEvent(domainEvent);
        log.info("Published PAYMENT_REFUNDED domain event for providerReference: {}", providerReference);
    }

    /**
     * Publishes a payment domain event to the payment exchange.
     * Both the investment-payment queue and notification-payment queue are bound
     * to this exchange with the same routing key, so both modules receive the event.
     */
    private void publishPaymentDomainEvent(Map<String, Object> domainEvent) {
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, PAYMENT_ROUTING_KEY, domainEvent);
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
