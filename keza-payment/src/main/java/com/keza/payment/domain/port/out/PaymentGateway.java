package com.keza.payment.domain.port.out;

import com.keza.payment.domain.model.PaymentInitiationResult;
import com.keza.payment.domain.model.PaymentStatusResult;
import com.keza.payment.domain.model.RefundResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentGateway {

    String getName();

    PaymentInitiationResult initiatePayment(UUID transactionId, BigDecimal amount, String currency, Map<String, String> metadata);

    PaymentStatusResult checkStatus(String providerReference);

    RefundResult refund(String providerReference, BigDecimal amount);
}
