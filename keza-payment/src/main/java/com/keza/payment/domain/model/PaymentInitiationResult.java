package com.keza.payment.domain.model;

import java.util.Map;

public record PaymentInitiationResult(
        boolean success,
        String providerReference,
        String redirectUrl,
        String message
) {}
