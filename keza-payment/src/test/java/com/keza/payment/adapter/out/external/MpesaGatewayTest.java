package com.keza.payment.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.keza.payment.domain.model.PaymentInitiationResult;
import com.keza.payment.domain.model.PaymentStatusResult;
import com.keza.payment.domain.model.RefundResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MpesaGateway")
class MpesaGatewayTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private MpesaGateway mpesaGateway;

    private static final String BASE_URL = "https://sandbox.safaricom.co.ke";
    private static final String SHORTCODE = "174379";
    private static final String PASSKEY = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";
    private static final String CALLBACK_URL = "https://api.keza.co.ke";
    private static final String CONSUMER_KEY = "test-consumer-key";
    private static final String CONSUMER_SECRET = "test-consumer-secret";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mpesaGateway = new MpesaGateway(restTemplate, objectMapper, redisTemplate);

        ReflectionTestUtils.setField(mpesaGateway, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(mpesaGateway, "shortcode", SHORTCODE);
        ReflectionTestUtils.setField(mpesaGateway, "passkey", PASSKEY);
        ReflectionTestUtils.setField(mpesaGateway, "callbackUrl", CALLBACK_URL);
        ReflectionTestUtils.setField(mpesaGateway, "consumerKey", CONSUMER_KEY);
        ReflectionTestUtils.setField(mpesaGateway, "consumerSecret", CONSUMER_SECRET);
    }

    @Test
    @DisplayName("getName should return 'mpesa'")
    void shouldReturnMpesaName() {
        assertThat(mpesaGateway.getName()).isEqualTo("mpesa");
    }

    private void stubCachedToken(String token) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("keza:mpesa:oauth_token")).thenReturn(token);
    }

    private void stubFreshToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("keza:mpesa:oauth_token")).thenReturn(null);

        ObjectNode tokenResponse = objectMapper.createObjectNode();
        tokenResponse.put("access_token", "fresh-token-123");
        tokenResponse.put("expires_in", 3599);

        when(restTemplate.exchange(
                eq(BASE_URL + "/oauth/v1/generate?grant_type=client_credentials"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));
    }

    @Nested
    @DisplayName("initiatePayment - STK Push")
    class InitiatePayment {

        @Test
        @DisplayName("should successfully initiate STK push with cached token")
        void shouldInitiateStkPushSuccessfully() {
            stubCachedToken("cached-token-abc");

            UUID transactionId = UUID.randomUUID();
            Map<String, String> metadata = new HashMap<>();
            metadata.put("phoneNumber", "254712345678");

            ObjectNode successResponse = objectMapper.createObjectNode();
            successResponse.put("ResponseCode", "0");
            successResponse.put("CheckoutRequestID", "ws_CO_123456");
            successResponse.put("MerchantRequestID", "mr_123");
            successResponse.put("CustomerMessage", "Success. Request accepted for processing");

            when(restTemplate.exchange(
                    eq(BASE_URL + "/mpesa/stkpush/v1/processrequest"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(successResponse));

            PaymentInitiationResult result = mpesaGateway.initiatePayment(
                    transactionId, new BigDecimal("5000"), "KES", metadata);

            assertThat(result.success()).isTrue();
            assertThat(result.providerReference()).isEqualTo("ws_CO_123456");
            assertThat(result.message()).contains("Success");
        }

        @Test
        @DisplayName("should return failure when phone number is missing")
        void shouldFailWhenPhoneNumberMissing() {
            Map<String, String> metadata = new HashMap<>();

            PaymentInitiationResult result = mpesaGateway.initiatePayment(
                    UUID.randomUUID(), new BigDecimal("5000"), "KES", metadata);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Phone number is required");
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("should return failure when phone number is blank")
        void shouldFailWhenPhoneNumberBlank() {
            Map<String, String> metadata = Map.of("phoneNumber", "  ");

            PaymentInitiationResult result = mpesaGateway.initiatePayment(
                    UUID.randomUUID(), new BigDecimal("5000"), "KES", metadata);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Phone number is required");
        }

        @Test
        @DisplayName("should return failure when M-Pesa returns non-zero response code")
        void shouldReturnFailureOnNonZeroResponseCode() {
            stubCachedToken("cached-token");

            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("ResponseCode", "1");
            errorResponse.put("ResponseDescription", "Insufficient funds");

            when(restTemplate.exchange(
                    contains("/stkpush"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(errorResponse));

            Map<String, String> metadata = Map.of("phoneNumber", "254712345678");

            PaymentInitiationResult result = mpesaGateway.initiatePayment(
                    UUID.randomUUID(), new BigDecimal("5000"), "KES", metadata);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Insufficient funds");
        }

        @Test
        @DisplayName("should return failure when M-Pesa response body is null")
        void shouldReturnFailureOnNullBody() {
            stubCachedToken("cached-token");

            when(restTemplate.exchange(
                    contains("/stkpush"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(null));

            Map<String, String> metadata = Map.of("phoneNumber", "254712345678");

            PaymentInitiationResult result = mpesaGateway.initiatePayment(
                    UUID.randomUUID(), new BigDecimal("5000"), "KES", metadata);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Empty response");
        }

        @Test
        @DisplayName("should throw RuntimeException when REST call fails")
        void shouldThrowOnRestException() {
            stubCachedToken("cached-token");

            when(restTemplate.exchange(
                    contains("/stkpush"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            Map<String, String> metadata = Map.of("phoneNumber", "254712345678");

            assertThatThrownBy(() -> mpesaGateway.initiatePayment(
                    UUID.randomUUID(), new BigDecimal("5000"), "KES", metadata))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("M-Pesa STK push request failed");
        }

        @Test
        @DisplayName("should fetch fresh token when cache is empty")
        void shouldFetchFreshTokenWhenCacheEmpty() {
            stubFreshToken();

            ObjectNode successResponse = objectMapper.createObjectNode();
            successResponse.put("ResponseCode", "0");
            successResponse.put("CheckoutRequestID", "ws_CO_fresh");
            successResponse.put("MerchantRequestID", "mr_fresh");
            successResponse.put("CustomerMessage", "OK");

            when(restTemplate.exchange(
                    contains("/stkpush"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(successResponse));

            Map<String, String> metadata = Map.of("phoneNumber", "254712345678");

            PaymentInitiationResult result = mpesaGateway.initiatePayment(
                    UUID.randomUUID(), new BigDecimal("1000"), "KES", metadata);

            assertThat(result.success()).isTrue();

            // Verify OAuth token was fetched and cached
            verify(restTemplate).exchange(
                    contains("/oauth/v1/generate"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(JsonNode.class));

            verify(valueOperations).set(
                    eq("keza:mpesa:oauth_token"),
                    eq("fresh-token-123"),
                    eq(3539L), // 3599 - 60
                    eq(TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("checkStatus")
    class CheckStatus {

        @Test
        @DisplayName("should return COMPLETED when result code is 0")
        void shouldReturnCompletedForCodeZero() {
            stubCachedToken("token");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("ResultCode", "0");
            body.put("ResultDesc", "The service request is processed successfully.");
            body.put("MerchantRequestID", "mr_123");

            when(restTemplate.exchange(
                    contains("/stkpushquery"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            PaymentStatusResult result = mpesaGateway.checkStatus("ws_CO_123");

            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.providerReference()).isEqualTo("ws_CO_123");
        }

        @Test
        @DisplayName("should return CANCELLED when result code is 1032")
        void shouldReturnCancelledForCode1032() {
            stubCachedToken("token");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("ResultCode", "1032");
            body.put("ResultDesc", "Request cancelled by user");
            body.put("MerchantRequestID", "mr_123");

            when(restTemplate.exchange(
                    contains("/stkpushquery"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            PaymentStatusResult result = mpesaGateway.checkStatus("ws_CO_123");

            assertThat(result.status()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should return TIMEOUT when result code is 1037")
        void shouldReturnTimeoutForCode1037() {
            stubCachedToken("token");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("ResultCode", "1037");
            body.put("ResultDesc", "DS timeout");
            body.put("MerchantRequestID", "mr_123");

            when(restTemplate.exchange(
                    contains("/stkpushquery"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            PaymentStatusResult result = mpesaGateway.checkStatus("ws_CO_123");

            assertThat(result.status()).isEqualTo("TIMEOUT");
        }

        @Test
        @DisplayName("should return FAILED for unknown result codes")
        void shouldReturnFailedForUnknownCode() {
            stubCachedToken("token");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("ResultCode", "9999");
            body.put("ResultDesc", "Unknown error");
            body.put("MerchantRequestID", "mr_123");

            when(restTemplate.exchange(
                    contains("/stkpushquery"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            PaymentStatusResult result = mpesaGateway.checkStatus("ws_CO_123");

            assertThat(result.status()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("should return UNKNOWN when response body is null")
        void shouldReturnUnknownOnNullBody() {
            stubCachedToken("token");

            when(restTemplate.exchange(
                    contains("/stkpushquery"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(null));

            PaymentStatusResult result = mpesaGateway.checkStatus("ws_CO_123");

            assertThat(result.status()).isEqualTo("UNKNOWN");
            assertThat(result.message()).contains("Empty response");
        }

        @Test
        @DisplayName("should return UNKNOWN when REST call throws exception")
        void shouldReturnUnknownOnException() {
            stubCachedToken("token");

            when(restTemplate.exchange(
                    contains("/stkpushquery"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenThrow(new RuntimeException("Network error"));

            PaymentStatusResult result = mpesaGateway.checkStatus("ws_CO_123");

            assertThat(result.status()).isEqualTo("UNKNOWN");
            assertThat(result.message()).contains("Failed to query status");
        }
    }

    @Nested
    @DisplayName("refund (reversal)")
    class Refund {

        @Test
        @DisplayName("should return success when reversal is initiated successfully")
        void shouldReturnSuccessOnReversal() {
            stubCachedToken("token");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("ResponseCode", "0");
            body.put("ConversationID", "AG_20230101_conv123");

            when(restTemplate.exchange(
                    contains("/reversal"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            RefundResult result = mpesaGateway.refund("tx_ABC123", new BigDecimal("3000"));

            assertThat(result.success()).isTrue();
            assertThat(result.refundReference()).isEqualTo("AG_20230101_conv123");
            assertThat(result.message()).contains("Reversal initiated successfully");
        }

        @Test
        @DisplayName("should return failure when M-Pesa reversal returns non-zero code")
        void shouldReturnFailureOnNonZeroCode() {
            stubCachedToken("token");

            ObjectNode body = objectMapper.createObjectNode();
            body.put("ResponseCode", "1");
            body.put("ResponseDescription", "Reversal not allowed");

            when(restTemplate.exchange(
                    contains("/reversal"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            RefundResult result = mpesaGateway.refund("tx_ABC123", new BigDecimal("3000"));

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Reversal not allowed");
        }

        @Test
        @DisplayName("should return failure when response body is null")
        void shouldReturnFailureOnNullBody() {
            stubCachedToken("token");

            when(restTemplate.exchange(
                    contains("/reversal"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(null));

            RefundResult result = mpesaGateway.refund("tx_ABC123", new BigDecimal("3000"));

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Empty response");
        }

        @Test
        @DisplayName("should return failure when REST call throws exception")
        void shouldReturnFailureOnException() {
            stubCachedToken("token");

            when(restTemplate.exchange(
                    contains("/reversal"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenThrow(new RuntimeException("Timeout"));

            RefundResult result = mpesaGateway.refund("tx_ABC123", new BigDecimal("3000"));

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Reversal request failed");
        }
    }

    @Nested
    @DisplayName("OAuth token caching")
    class OAuthTokenCaching {

        @Test
        @DisplayName("should use cached token when available")
        void shouldUseCachedToken() {
            stubCachedToken("cached-token-xyz");

            ObjectNode successResponse = objectMapper.createObjectNode();
            successResponse.put("ResponseCode", "0");
            successResponse.put("CheckoutRequestID", "ws_CO_cached");
            successResponse.put("MerchantRequestID", "mr_cached");
            successResponse.put("CustomerMessage", "OK");

            when(restTemplate.exchange(
                    contains("/stkpush"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(successResponse));

            Map<String, String> metadata = Map.of("phoneNumber", "254712345678");
            mpesaGateway.initiatePayment(UUID.randomUUID(), new BigDecimal("1000"), "KES", metadata);

            // Should NOT call the OAuth endpoint
            verify(restTemplate, never()).exchange(
                    contains("/oauth/v1/generate"),
                    any(HttpMethod.class),
                    any(HttpEntity.class),
                    eq(JsonNode.class));

            // Verify the bearer token was used in STK push call
            ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    contains("/stkpush"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(JsonNode.class));
            assertThat(entityCaptor.getValue().getHeaders().getFirst("Authorization"))
                    .isEqualTo("Bearer cached-token-xyz");
        }

        @Test
        @DisplayName("should fetch and cache new token when cache is empty")
        void shouldFetchNewTokenWhenCacheEmpty() {
            stubFreshToken();

            ObjectNode successResponse = objectMapper.createObjectNode();
            successResponse.put("ResponseCode", "0");
            successResponse.put("CheckoutRequestID", "ws_CO_new");
            successResponse.put("MerchantRequestID", "mr_new");
            successResponse.put("CustomerMessage", "OK");

            when(restTemplate.exchange(
                    contains("/stkpush"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(successResponse));

            Map<String, String> metadata = Map.of("phoneNumber", "254712345678");
            mpesaGateway.initiatePayment(UUID.randomUUID(), new BigDecimal("1000"), "KES", metadata);

            // Verify OAuth endpoint was called
            verify(restTemplate).exchange(
                    contains("/oauth/v1/generate"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(JsonNode.class));

            // Verify token was cached with TTL (3599 - 60 = 3539 seconds)
            verify(valueOperations).set(
                    eq("keza:mpesa:oauth_token"),
                    eq("fresh-token-123"),
                    eq(3539L),
                    eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("should throw RuntimeException when OAuth endpoint returns empty body")
        void shouldThrowWhenOAuthReturnsEmptyBody() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("keza:mpesa:oauth_token")).thenReturn(null);

            when(restTemplate.exchange(
                    contains("/oauth/v1/generate"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(null));

            Map<String, String> metadata = Map.of("phoneNumber", "254712345678");

            assertThatThrownBy(() -> mpesaGateway.initiatePayment(
                    UUID.randomUUID(), new BigDecimal("1000"), "KES", metadata))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to obtain M-Pesa access token");
        }
    }
}
