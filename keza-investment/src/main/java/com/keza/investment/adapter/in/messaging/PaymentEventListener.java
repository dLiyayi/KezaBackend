package com.keza.investment.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.common.enums.TransactionStatus;
import com.keza.investment.application.usecase.InvestmentUseCase;
import com.keza.investment.domain.model.Transaction;
import com.keza.investment.domain.port.out.TransactionRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

import static com.keza.infrastructure.config.RabbitMQConfig.INVESTMENT_PAYMENT_QUEUE;

/**
 * Listens for payment domain events on the investment-payment queue.
 * <p>
 * Handles PAYMENT_COMPLETED, PAYMENT_FAILED, and PAYMENT_REFUNDED events
 * to update Transaction and Investment statuses accordingly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final TransactionRepository transactionRepository;
    private final InvestmentUseCase investmentUseCase;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = INVESTMENT_PAYMENT_QUEUE)
    public void handlePaymentEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            JsonNode event = objectMapper.readTree(message.getBody());

            String eventType = event.path("eventType").asText();
            String providerReference = event.path("providerReference").asText();

            log.info("Received payment event: eventType={}, providerReference={}", eventType, providerReference);

            switch (eventType) {
                case "PAYMENT_COMPLETED" -> handlePaymentCompleted(event);
                case "PAYMENT_FAILED" -> handlePaymentFailed(event);
                case "PAYMENT_REFUNDED" -> handlePaymentRefunded(event);
                default -> log.warn("Unknown payment event type: {}", eventType);
            }

            channel.basicAck(deliveryTag, false);
            log.debug("Payment event acknowledged: eventType={}, deliveryTag={}", eventType, deliveryTag);

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @Transactional
    protected void handlePaymentCompleted(JsonNode event) {
        String providerReference = event.path("providerReference").asText();
        String investmentIdStr = event.path("investmentId").asText(null);

        log.info("Handling PAYMENT_COMPLETED for providerReference: {}", providerReference);

        // Update transaction status to COMPLETED
        updateTransactionStatus(providerReference, TransactionStatus.COMPLETED);

        // Complete the investment
        if (investmentIdStr != null && !investmentIdStr.isEmpty() && !"null".equals(investmentIdStr)) {
            try {
                UUID investmentId = UUID.fromString(investmentIdStr);
                investmentUseCase.completeInvestment(investmentId);
                log.info("Investment {} completed via payment callback", investmentId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid investmentId in payment event: {}", investmentIdStr);
            } catch (Exception e) {
                log.error("Failed to complete investment {}: {}", investmentIdStr, e.getMessage(), e);
            }
        } else {
            log.warn("No investmentId in PAYMENT_COMPLETED event for providerReference: {}", providerReference);
        }
    }

    @Transactional
    protected void handlePaymentFailed(JsonNode event) {
        String providerReference = event.path("providerReference").asText();
        String investmentIdStr = event.path("investmentId").asText(null);
        String failureReason = event.path("failureReason").asText("Payment failed");

        log.info("Handling PAYMENT_FAILED for providerReference: {}", providerReference);

        // Update transaction status to FAILED
        updateTransactionStatus(providerReference, TransactionStatus.FAILED);

        // Cancel the investment
        if (investmentIdStr != null && !investmentIdStr.isEmpty() && !"null".equals(investmentIdStr)) {
            try {
                UUID investmentId = UUID.fromString(investmentIdStr);
                // Use the internal cancel - we look up the investment to get the userId
                var investmentResponse = investmentUseCase.getInvestment(investmentId);
                investmentUseCase.cancelInvestment(investmentId, investmentResponse.getInvestorId());
                log.info("Investment {} cancelled due to payment failure: {}", investmentId, failureReason);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid investmentId in payment event: {}", investmentIdStr);
            } catch (Exception e) {
                log.error("Failed to cancel investment {}: {}", investmentIdStr, e.getMessage(), e);
            }
        } else {
            log.warn("No investmentId in PAYMENT_FAILED event for providerReference: {}", providerReference);
        }
    }

    @Transactional
    protected void handlePaymentRefunded(JsonNode event) {
        String providerReference = event.path("providerReference").asText();
        String investmentIdStr = event.path("investmentId").asText(null);
        String refundReference = event.path("refundReference").asText(null);

        log.info("Handling PAYMENT_REFUNDED for providerReference: {}, refundReference: {}",
                providerReference, refundReference);

        // Update transaction status to REFUNDED
        updateTransactionStatus(providerReference, TransactionStatus.REFUNDED);

        // Refund the investment
        if (investmentIdStr != null && !investmentIdStr.isEmpty() && !"null".equals(investmentIdStr)) {
            try {
                UUID investmentId = UUID.fromString(investmentIdStr);
                investmentUseCase.refundInvestment(investmentId);
                log.info("Investment {} refunded via payment callback", investmentId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid investmentId in payment event: {}", investmentIdStr);
            } catch (Exception e) {
                log.error("Failed to refund investment {}: {}", investmentIdStr, e.getMessage(), e);
            }
        } else {
            log.warn("No investmentId in PAYMENT_REFUNDED event for providerReference: {}", providerReference);
        }
    }

    private void updateTransactionStatus(String providerReference, TransactionStatus status) {
        if (providerReference == null || providerReference.isEmpty()) {
            log.warn("Cannot update transaction status: providerReference is null or empty");
            return;
        }

        transactionRepository.findByProviderReference(providerReference)
                .ifPresentOrElse(
                        transaction -> {
                            transaction.setStatus(status);
                            transactionRepository.save(transaction);
                            log.info("Transaction {} status updated to {} for providerReference: {}",
                                    transaction.getId(), status, providerReference);
                        },
                        () -> log.warn("No transaction found for providerReference: {}", providerReference)
                );
    }
}
