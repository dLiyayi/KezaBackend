package com.keza.payment.adapter.out.external;

import com.keza.payment.domain.model.PaymentInitiationResult;
import com.keza.payment.domain.model.PaymentStatusResult;
import com.keza.payment.domain.model.RefundResult;
import com.keza.payment.domain.port.out.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Stub/placeholder gateway for Flutterwave card payment integration.
 * <p>
 * This implementation returns placeholder responses simulating a redirect-based
 * card payment flow. Replace with actual Flutterwave API calls in production.
 */
@Slf4j
@Component
public class CardPaymentGateway implements PaymentGateway {

    @Value("${keza.card.flutterwave-base-url:https://api.flutterwave.com/v3}")
    private String flutterwaveBaseUrl;

    @Value("${keza.card.callback-url:http://localhost:8080/api/v1/payments/callbacks/card}")
    private String callbackUrl;

    @Override
    public String getName() {
        return "card";
    }

    @Override
    public PaymentInitiationResult initiatePayment(UUID transactionId, BigDecimal amount, String currency, Map<String, String> metadata) {
        log.info("Initiating card payment (stub) for transaction: {}, amount: {} {}", transactionId, amount, currency);

        // In production, this would call Flutterwave's /payments endpoint
        // and return a hosted payment link for the user to complete card details
        String stubReference = "FLW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String redirectUrl = flutterwaveBaseUrl + "/hosted/pay/" + stubReference;

        log.info("Card payment stub generated. Reference: {}, Redirect: {}", stubReference, redirectUrl);

        return new PaymentInitiationResult(
                true,
                stubReference,
                redirectUrl,
                "Redirect user to the payment page to complete card payment"
        );
    }

    @Override
    public PaymentStatusResult checkStatus(String providerReference) {
        log.info("Checking card payment status (stub) for reference: {}", providerReference);

        // In production, this would call Flutterwave's /transactions/:id/verify endpoint
        return new PaymentStatusResult(
                providerReference,
                "PENDING",
                "Card payment status check is a stub. Integrate Flutterwave verify endpoint.",
                Map.of("provider", "flutterwave", "note", "stub implementation")
        );
    }

    @Override
    public RefundResult refund(String providerReference, BigDecimal amount) {
        log.info("Initiating card payment refund (stub) for reference: {}, amount: {}", providerReference, amount);

        // In production, this would call Flutterwave's /transactions/:id/refund endpoint
        String stubRefundReference = "FLW-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new RefundResult(
                true,
                stubRefundReference,
                "Card refund initiated (stub). Integrate Flutterwave refund endpoint for production."
        );
    }
}
