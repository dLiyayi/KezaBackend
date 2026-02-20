package com.keza.notification.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.keza.notification.domain.model.NotificationChannel;
import com.keza.notification.domain.port.out.EmailSender;
import com.keza.notification.domain.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener")
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailSender emailSender;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationEventListener listener;

    @Mock
    private Channel channel;

    private static final long DELIVERY_TAG = 42L;

    private Message createMessage(ObjectNode event) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(event);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(DELIVERY_TAG);
        return new Message(body, props);
    }

    @Nested
    @DisplayName("USER_REGISTERED event")
    class UserRegisteredEvent {

        @Test
        @DisplayName("should send in-app and email notifications for user registration")
        void shouldSendNotificationsForRegistration() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "USER_REGISTERED");
            event.put("userId", userId.toString());
            event.put("email", "john@example.com");
            event.put("firstName", "John");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            // In-app notification
            verify(notificationService).sendNotification(
                    eq(userId), eq("USER_REGISTERED"),
                    eq("Welcome to Keza!"),
                    contains("Welcome to Keza, John"),
                    eq(NotificationChannel.IN_APP));

            // Email notification
            verify(notificationService).sendEmailNotification(
                    eq(userId), eq("USER_REGISTERED"),
                    eq("Welcome to Keza!"),
                    contains("Welcome to Keza, John"),
                    eq("john@example.com"),
                    anyString(),
                    isNull());

            verify(channel).basicAck(DELIVERY_TAG, false);
        }

        @Test
        @DisplayName("should skip email when email is empty")
        void shouldSkipEmailWhenEmpty() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "USER_REGISTERED");
            event.put("userId", userId.toString());
            event.put("email", "");
            event.put("firstName", "Jane");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("USER_REGISTERED"), anyString(), anyString(), eq(NotificationChannel.IN_APP));
            verify(notificationService, never()).sendEmailNotification(
                    any(), any(), any(), any(), any(), any(), any());
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }

    @Nested
    @DisplayName("KYC events")
    class KycEvents {

        @Test
        @DisplayName("should handle KYC_APPROVED event")
        void shouldHandleKycApproved() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "KYC_APPROVED");
            event.put("userId", userId.toString());
            event.put("email", "user@example.com");
            event.put("firstName", "Alice");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("KYC_APPROVED"),
                    eq("KYC Verification Approved"),
                    contains("approved"),
                    eq(NotificationChannel.IN_APP));
            verify(notificationService).sendEmailNotification(
                    eq(userId), eq("KYC_APPROVED"), anyString(), anyString(),
                    eq("user@example.com"), anyString(), isNull());
            verify(channel).basicAck(DELIVERY_TAG, false);
        }

        @Test
        @DisplayName("should handle KYC_REJECTED event")
        void shouldHandleKycRejected() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "KYC_REJECTED");
            event.put("userId", userId.toString());
            event.put("email", "user@example.com");
            event.put("firstName", "Bob");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("KYC_REJECTED"),
                    eq("KYC Verification Update"),
                    contains("requires attention"),
                    eq(NotificationChannel.IN_APP));
            verify(channel).basicAck(DELIVERY_TAG, false);
        }

        @Test
        @DisplayName("should handle KYC_PENDING_REVIEW event")
        void shouldHandleKycPendingReview() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "KYC_PENDING_REVIEW");
            event.put("userId", userId.toString());
            event.put("email", "");
            event.put("firstName", "Charlie");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("KYC_PENDING_REVIEW"),
                    eq("KYC Documents Under Review"),
                    contains("under review"),
                    eq(NotificationChannel.IN_APP));
            // No email since email is empty
            verify(notificationService, never()).sendEmailNotification(
                    any(), any(), any(), any(), any(), any(), any());
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }

    @Nested
    @DisplayName("INVESTMENT_CONFIRMED event")
    class InvestmentConfirmedEvent {

        @Test
        @DisplayName("should send in-app and email for investment confirmation")
        void shouldSendInvestmentConfirmation() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "INVESTMENT_CONFIRMED");
            event.put("userId", userId.toString());
            event.put("email", "investor@example.com");
            event.put("firstName", "Investor");
            event.put("campaignName", "Keza Solar");
            event.put("amount", "50000");
            event.put("currency", "KES");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("INVESTMENT_CONFIRMED"),
                    eq("Investment Confirmed"),
                    contains("KES 50000"),
                    eq(NotificationChannel.IN_APP),
                    anyString());
            verify(notificationService).sendEmailNotification(
                    eq(userId), eq("INVESTMENT_CONFIRMED"), anyString(), anyString(),
                    eq("investor@example.com"), anyString(), anyString());
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }

    @Nested
    @DisplayName("PAYMENT_RECEIVED event")
    class PaymentReceivedEvent {

        @Test
        @DisplayName("should send in-app notification for payment")
        void shouldSendPaymentNotification() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "PAYMENT_RECEIVED");
            event.put("userId", userId.toString());
            event.put("amount", "25000");
            event.put("currency", "KES");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("PAYMENT_RECEIVED"),
                    eq("Payment Received"),
                    contains("KES 25000"),
                    eq(NotificationChannel.IN_APP));
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }

    @Nested
    @DisplayName("CAMPAIGN_FUNDED event")
    class CampaignFundedEvent {

        @Test
        @DisplayName("should send in-app notification for campaign funded")
        void shouldSendCampaignFundedNotification() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "CAMPAIGN_FUNDED");
            event.put("userId", userId.toString());
            event.put("campaignName", "TechStartup");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("CAMPAIGN_FUNDED"),
                    eq("Campaign Fully Funded!"),
                    contains("TechStartup"),
                    eq(NotificationChannel.IN_APP));
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }

    @Nested
    @DisplayName("unknown event type")
    class UnknownEventType {

        @Test
        @DisplayName("should handle unknown event type via generic handler")
        void shouldHandleUnknownEventType() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "UNKNOWN_EVENT");
            event.put("userId", userId.toString());
            event.put("title", "Custom Notification");
            event.put("message", "Something happened");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService).sendNotification(
                    eq(userId), eq("UNKNOWN_EVENT"),
                    eq("Custom Notification"),
                    eq("Something happened"),
                    eq(NotificationChannel.IN_APP));
            verify(channel).basicAck(DELIVERY_TAG, false);
        }

        @Test
        @DisplayName("should skip generic handler when userId or message is empty")
        void shouldSkipGenericWhenMissingFields() throws Exception {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "UNKNOWN_EVENT");
            event.put("userId", "");
            event.put("message", "");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(notificationService, never()).sendNotification(
                    any(), any(), any(), any(), any());
            verify(channel).basicAck(DELIVERY_TAG, false);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should NACK message on processing failure")
        void shouldNackOnFailure() throws Exception {
            byte[] invalidBody = "not json".getBytes();
            MessageProperties props = new MessageProperties();
            props.setDeliveryTag(DELIVERY_TAG);
            Message message = new Message(invalidBody, props);

            listener.handleNotificationEvent(message, channel);

            verify(channel).basicNack(DELIVERY_TAG, false, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("should NACK and not requeue on exception")
        void shouldNackWithoutRequeue() throws Exception {
            UUID userId = UUID.randomUUID();
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "USER_REGISTERED");
            event.put("userId", "not-a-valid-uuid");
            event.put("email", "test@test.com");
            event.put("firstName", "Test");

            Message message = createMessage(event);

            listener.handleNotificationEvent(message, channel);

            verify(channel).basicNack(DELIVERY_TAG, false, false);
        }
    }
}
