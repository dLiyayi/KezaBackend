package com.keza.payment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.payment.application.dto.MpesaCallbackRequest;
import com.keza.payment.application.usecase.PaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/callbacks")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentUseCase paymentUseCase;

    /**
     * Handles the M-Pesa STK push callback from Safaricom.
     * This endpoint is called by M-Pesa's servers after the user completes or cancels the STK push.
     */
    @PostMapping("/mpesa")
    public ResponseEntity<ApiResponse<String>> handleMpesaCallback(@RequestBody MpesaCallbackRequest callbackRequest) {
        log.info("Received M-Pesa STK callback: CheckoutRequestID={}", callbackRequest.getCheckoutRequestId());

        try {
            String checkoutRequestId = callbackRequest.getCheckoutRequestId();
            boolean success = callbackRequest.isSuccessful();

            Map<String, Object> metadata = new HashMap<>();
            if (callbackRequest.getBody() != null && callbackRequest.getBody().getStkCallback() != null) {
                var stkCallback = callbackRequest.getBody().getStkCallback();
                metadata.put("merchantRequestId", stkCallback.getMerchantRequestID());
                metadata.put("resultCode", stkCallback.getResultCode());
                metadata.put("resultDesc", stkCallback.getResultDesc());

                // Extract callback metadata items if present
                Object mpesaReceiptNumber = callbackRequest.getMetadataValue("MpesaReceiptNumber");
                if (mpesaReceiptNumber != null) {
                    metadata.put("mpesaReceiptNumber", mpesaReceiptNumber);
                }
                Object amount = callbackRequest.getMetadataValue("Amount");
                if (amount != null) {
                    metadata.put("amount", amount);
                }
                Object phoneNumber = callbackRequest.getMetadataValue("PhoneNumber");
                if (phoneNumber != null) {
                    metadata.put("phoneNumber", phoneNumber);
                }
                Object transactionDate = callbackRequest.getMetadataValue("TransactionDate");
                if (transactionDate != null) {
                    metadata.put("transactionDate", transactionDate);
                }
            }

            paymentUseCase.handlePaymentCallback(checkoutRequestId, success, metadata);

            // M-Pesa expects a simple acknowledgement response
            return ResponseEntity.ok(ApiResponse.success("Callback received"));

        } catch (Exception e) {
            log.error("Error processing M-Pesa callback", e);
            // Still return 200 to M-Pesa to prevent retries for processing errors
            return ResponseEntity.ok(ApiResponse.success("Callback received"));
        }
    }

    /**
     * Handles card payment callbacks (e.g., from Flutterwave webhook).
     */
    @PostMapping("/card")
    public ResponseEntity<ApiResponse<String>> handleCardCallback(@RequestBody Map<String, Object> payload) {
        log.info("Received card payment callback: {}", payload);

        try {
            String providerReference = extractStringValue(payload, "tx_ref",
                    extractStringValue(payload, "flw_ref", null));

            boolean success = "successful".equalsIgnoreCase(
                    extractStringValue(payload, "status", "failed"));

            Map<String, Object> metadata = new HashMap<>(payload);

            paymentUseCase.handlePaymentCallback(providerReference, success, metadata);

            return ResponseEntity.ok(ApiResponse.success("Callback received"));

        } catch (Exception e) {
            log.error("Error processing card payment callback", e);
            return ResponseEntity.ok(ApiResponse.success("Callback received"));
        }
    }

    private String extractStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
