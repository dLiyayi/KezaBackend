package com.keza.investment.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.enums.TransactionStatus;
import com.keza.investment.application.dto.InvestmentResponse;
import com.keza.investment.application.usecase.InvestmentUseCase;
import com.keza.investment.domain.model.Transaction;
import com.keza.investment.domain.port.out.TransactionRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventListener")
class PaymentEventListenerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InvestmentUseCase investmentUseCase;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Channel channel;

    @InjectMocks
    private PaymentEventListener listener;

    private static final long DELIVERY_TAG = 1L;
    private static final String PROVIDER_REFERENCE = "ws_CO_123";
    private UUID investmentId;
    private UUID investorId;

    @BeforeEach
    void setUp() {
        investmentId = UUID.randomUUID();
        investorId = UUID.randomUUID();
    }

    private Message createMessage(String json) throws Exception {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(DELIVERY_TAG);
        byte[] body = json.getBytes();
        Message message = new Message(body, props);
        when(objectMapper.readTree(body)).thenReturn(new ObjectMapper().readTree(json));
        return message;
    }

    @Nested
    @DisplayName("PAYMENT_COMPLETED")
    class PaymentCompleted {

        @Test
        @DisplayName("should update transaction to COMPLETED and complete investment")
        void shouldUpdateTransactionAndCompleteInvestment() throws Exception {
            String json = """
                    {
                        "eventType": "PAYMENT_COMPLETED",
                        "providerReference": "%s",
                        "investmentId": "%s",
                        "transactionId": "tx-123"
                    }
                    """.formatted(PROVIDER_REFERENCE, investmentId);

            Message message = createMessage(json);

            Transaction transaction = Transaction.builder()
                    .investmentId(investmentId)
                    .userId(investorId)
                    .amount(new BigDecimal("5000"))
                    .status(TransactionStatus.PENDING)
                    .providerReference(PROVIDER_REFERENCE)
                    .build();
            transaction.setId(UUID.randomUUID());

            when(transactionRepository.findByProviderReference(PROVIDER_REFERENCE))
                    .thenReturn(Optional.of(transaction));
            when(investmentUseCase.completeInvestment(investmentId))
                    .thenReturn(InvestmentResponse.builder().id(investmentId).status("COMPLETED").build());

            listener.handlePaymentEvent(message, channel);

            verify(transactionRepository).findByProviderReference(PROVIDER_REFERENCE);
            verify(transactionRepository).save(transaction);
            verify(investmentUseCase).completeInvestment(investmentId);
            verify(channel).basicAck(DELIVERY_TAG, false);

            assert transaction.getStatus() == TransactionStatus.COMPLETED;
        }

        @Test
        @DisplayName("should ack even when no transaction found")
        void shouldAckWhenNoTransactionFound() throws Exception {
            String json = """
                    {
                        "eventType": "PAYMENT_COMPLETED",
                        "providerReference": "unknown-ref",
                        "investmentId": "%s"
                    }
                    """.formatted(investmentId);

            Message message = createMessage(json);

            when(transactionRepository.findByProviderReference("unknown-ref"))
                    .thenReturn(Optional.empty());
            when(investmentUseCase.completeInvestment(investmentId))
                    .thenReturn(InvestmentResponse.builder().id(investmentId).status("COMPLETED").build());

            listener.handlePaymentEvent(message, channel);

            verify(transactionRepository, never()).save(any());
            verify(investmentUseCase).completeInvestment(investmentId);
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }

    @Nested
    @DisplayName("PAYMENT_FAILED")
    class PaymentFailed {

        @Test
        @DisplayName("should update transaction to FAILED and cancel investment")
        void shouldUpdateTransactionAndCancelInvestment() throws Exception {
            String json = """
                    {
                        "eventType": "PAYMENT_FAILED",
                        "providerReference": "%s",
                        "investmentId": "%s",
                        "failureReason": "Insufficient funds"
                    }
                    """.formatted(PROVIDER_REFERENCE, investmentId);

            Message message = createMessage(json);

            Transaction transaction = Transaction.builder()
                    .investmentId(investmentId)
                    .userId(investorId)
                    .amount(new BigDecimal("5000"))
                    .status(TransactionStatus.PENDING)
                    .providerReference(PROVIDER_REFERENCE)
                    .build();
            transaction.setId(UUID.randomUUID());

            when(transactionRepository.findByProviderReference(PROVIDER_REFERENCE))
                    .thenReturn(Optional.of(transaction));

            InvestmentResponse investmentResponse = InvestmentResponse.builder()
                    .id(investmentId)
                    .investorId(investorId)
                    .status("PENDING")
                    .build();
            when(investmentUseCase.getInvestment(investmentId)).thenReturn(investmentResponse);
            when(investmentUseCase.cancelInvestment(investmentId, investorId))
                    .thenReturn(InvestmentResponse.builder().id(investmentId).status("CANCELLED").build());

            listener.handlePaymentEvent(message, channel);

            verify(transactionRepository).save(transaction);
            verify(investmentUseCase).getInvestment(investmentId);
            verify(investmentUseCase).cancelInvestment(investmentId, investorId);
            verify(channel).basicAck(DELIVERY_TAG, false);

            assert transaction.getStatus() == TransactionStatus.FAILED;
        }
    }

    @Nested
    @DisplayName("PAYMENT_REFUNDED")
    class PaymentRefunded {

        @Test
        @DisplayName("should update transaction to REFUNDED and refund investment")
        void shouldUpdateTransactionAndRefundInvestment() throws Exception {
            String json = """
                    {
                        "eventType": "PAYMENT_REFUNDED",
                        "providerReference": "%s",
                        "investmentId": "%s",
                        "refundReference": "ref-999"
                    }
                    """.formatted(PROVIDER_REFERENCE, investmentId);

            Message message = createMessage(json);

            Transaction transaction = Transaction.builder()
                    .investmentId(investmentId)
                    .userId(investorId)
                    .amount(new BigDecimal("5000"))
                    .status(TransactionStatus.COMPLETED)
                    .providerReference(PROVIDER_REFERENCE)
                    .build();
            transaction.setId(UUID.randomUUID());

            when(transactionRepository.findByProviderReference(PROVIDER_REFERENCE))
                    .thenReturn(Optional.of(transaction));
            when(investmentUseCase.refundInvestment(investmentId))
                    .thenReturn(InvestmentResponse.builder().id(investmentId).status("REFUNDED").build());

            listener.handlePaymentEvent(message, channel);

            verify(transactionRepository).save(transaction);
            verify(investmentUseCase).refundInvestment(investmentId);
            verify(channel).basicAck(DELIVERY_TAG, false);

            assert transaction.getStatus() == TransactionStatus.REFUNDED;
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should nack message on processing exception")
        void shouldNackOnException() throws Exception {
            MessageProperties props = new MessageProperties();
            props.setDeliveryTag(DELIVERY_TAG);
            byte[] body = "invalid json".getBytes();
            Message message = new Message(body, props);

            when(objectMapper.readTree(body)).thenThrow(new RuntimeException("Parse error"));

            listener.handlePaymentEvent(message, channel);

            verify(channel).basicNack(DELIVERY_TAG, false, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("should handle unknown event type gracefully and still ack")
        void shouldHandleUnknownEventType() throws Exception {
            String json = """
                    {
                        "eventType": "UNKNOWN_EVENT",
                        "providerReference": "ws_CO_123"
                    }
                    """;

            Message message = createMessage(json);

            listener.handlePaymentEvent(message, channel);

            verify(channel).basicAck(DELIVERY_TAG, false);
            verifyNoInteractions(investmentUseCase);
        }

        @Test
        @DisplayName("should handle missing investmentId in COMPLETED event gracefully")
        void shouldHandleMissingInvestmentId() throws Exception {
            String json = """
                    {
                        "eventType": "PAYMENT_COMPLETED",
                        "providerReference": "%s"
                    }
                    """.formatted(PROVIDER_REFERENCE);

            Message message = createMessage(json);

            when(transactionRepository.findByProviderReference(PROVIDER_REFERENCE))
                    .thenReturn(Optional.empty());

            listener.handlePaymentEvent(message, channel);

            verify(investmentUseCase, never()).completeInvestment(any());
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }
}
