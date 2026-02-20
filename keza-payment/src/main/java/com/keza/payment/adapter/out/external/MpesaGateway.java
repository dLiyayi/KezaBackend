package com.keza.payment.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.payment.domain.model.PaymentInitiationResult;
import com.keza.payment.domain.model.PaymentStatusResult;
import com.keza.payment.domain.model.RefundResult;
import com.keza.payment.domain.port.out.PaymentGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MpesaGateway implements PaymentGateway {

    private static final String MPESA_OAUTH_TOKEN_CACHE_KEY = "keza:mpesa:oauth_token";
    private static final String OAUTH_PATH = "/oauth/v1/generate?grant_type=client_credentials";
    private static final String STK_PUSH_PATH = "/mpesa/stkpush/v1/processrequest";
    private static final String STK_QUERY_PATH = "/mpesa/stkpushquery/v1/query";
    private static final String REVERSAL_PATH = "/mpesa/reversal/v1/request";
    private static final DateTimeFormatter MPESA_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${keza.mpesa.consumer-key}")
    private String consumerKey;

    @Value("${keza.mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${keza.mpesa.passkey}")
    private String passkey;

    @Value("${keza.mpesa.shortcode}")
    private String shortcode;

    @Value("${keza.mpesa.callback-url}")
    private String callbackUrl;

    @Value("${keza.mpesa.base-url:https://sandbox.safaricom.co.ke}")
    private String baseUrl;

    public MpesaGateway(RestTemplate restTemplate, ObjectMapper objectMapper, StringRedisTemplate redisTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getName() {
        return "mpesa";
    }

    @Override
    @CircuitBreaker(name = "mpesa", fallbackMethod = "initiatePaymentFallback")
    @Retry(name = "mpesa")
    public PaymentInitiationResult initiatePayment(UUID transactionId, BigDecimal amount, String currency, Map<String, String> metadata) {
        log.info("Initiating M-Pesa STK push for transaction: {}, amount: {} {}", transactionId, amount, currency);

        String phoneNumber = metadata.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return new PaymentInitiationResult(false, null, null, "Phone number is required for M-Pesa payments");
        }

        String accessToken = getOAuthToken();
        String timestamp = LocalDateTime.now().format(MPESA_TIMESTAMP_FORMAT);
        String password = generatePassword(timestamp);

        Map<String, Object> stkPushRequest = new LinkedHashMap<>();
        stkPushRequest.put("BusinessShortCode", shortcode);
        stkPushRequest.put("Password", password);
        stkPushRequest.put("Timestamp", timestamp);
        stkPushRequest.put("TransactionType", "CustomerPayBillOnline");
        stkPushRequest.put("Amount", amount.intValue());
        stkPushRequest.put("PartyA", phoneNumber);
        stkPushRequest.put("PartyB", shortcode);
        stkPushRequest.put("PhoneNumber", phoneNumber);
        stkPushRequest.put("CallBackURL", callbackUrl + "/api/v1/payments/callbacks/mpesa");
        stkPushRequest.put("AccountReference", transactionId.toString().substring(0, 12));
        stkPushRequest.put("TransactionDesc", "Keza Investment - " + transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(stkPushRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + STK_PUSH_PATH,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body == null) {
                log.error("Empty response from M-Pesa STK push for transaction: {}", transactionId);
                return new PaymentInitiationResult(false, null, null, "Empty response from M-Pesa");
            }

            String responseCode = body.path("ResponseCode").asText("");
            if ("0".equals(responseCode)) {
                String checkoutRequestId = body.path("CheckoutRequestID").asText();
                String merchantRequestId = body.path("MerchantRequestID").asText();
                log.info("M-Pesa STK push initiated successfully. CheckoutRequestID: {}, MerchantRequestID: {}",
                        checkoutRequestId, merchantRequestId);

                return new PaymentInitiationResult(
                        true,
                        checkoutRequestId,
                        null,
                        body.path("CustomerMessage").asText("STK push sent successfully")
                );
            } else {
                String errorMessage = body.path("errorMessage").asText(
                        body.path("ResponseDescription").asText("STK push request failed")
                );
                log.warn("M-Pesa STK push failed for transaction: {}. Response: {}", transactionId, errorMessage);
                return new PaymentInitiationResult(false, null, null, errorMessage);
            }

        } catch (Exception e) {
            log.error("Failed to initiate M-Pesa STK push for transaction: {}", transactionId, e);
            throw new RuntimeException("M-Pesa STK push request failed: " + e.getMessage(), e);
        }
    }

    @Override
    @CircuitBreaker(name = "mpesa", fallbackMethod = "checkStatusFallback")
    @Retry(name = "mpesa")
    public PaymentStatusResult checkStatus(String providerReference) {
        log.info("Checking M-Pesa payment status for CheckoutRequestID: {}", providerReference);

        String accessToken = getOAuthToken();
        String timestamp = LocalDateTime.now().format(MPESA_TIMESTAMP_FORMAT);
        String password = generatePassword(timestamp);

        Map<String, Object> queryRequest = new LinkedHashMap<>();
        queryRequest.put("BusinessShortCode", shortcode);
        queryRequest.put("Password", password);
        queryRequest.put("Timestamp", timestamp);
        queryRequest.put("CheckoutRequestID", providerReference);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(queryRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + STK_QUERY_PATH,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body == null) {
                return new PaymentStatusResult(providerReference, "UNKNOWN", "Empty response from M-Pesa", Map.of());
            }

            String resultCode = body.path("ResultCode").asText("");
            String resultDesc = body.path("ResultDesc").asText("No description");

            String status = switch (resultCode) {
                case "0" -> "COMPLETED";
                case "1032" -> "CANCELLED";
                case "1037" -> "TIMEOUT";
                default -> "FAILED";
            };

            Map<String, Object> meta = new HashMap<>();
            meta.put("resultCode", resultCode);
            meta.put("resultDesc", resultDesc);
            meta.put("merchantRequestId", body.path("MerchantRequestID").asText());

            return new PaymentStatusResult(providerReference, status, resultDesc, meta);

        } catch (Exception e) {
            log.error("Failed to query M-Pesa payment status for: {}", providerReference, e);
            return new PaymentStatusResult(providerReference, "UNKNOWN", "Failed to query status: " + e.getMessage(), Map.of());
        }
    }

    @Override
    @CircuitBreaker(name = "mpesa", fallbackMethod = "refundFallback")
    @Retry(name = "mpesa")
    public RefundResult refund(String providerReference, BigDecimal amount) {
        log.info("Initiating M-Pesa reversal for providerReference: {}, amount: {}", providerReference, amount);

        String accessToken = getOAuthToken();

        Map<String, Object> reversalRequest = new LinkedHashMap<>();
        reversalRequest.put("Initiator", "apitest");
        reversalRequest.put("SecurityCredential", ""); // Must be encrypted with Safaricom cert in production
        reversalRequest.put("CommandID", "TransactionReversal");
        reversalRequest.put("TransactionID", providerReference);
        reversalRequest.put("Amount", amount.intValue());
        reversalRequest.put("ReceiverParty", shortcode);
        reversalRequest.put("RecieverIdentifierType", "11");
        reversalRequest.put("ResultURL", callbackUrl + "/api/v1/payments/callbacks/mpesa/reversal");
        reversalRequest.put("QueueTimeOutURL", callbackUrl + "/api/v1/payments/callbacks/mpesa/timeout");
        reversalRequest.put("Remarks", "Keza refund");
        reversalRequest.put("Occasion", "Refund");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reversalRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + REVERSAL_PATH,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body == null) {
                return new RefundResult(false, null, "Empty response from M-Pesa reversal");
            }

            String responseCode = body.path("ResponseCode").asText("");
            if ("0".equals(responseCode)) {
                String conversationId = body.path("ConversationID").asText();
                log.info("M-Pesa reversal initiated. ConversationID: {}", conversationId);
                return new RefundResult(true, conversationId, "Reversal initiated successfully");
            } else {
                String errorMessage = body.path("errorMessage").asText(
                        body.path("ResponseDescription").asText("Reversal request failed")
                );
                return new RefundResult(false, null, errorMessage);
            }

        } catch (Exception e) {
            log.error("Failed to initiate M-Pesa reversal for: {}", providerReference, e);
            return new RefundResult(false, null, "Reversal request failed: " + e.getMessage());
        }
    }

    // ---- OAuth Token Management ----

    private String getOAuthToken() {
        // Try cache first
        String cachedToken = redisTemplate.opsForValue().get(MPESA_OAUTH_TOKEN_CACHE_KEY);
        if (cachedToken != null && !cachedToken.isBlank()) {
            log.debug("Using cached M-Pesa OAuth token");
            return cachedToken;
        }

        log.info("Fetching new M-Pesa OAuth token");
        return generateOAuthToken();
    }

    private String generateOAuthToken() {
        String credentials = consumerKey + ":" + consumerSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedCredentials);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + OAUTH_PATH,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from M-Pesa OAuth endpoint");
            }

            String accessToken = body.path("access_token").asText();
            long expiresIn = body.path("expires_in").asLong(3599);

            if (accessToken.isBlank()) {
                throw new RuntimeException("No access_token in M-Pesa OAuth response");
            }

            // Cache the token in Redis with a TTL slightly shorter than its expiry
            long cacheTtlSeconds = Math.max(expiresIn - 60, 60);
            redisTemplate.opsForValue().set(MPESA_OAUTH_TOKEN_CACHE_KEY, accessToken, cacheTtlSeconds, TimeUnit.SECONDS);

            log.info("M-Pesa OAuth token obtained and cached for {} seconds", cacheTtlSeconds);
            return accessToken;

        } catch (Exception e) {
            log.error("Failed to generate M-Pesa OAuth token", e);
            throw new RuntimeException("Failed to obtain M-Pesa access token: " + e.getMessage(), e);
        }
    }

    private String generatePassword(String timestamp) {
        String rawPassword = shortcode + passkey + timestamp;
        return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
    }

    // ---- Circuit Breaker Fallbacks ----

    private PaymentInitiationResult initiatePaymentFallback(UUID transactionId, BigDecimal amount, String currency, Map<String, String> metadata, Throwable t) {
        log.warn("M-Pesa circuit breaker triggered for initiatePayment. Transaction: {}, Error: {}", transactionId, t.getMessage());
        return new PaymentInitiationResult(false, null, null,
                "M-Pesa service is temporarily unavailable. Please try again later.");
    }

    private PaymentStatusResult checkStatusFallback(String providerReference, Throwable t) {
        log.warn("M-Pesa circuit breaker triggered for checkStatus. Reference: {}, Error: {}", providerReference, t.getMessage());
        return new PaymentStatusResult(providerReference, "UNKNOWN",
                "M-Pesa service is temporarily unavailable. Status check will be retried.", Map.of());
    }

    private RefundResult refundFallback(String providerReference, BigDecimal amount, Throwable t) {
        log.warn("M-Pesa circuit breaker triggered for refund. Reference: {}, Error: {}", providerReference, t.getMessage());
        return new RefundResult(false, null,
                "M-Pesa service is temporarily unavailable. Refund will be retried.");
    }
}
