package com.keza.payment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.common.enums.PaymentMethod;
import com.keza.payment.application.dto.PaymentInitiationRequest;
import com.keza.payment.application.dto.PaymentResponse;
import com.keza.payment.application.usecase.PaymentUseCase;
import com.keza.payment.domain.model.PaymentInitiationResult;
import com.keza.payment.domain.model.PaymentStatusResult;
import com.keza.payment.domain.model.RefundResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    /**
     * Initiates a payment for a given transaction using the specified payment method.
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentInitiationRequest request) {

        log.info("Payment initiation request: transactionId={}, method={}",
                request.getTransactionId(), request.getPaymentMethod());

        PaymentInitiationResult result = paymentUseCase.initiatePayment(
                request.getTransactionId(),
                request.getPaymentMethod(),
                request.getMetadata()
        );

        PaymentResponse response = PaymentResponse.builder()
                .success(result.success())
                .providerReference(result.providerReference())
                .redirectUrl(result.redirectUrl())
                .message(result.message())
                .build();

        if (result.success()) {
            return ResponseEntity.ok(ApiResponse.success(response, "Payment initiated successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.message()));
        }
    }

    /**
     * Checks the status of a payment by provider reference.
     */
    @GetMapping("/status/{providerReference}")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkPaymentStatus(
            @PathVariable String providerReference,
            @RequestParam PaymentMethod paymentMethod) {

        log.info("Payment status check: providerReference={}, method={}", providerReference, paymentMethod);

        PaymentStatusResult result = paymentUseCase.checkPaymentStatus(providerReference, paymentMethod);

        PaymentResponse response = PaymentResponse.builder()
                .success(true)
                .providerReference(result.providerReference())
                .status(result.status())
                .message(result.message())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Initiates a refund for a given transaction. Restricted to ADMIN role.
     */
    @PostMapping("/refund/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiateRefund(
            @PathVariable UUID transactionId,
            @RequestParam String providerReference,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam BigDecimal amount) {

        log.info("Refund request: transactionId={}, providerReference={}, method={}, amount={}",
                transactionId, providerReference, paymentMethod, amount);

        RefundResult result = paymentUseCase.processRefund(transactionId, providerReference, paymentMethod, amount);

        PaymentResponse response = PaymentResponse.builder()
                .success(result.success())
                .providerReference(result.refundReference())
                .message(result.message())
                .status(result.success() ? "REFUNDED" : "REFUND_FAILED")
                .build();

        if (result.success()) {
            return ResponseEntity.ok(ApiResponse.success(response, "Refund initiated successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.message()));
        }
    }
}
