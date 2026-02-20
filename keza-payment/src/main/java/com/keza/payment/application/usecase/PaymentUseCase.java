package com.keza.payment.application.usecase;

import com.keza.common.enums.PaymentMethod;
import com.keza.common.exception.BusinessRuleException;
import com.keza.payment.domain.model.PaymentInitiationResult;
import com.keza.payment.domain.model.PaymentStatusResult;
import com.keza.payment.domain.model.RefundResult;
import com.keza.payment.domain.port.out.PaymentGateway;
import com.keza.payment.domain.service.PaymentRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.keza.infrastructure.config.RabbitMQConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentUseCase {

    private static final String IDEMPOTENCY_KEY_PREFIX = "keza:payment:callback:";
    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    private final PaymentRouter paymentRouter;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    /**
     * Initiates a payment through the appropriate gateway based on the payment method.
     *
     * @param transactionId the unique transaction identifier
     * @param method        the chosen payment method (MPESA, CARD, BANK_TRANSFER, KCB_ESCROW)
     * @param metadata      additional metadata (e.g., phoneNumber for M-Pesa)
     * @return the payment initiation result from the gateway
     */
    public PaymentInitiationResult initiatePayment(UUID transactionId, PaymentMethod method, Map<String, String> metadata) {
        log.info("Initiating payment for transaction: {}, method: {}", transactionId, method);

        if (transactionId == null) {
            throw new BusinessRuleException("INVALID_TRANSACTION", "Transaction ID must not be null");
        }
        if (method == null) {
            throw new BusinessRuleException("INVALID_PAYMENT_METHOD", "Payment method must not be null");
        }

        PaymentGateway gateway = paymentRouter.route(method);

        // Default currency for East Africa
        String currency = metadata.getOrDefault("currency", "KES");
        BigDecimal amount = parseAmount(metadata);

        PaymentInitiationResult result = gateway.initiatePayment(transactionId, amount, currency, metadata);

        if (result.success()) {
            log.info("Payment initiated successfully for transaction: {}. Provider reference: {}",
                    transactionId, result.providerReference());
        } else {
            log.warn("Payment initiation failed for transaction: {}. Message: {}", transactionId, result.message());
        }

        return result;
    }

    /**
     * Handles a payment callback in an idempotent manner.
     * Uses Redis to ensure each callback is processed only once.
     * Publishes a payment event to RabbitMQ upon successful processing.
     *
     * @param providerReference the payment provider's reference
     * @param success           whether the payment was successful
     * @param metadata          additional callback metadata
     */
    public void handlePaymentCallback(String providerReference, boolean success, Map<String, Object> metadata) {
        log.info("Handling payment callback for providerReference: {}, success: {}", providerReference, success);

        if (providerReference == null || providerReference.isBlank()) {
            log.warn("Received callback with empty provider reference. Ignoring.");
            return;
        }

        // Idempotency check: ensure this callback is only processed once
        String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + providerReference;
        Boolean alreadyProcessed = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, success ? "SUCCESS" : "FAILED", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(alreadyProcessed)) {
            log.info("Callback for providerReference: {} already processed. Skipping (idempotent).", providerReference);
            return;
        }

        // Build the event payload
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("providerReference", providerReference);
        eventPayload.put("success", success);
        eventPayload.put("status", success ? "COMPLETED" : "FAILED");
        eventPayload.put("timestamp", System.currentTimeMillis());
        if (metadata != null) {
            eventPayload.put("metadata", metadata);
        }

        // Publish to the payment exchange for downstream consumers
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, PAYMENT_ROUTING_KEY, eventPayload);

        log.info("Payment callback event published for providerReference: {}, success: {}", providerReference, success);
    }

    /**
     * Processes a refund for the given transaction.
     *
     * @param transactionId     the transaction to refund
     * @param providerReference the original payment provider reference
     * @param method            the payment method used for the original transaction
     * @param amount            the amount to refund
     * @return the refund result from the gateway
     */
    public RefundResult processRefund(UUID transactionId, String providerReference, PaymentMethod method, BigDecimal amount) {
        log.info("Processing refund for transaction: {}, providerReference: {}, amount: {}", transactionId, providerReference, amount);

        if (transactionId == null) {
            throw new BusinessRuleException("INVALID_TRANSACTION", "Transaction ID must not be null for refund");
        }
        if (providerReference == null || providerReference.isBlank()) {
            throw new BusinessRuleException("INVALID_REFERENCE", "Provider reference is required for refund");
        }

        PaymentGateway gateway = paymentRouter.route(method);
        RefundResult result = gateway.refund(providerReference, amount);

        if (result.success()) {
            log.info("Refund initiated for transaction: {}. Refund reference: {}", transactionId, result.refundReference());

            // Publish refund event
            Map<String, Object> refundEvent = new HashMap<>();
            refundEvent.put("transactionId", transactionId.toString());
            refundEvent.put("providerReference", providerReference);
            refundEvent.put("refundReference", result.refundReference());
            refundEvent.put("amount", amount);
            refundEvent.put("status", "REFUNDED");
            refundEvent.put("timestamp", System.currentTimeMillis());

            rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, PAYMENT_ROUTING_KEY, refundEvent);
        } else {
            log.warn("Refund failed for transaction: {}. Message: {}", transactionId, result.message());
        }

        return result;
    }

    /**
     * Checks the payment status via the appropriate gateway.
     *
     * @param providerReference the provider reference to check
     * @param method            the payment method to determine the gateway
     * @return the payment status result
     */
    public PaymentStatusResult checkPaymentStatus(String providerReference, PaymentMethod method) {
        log.info("Checking payment status for providerReference: {}, method: {}", providerReference, method);

        PaymentGateway gateway = paymentRouter.route(method);
        return gateway.checkStatus(providerReference);
    }

    private BigDecimal parseAmount(Map<String, String> metadata) {
        String amountStr = metadata.get("amount");
        if (amountStr == null || amountStr.isBlank()) {
            throw new BusinessRuleException("INVALID_AMOUNT", "Payment amount is required in metadata");
        }
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("INVALID_AMOUNT", "Payment amount must be greater than zero");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("INVALID_AMOUNT", "Invalid payment amount: " + amountStr);
        }
    }
}
