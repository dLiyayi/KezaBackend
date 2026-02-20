package com.keza.payment.domain.model;

import java.util.Map;

public record PaymentStatusResult(
        String providerReference,
        String status,
        String message,
        Map<String, Object> metadata
) {}
