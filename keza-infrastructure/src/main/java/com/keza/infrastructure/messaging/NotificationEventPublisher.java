package com.keza.infrastructure.messaging;

import com.keza.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Async
    public void publishUserRegistered(UUID userId, String email, String firstName) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "USER_REGISTERED");
        event.put("userId", userId.toString());
        event.put("email", email);
        event.put("firstName", firstName);
        publish(event);
    }

    @Async
    public void publishInvestmentConfirmed(UUID userId, String email, String firstName,
                                            String campaignName, BigDecimal amount, String currency) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "INVESTMENT_CONFIRMED");
        event.put("userId", userId.toString());
        event.put("email", email);
        event.put("firstName", firstName);
        event.put("campaignName", campaignName);
        event.put("amount", amount.toPlainString());
        event.put("currency", currency);
        publish(event);
    }

    @Async
    public void publishInvestmentCancelled(UUID userId, String campaignName, BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "INVESTMENT_CANCELLED");
        event.put("userId", userId.toString());
        event.put("title", "Investment Cancelled");
        event.put("message", "Your investment of KES " + amount.toPlainString() + " in " + campaignName + " has been cancelled.");
        publish(event);
    }

    @Async
    public void publishCampaignFunded(UUID issuerId, String campaignName) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "CAMPAIGN_FUNDED");
        event.put("userId", issuerId.toString());
        event.put("campaignName", campaignName);
        publish(event);
    }

    @Async
    public void publishCampaignApproved(UUID issuerId, String campaignName) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "CAMPAIGN_APPROVED");
        event.put("userId", issuerId.toString());
        event.put("title", "Campaign Approved");
        event.put("message", "Your campaign '" + campaignName + "' has been approved and is now live!");
        publish(event);
    }

    @Async
    public void publishCampaignRejected(UUID issuerId, String campaignName, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "CAMPAIGN_REJECTED");
        event.put("userId", issuerId.toString());
        event.put("title", "Campaign Update Required");
        event.put("message", "Your campaign '" + campaignName + "' requires changes: " + reason);
        publish(event);
    }

    @Async
    public void publishKycApproved(UUID userId, String email, String firstName) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "KYC_APPROVED");
        event.put("userId", userId.toString());
        event.put("email", email);
        event.put("firstName", firstName);
        publish(event);
    }

    @Async
    public void publishKycRejected(UUID userId, String email, String firstName) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "KYC_REJECTED");
        event.put("userId", userId.toString());
        event.put("email", email);
        event.put("firstName", firstName);
        publish(event);
    }

    private void publish(Map<String, Object> event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    event);
            log.debug("Published notification event: type={}", event.get("type"));
        } catch (Exception e) {
            log.error("Failed to publish notification event: type={}, error={}",
                    event.get("type"), e.getMessage());
        }
    }
}
