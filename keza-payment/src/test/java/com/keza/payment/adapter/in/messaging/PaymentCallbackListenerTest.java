package com.keza.payment.adapter.in.messaging;

import com.rabbitmq.client.Channel;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.keza.infrastructure.config.RabbitMQConfig.PAYMENT_EXCHANGE;
import static com.keza.infrastructure.config.RabbitMQConfig.PAYMENT_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCallbackListener")
class PaymentCallbackListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    @InjectMocks
    private PaymentCallbackListener listener;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventCaptor;

    private static final long DELIVERY_TAG = 1L;

    @Nested
    @DisplayName("handlePaymentCallbackEvent")
    class HandlePaymentCallbackEvent {

        @Test
        @DisplayName("should publish PAYMENT_COMPLETED domain event and ack message")
        void shouldPublishCompletedEventAndAck() throws IOException {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("transactionId", "tx-123");
            metadata.put("investmentId", "inv-456");
            metadata.put("amount", "5000");
            metadata.put("userId", "user-789");
            metadata.put("campaignName", "Test Campaign");

            Map<String, Object> event = new HashMap<>();
            event.put("providerReference", "ws_CO_123");
            event.put("status", "COMPLETED");
            event.put("metadata", metadata);

            listener.handlePaymentCallbackEvent(event, DELIVERY_TAG, channel);

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());
            Map<String, Object> published = eventCaptor.getValue();

            assertThat(published).containsEntry("eventType", "PAYMENT_COMPLETED");
            assertThat(published).containsEntry("providerReference", "ws_CO_123");
            assertThat(published).containsEntry("transactionId", "tx-123");
            assertThat(published).containsEntry("investmentId", "inv-456");
            assertThat(published).containsEntry("amount", "5000");
            assertThat(published).containsEntry("userId", "user-789");
            assertThat(published).containsKey("timestamp");

            verify(channel).basicAck(DELIVERY_TAG, false);
            verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
        }

        @Test
        @DisplayName("should publish PAYMENT_FAILED domain event and ack message")
        void shouldPublishFailedEventAndAck() throws IOException {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("transactionId", "tx-123");
            metadata.put("investmentId", "inv-456");
            metadata.put("userId", "user-789");

            Map<String, Object> event = new HashMap<>();
            event.put("providerReference", "ws_CO_456");
            event.put("status", "FAILED");
            event.put("failureReason", "Insufficient funds");
            event.put("metadata", metadata);

            listener.handlePaymentCallbackEvent(event, DELIVERY_TAG, channel);

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());
            Map<String, Object> published = eventCaptor.getValue();

            assertThat(published).containsEntry("eventType", "PAYMENT_FAILED");
            assertThat(published).containsEntry("providerReference", "ws_CO_456");
            assertThat(published).containsEntry("failureReason", "Insufficient funds");
            assertThat(published).containsEntry("transactionId", "tx-123");

            verify(channel).basicAck(DELIVERY_TAG, false);
        }

        @Test
        @DisplayName("should publish PAYMENT_REFUNDED domain event and ack message")
        void shouldPublishRefundedEventAndAck() throws IOException {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("investmentId", "inv-456");
            metadata.put("userId", "user-789");

            Map<String, Object> event = new HashMap<>();
            event.put("providerReference", "ws_CO_789");
            event.put("status", "REFUNDED");
            event.put("transactionId", "tx-123");
            event.put("refundReference", "ref-999");
            event.put("amount", "5000");
            event.put("metadata", metadata);

            listener.handlePaymentCallbackEvent(event, DELIVERY_TAG, channel);

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());
            Map<String, Object> published = eventCaptor.getValue();

            assertThat(published).containsEntry("eventType", "PAYMENT_REFUNDED");
            assertThat(published).containsEntry("providerReference", "ws_CO_789");
            assertThat(published).containsEntry("transactionId", "tx-123");
            assertThat(published).containsEntry("refundReference", "ref-999");
            assertThat(published).containsEntry("amount", "5000");

            verify(channel).basicAck(DELIVERY_TAG, false);
        }

        @Test
        @DisplayName("should handle unknown status without publishing and still ack")
        void shouldHandleUnknownStatusAndAck() throws IOException {
            Map<String, Object> event = new HashMap<>();
            event.put("providerReference", "ws_CO_unknown");
            event.put("status", "UNKNOWN_STATUS");

            listener.handlePaymentCallbackEvent(event, DELIVERY_TAG, channel);

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
            verify(channel).basicAck(DELIVERY_TAG, false);
        }

        @Test
        @DisplayName("should nack message when processing throws exception")
        void shouldNackOnException() throws IOException {
            Map<String, Object> event = new HashMap<>();
            event.put("providerReference", "ws_CO_error");
            event.put("status", "COMPLETED");
            // No metadata - this is fine, but simulate exception via rabbitTemplate
            doThrow(new RuntimeException("RabbitMQ connection error"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            listener.handlePaymentCallbackEvent(event, DELIVERY_TAG, channel);

            verify(channel).basicNack(DELIVERY_TAG, false, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("should handle COMPLETED event without metadata gracefully")
        void shouldHandleCompletedWithoutMetadata() throws IOException {
            Map<String, Object> event = new HashMap<>();
            event.put("providerReference", "ws_CO_no_meta");
            event.put("status", "COMPLETED");

            listener.handlePaymentCallbackEvent(event, DELIVERY_TAG, channel);

            verify(rabbitTemplate).convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_ROUTING_KEY), eventCaptor.capture());
            Map<String, Object> published = eventCaptor.getValue();

            assertThat(published).containsEntry("eventType", "PAYMENT_COMPLETED");
            assertThat(published).containsEntry("providerReference", "ws_CO_no_meta");
            // Metadata fields should be null but event still published
            assertThat(published.get("transactionId")).isNull();

            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }
}
