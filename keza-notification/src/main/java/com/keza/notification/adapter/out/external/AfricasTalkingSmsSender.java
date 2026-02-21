package com.keza.notification.adapter.out.external;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.keza.notification.domain.port.out.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SMS sender implementation using Africa's Talking API.
 * Activated when the Africa's Talking API key is configured.
 */
@Component("africasTalkingSmsSender")
@ConditionalOnProperty(name = "keza.notification.africastalking.api-key")
@Slf4j
public class AfricasTalkingSmsSender implements SmsSender {

    private final SmsService smsService;
    private final String senderId;

    public AfricasTalkingSmsSender(
            @Value("${keza.notification.africastalking.api-key}") String apiKey,
            @Value("${keza.notification.africastalking.username:sandbox}") String username,
            @Value("${keza.notification.africastalking.sender-id:}") String senderId) {
        AfricasTalking.initialize(username, apiKey);
        this.smsService = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
        this.senderId = senderId != null && !senderId.isBlank() ? senderId : null;
        log.info("Africa's Talking SMS sender initialized with username: {}", username);
    }

    @Override
    public void send(String phoneNumber, String message) {
        try {
            String[] recipients = new String[]{phoneNumber};
            if (senderId != null) {
                smsService.send(message, senderId, recipients, false);
            } else {
                smsService.send(message, recipients, false);
            }
            log.info("SMS sent successfully to {} via Africa's Talking", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {} via Africa's Talking: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS via Africa's Talking to " + phoneNumber, e);
        }
    }
}
