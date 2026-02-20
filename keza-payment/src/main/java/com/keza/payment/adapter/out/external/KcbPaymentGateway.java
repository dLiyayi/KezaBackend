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
 * Stub gateway for KCB bank transfer integration.
 * <p>
 * This implementation returns manual bank transfer instructions.
 * Replace with actual KCB API integration when available.
 */
@Slf4j
@Component
public class KcbPaymentGateway implements PaymentGateway {

    @Value("${keza.kcb.account-name:Keza Platform Limited}")
    private String accountName;

    @Value("${keza.kcb.account-number:1234567890}")
    private String accountNumber;

    @Value("${keza.kcb.branch:Nairobi Main}")
    private String branch;

    @Value("${keza.kcb.bank-code:01}")
    private String bankCode;

    @Override
    public String getName() {
        return "kcb";
    }

    @Override
    public PaymentInitiationResult initiatePayment(UUID transactionId, BigDecimal amount, String currency, Map<String, String> metadata) {
        log.info("Initiating KCB bank transfer (stub) for transaction: {}, amount: {} {}", transactionId, amount, currency);

        // Generate a unique reference for this bank transfer
        String transferReference = "KCB-" + transactionId.toString().substring(0, 8).toUpperCase();

        String instructions = String.format(
                "Please transfer %s %s to: Bank: KCB | Account Name: %s | Account Number: %s | Branch: %s | Reference: %s",
                currency, amount.toPlainString(), accountName, accountNumber, branch, transferReference
        );

        log.info("KCB bank transfer instructions generated for transaction: {}. Reference: {}", transactionId, transferReference);

        return new PaymentInitiationResult(
                true,
                transferReference,
                null,
                instructions
        );
    }

    @Override
    public PaymentStatusResult checkStatus(String providerReference) {
        log.info("Checking KCB bank transfer status (stub) for reference: {}", providerReference);

        // In production, this would integrate with KCB's API or a reconciliation service
        // to verify whether the bank transfer has been received
        return new PaymentStatusResult(
                providerReference,
                "PENDING",
                "Bank transfer verification is manual. An admin will confirm receipt.",
                Map.of(
                        "provider", "kcb",
                        "accountNumber", accountNumber,
                        "note", "Awaiting manual confirmation"
                )
        );
    }

    @Override
    public RefundResult refund(String providerReference, BigDecimal amount) {
        log.info("Initiating KCB bank transfer refund (stub) for reference: {}, amount: {}", providerReference, amount);

        // Bank transfer refunds are typically handled manually or via a separate transfer
        String refundReference = "KCB-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new RefundResult(
                true,
                refundReference,
                "Bank transfer refund request recorded. Refund will be processed manually by the finance team."
        );
    }
}
