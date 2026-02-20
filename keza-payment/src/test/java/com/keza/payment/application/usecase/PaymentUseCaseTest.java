package com.keza.payment.application.usecase;

import com.keza.common.enums.PaymentMethod;
import com.keza.common.exception.BusinessRuleException;
import com.keza.payment.domain.model.PaymentInitiationResult;
import com.keza.payment.domain.model.PaymentStatusResult;
import com.keza.payment.domain.model.RefundResult;
import com.keza.payment.domain.port.out.PaymentGateway;
import com.keza.payment.domain.service.PaymentRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.keza.infrastructure.config.RabbitMQConfig.PAYMENT_EXCHANGE;
import static com.keza.infrastructure.config.RabbitMQConfig.PAYMENT_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentUseCase")
class PaymentUseCaseTest {

    @Mock
    private PaymentRouter paymentRouter;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentUseCase paymentUseCase;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventCaptor;

    private UUID transactionId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("initiatePayment")
    class InitiatePayment {

        @Test
        @DisplayName("should initiate payment successfully via routed gateway")
        void shouldInitiatePaymentSuccessfully() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("amount", "5000");
            metadata.put("phoneNumber", "254712345678");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);
            when(paymentGateway.initiatePayment(eq(transactionId), eq(new BigDecimal("5000")),
                    eq("KES"), eq(metadata)))
                    .thenReturn(new PaymentInitiationResult(true, "ws_CO_123", null, "STK push sent"));

            PaymentInitiationResult result = paymentUseCase.initiatePayment(transactionId, PaymentMethod.MPESA, metadata);

            assertThat(result.success()).isTrue();
            assertThat(result.providerReference()).isEqualTo("ws_CO_123");
            assertThat(result.message()).isEqualTo("STK push sent");

            verify(paymentRouter).route(PaymentMethod.MPESA);
            verify(paymentGateway).initiatePayment(eq(transactionId), any(), eq("KES"), eq(metadata));
        }

        @Test
        @DisplayName("should use default KES currency when none specified")
        void shouldUseDefaultCurrency() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("amount", "1000");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);
            when(paymentGateway.initiatePayment(any(), any(), eq("KES"), any()))
                    .thenReturn(new PaymentInitiationResult(true, "ref123", null, "OK"));

            paymentUseCase.initiatePayment(transactionId, PaymentMethod.MPESA, metadata);

            verify(paymentGateway).initiatePayment(eq(transactionId), eq(new BigDecimal("1000")), eq("KES"), eq(metadata));
        }

        @Test
        @DisplayName("should return failure result when gateway returns failure")
        void shouldReturnFailureFromGateway() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("amount", "5000");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);
            when(paymentGateway.initiatePayment(any(), any(), any(), any()))
                    .thenReturn(new PaymentInitiationResult(false, null, null, "Phone number invalid"));

            PaymentInitiationResult result = paymentUseCase.initiatePayment(transactionId, PaymentMethod.MPESA, metadata);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Phone number invalid");
        }

        @Test
        @DisplayName("should throw when transactionId is null")
        void shouldThrowWhenTransactionIdNull() {
            Map<String, String> metadata = Map.of("amount", "1000");

            assertThatThrownBy(() -> paymentUseCase.initiatePayment(null, PaymentMethod.MPESA, metadata))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Transaction ID must not be null");
        }

        @Test
        @DisplayName("should throw when payment method is null")
        void shouldThrowWhenPaymentMethodNull() {
            Map<String, String> metadata = Map.of("amount", "1000");

            assertThatThrownBy(() -> paymentUseCase.initiatePayment(transactionId, null, metadata))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Payment method must not be null");
        }

        @Test
        @DisplayName("should throw INVALID_AMOUNT when amount is missing from metadata")
        void shouldThrowWhenAmountMissing() {
            Map<String, String> metadata = new HashMap<>();

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);

            assertThatThrownBy(() -> paymentUseCase.initiatePayment(transactionId, PaymentMethod.MPESA, metadata))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Payment amount is required");
        }

        @Test
        @DisplayName("should throw INVALID_AMOUNT when amount is not a number")
        void shouldThrowWhenAmountNotNumeric() {
            Map<String, String> metadata = Map.of("amount", "not-a-number");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);

            assertThatThrownBy(() -> paymentUseCase.initiatePayment(transactionId, PaymentMethod.MPESA, metadata))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid payment amount");
        }

        @Test
        @DisplayName("should throw INVALID_AMOUNT when amount is zero")
        void shouldThrowWhenAmountZero() {
            Map<String, String> metadata = Map.of("amount", "0");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);

            assertThatThrownBy(() -> paymentUseCase.initiatePayment(transactionId, PaymentMethod.MPESA, metadata))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("should throw INVALID_AMOUNT when amount is negative")
        void shouldThrowWhenAmountNegative() {
            Map<String, String> metadata = Map.of("amount", "-500");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);

            assertThatThrownBy(() -> paymentUseCase.initiatePayment(transactionId, PaymentMethod.MPESA, metadata))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("greater than zero");
        }
    }

    @Nested
    @DisplayName("handlePaymentCallback")
    class HandlePaymentCallback {

        @Test
        @DisplayName("should process callback and publish event to RabbitMQ")
        void shouldProcessCallbackAndPublish() {
            String providerReference = "ws_CO_123";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

            Map<String, Object> metadata = Map.of("transactionId", "tx-123");

            paymentUseCase.handlePaymentCallback(providerReference, true, metadata);

            verify(valueOperations).setIfAbsent(
                    eq("keza:payment:callback:" + providerReference),
                    eq("SUCCESS"),
                    eq(24L),
                    eq(TimeUnit.HOURS));

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());

            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event).containsEntry("providerReference", providerReference);
            assertThat(event).containsEntry("success", true);
            assertThat(event).containsEntry("status", "COMPLETED");
            assertThat(event).containsKey("timestamp");
            assertThat(event).containsKey("metadata");
        }

        @Test
        @DisplayName("should set FAILED status when callback success is false")
        void shouldSetFailedStatus() {
            String providerReference = "ws_CO_456";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

            paymentUseCase.handlePaymentCallback(providerReference, false, null);

            verify(valueOperations).setIfAbsent(
                    anyString(), eq("FAILED"), anyLong(), any(TimeUnit.class));

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());
            assertThat(eventCaptor.getValue()).containsEntry("status", "FAILED");
        }

        @Test
        @DisplayName("should skip duplicate callback (idempotent)")
        void shouldSkipDuplicateCallback() {
            String providerReference = "ws_CO_duplicate";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            paymentUseCase.handlePaymentCallback(providerReference, true, null);

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("should ignore callback when provider reference is null")
        void shouldIgnoreNullProviderReference() {
            paymentUseCase.handlePaymentCallback(null, true, null);

            verifyNoInteractions(redisTemplate, rabbitTemplate);
        }

        @Test
        @DisplayName("should ignore callback when provider reference is blank")
        void shouldIgnoreBlankProviderReference() {
            paymentUseCase.handlePaymentCallback("  ", true, null);

            verifyNoInteractions(redisTemplate, rabbitTemplate);
        }

        @Test
        @DisplayName("should not include metadata key when metadata is null")
        void shouldExcludeNullMetadata() {
            String providerReference = "ws_CO_no_meta";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

            paymentUseCase.handlePaymentCallback(providerReference, true, null);

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());
            assertThat(eventCaptor.getValue()).doesNotContainKey("metadata");
        }
    }

    @Nested
    @DisplayName("processRefund")
    class ProcessRefund {

        @Test
        @DisplayName("should process refund successfully and publish event")
        void shouldProcessRefundSuccessfully() {
            String providerRef = "ws_CO_123";
            BigDecimal amount = new BigDecimal("5000");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);
            when(paymentGateway.refund(providerRef, amount))
                    .thenReturn(new RefundResult(true, "conv-id-456", "Reversal initiated"));

            RefundResult result = paymentUseCase.processRefund(transactionId, providerRef, PaymentMethod.MPESA, amount);

            assertThat(result.success()).isTrue();
            assertThat(result.refundReference()).isEqualTo("conv-id-456");

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event).containsEntry("transactionId", transactionId.toString());
            assertThat(event).containsEntry("status", "REFUNDED");
            assertThat(event).containsEntry("refundReference", "conv-id-456");
        }

        @Test
        @DisplayName("should not publish event when refund fails")
        void shouldNotPublishWhenRefundFails() {
            String providerRef = "ws_CO_123";
            BigDecimal amount = new BigDecimal("5000");

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);
            when(paymentGateway.refund(providerRef, amount))
                    .thenReturn(new RefundResult(false, null, "Reversal failed"));

            RefundResult result = paymentUseCase.processRefund(transactionId, providerRef, PaymentMethod.MPESA, amount);

            assertThat(result.success()).isFalse();
            verifyNoInteractions(rabbitTemplate);
        }

        @Test
        @DisplayName("should throw when transactionId is null")
        void shouldThrowWhenTransactionIdNull() {
            assertThatThrownBy(() -> paymentUseCase.processRefund(
                    null, "ref", PaymentMethod.MPESA, new BigDecimal("1000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Transaction ID must not be null");
        }

        @Test
        @DisplayName("should throw when providerReference is null")
        void shouldThrowWhenProviderReferenceNull() {
            assertThatThrownBy(() -> paymentUseCase.processRefund(
                    transactionId, null, PaymentMethod.MPESA, new BigDecimal("1000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Provider reference is required");
        }

        @Test
        @DisplayName("should throw when providerReference is blank")
        void shouldThrowWhenProviderReferenceBlank() {
            assertThatThrownBy(() -> paymentUseCase.processRefund(
                    transactionId, "  ", PaymentMethod.MPESA, new BigDecimal("1000")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Provider reference is required");
        }
    }

    @Nested
    @DisplayName("checkPaymentStatus")
    class CheckPaymentStatus {

        @Test
        @DisplayName("should delegate to routed gateway and return status")
        void shouldReturnPaymentStatus() {
            String providerRef = "ws_CO_123";
            PaymentStatusResult expected = new PaymentStatusResult(providerRef, "COMPLETED", "Success", Map.of());

            when(paymentRouter.route(PaymentMethod.MPESA)).thenReturn(paymentGateway);
            when(paymentGateway.checkStatus(providerRef)).thenReturn(expected);

            PaymentStatusResult result = paymentUseCase.checkPaymentStatus(providerRef, PaymentMethod.MPESA);

            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.providerReference()).isEqualTo(providerRef);
        }
    }
}
