package com.keza.app.listener;

import com.keza.infrastructure.messaging.NotificationEventPublisher;
import com.keza.investment.domain.event.InvestmentCreatedEvent;
import com.keza.user.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBridgeListener {

    private final NotificationEventPublisher notificationEventPublisher;

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Bridging UserRegisteredEvent to notification queue for user {}", event.userId());
        notificationEventPublisher.publishUserRegistered(
                event.userId(), event.email(), event.firstName());
    }

    @EventListener
    public void onInvestmentCreated(InvestmentCreatedEvent event) {
        log.info("Bridging InvestmentCreatedEvent to notification queue for investor {}", event.investorId());
        notificationEventPublisher.publishInvestmentConfirmed(
                event.investorId(), "", "",
                "Campaign", event.amount(), "KES");
    }
}
