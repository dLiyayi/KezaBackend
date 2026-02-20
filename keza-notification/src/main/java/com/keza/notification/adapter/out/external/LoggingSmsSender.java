package com.keza.notification.adapter.out.external;

import com.keza.notification.domain.port.out.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback SMS sender that logs SMS content instead of sending.
 * Used in development environments where a real SMS provider is not configured.
 */
@Component
@Slf4j
public class LoggingSmsSender implements SmsSender {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("========== SMS (DEV MODE) ==========");
        log.info("To:      {}", phoneNumber);
        log.info("Message: {}", message);
        log.info("=====================================");
    }
}
