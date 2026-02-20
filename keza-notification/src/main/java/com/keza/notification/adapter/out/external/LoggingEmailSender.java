package com.keza.notification.adapter.out.external;

import com.keza.notification.domain.port.out.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback email sender that logs email content instead of sending.
 * Used in development environments where SendGrid is not configured.
 */
@Component
@ConditionalOnMissingBean(name = "sendGridEmailSender")
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("========== EMAIL (DEV MODE) ==========");
        log.info("To:      {}", to);
        log.info("Subject: {}", subject);
        log.info("Body:    {}", htmlBody);
        log.info("=======================================");
    }
}
