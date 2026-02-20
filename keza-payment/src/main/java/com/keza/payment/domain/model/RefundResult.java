package com.keza.payment.domain.model;

public record RefundResult(
        boolean success,
        String refundReference,
        String message
) {}
