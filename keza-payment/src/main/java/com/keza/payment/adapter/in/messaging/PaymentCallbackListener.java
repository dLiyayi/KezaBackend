package com.keza.payment.adapter.in.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static com.keza.infrastructure.config.RabbitMQConfig.PAYMENT_CALLBACK_QUEUE;

/**
 * Listens for payment callback events on the RabbitMQ payment callback queue.
 * <p>
 * This listener consumes payment events published by {@code PaymentUseCase.handlePaymentCallback}
 * and can be used to trigger downstream processes such as:
 * <ul>
 *   <li>Updating transaction/investment status in the database</li>
 *   <li>Sending notifications to users</li>
 *   <li>Triggering escrow or settlement workflows</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackListener {

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

        // TODO: Update transaction status to COMPLETED in the database
        // TODO: Update investment status if applicable
        // TODO: Trigger notification to the investor
        // TODO: Trigger escrow deposit if KCB escrow is involved

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) event.get("metadata");
        if (metadata != null) {
            log.debug("Payment metadata: {}", metadata);
        }
    }

    private void handlePaymentFailed(String providerReference, Map<String, Object> event) {
        log.info("Processing FAILED payment event for providerReference: {}", providerReference);

        // TODO: Update transaction status to FAILED in the database
        // TODO: Notify the user about the failure
        // TODO: Clean up any pending investment records
    }

    private void handlePaymentRefunded(String providerReference, Map<String, Object> event) {
        log.info("Processing REFUNDED payment event for providerReference: {}", providerReference);

        String transactionId = extractString(event, "transactionId");
        String refundReference = extractString(event, "refundReference");

        log.info("Refund processed. transactionId={}, refundReference={}", transactionId, refundReference);

        // TODO: Update transaction status to REFUNDED in the database
        // TODO: Update investment status
        // TODO: Notify the user about the refund
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
