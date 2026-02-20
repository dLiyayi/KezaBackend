package com.keza.notification.adapter.out.external;

import com.keza.notification.domain.port.out.EmailSender;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component("sendGridEmailSender")
@ConditionalOnProperty(name = "keza.notification.sendgrid.api-key")
@Slf4j
public class SendGridEmailSender implements EmailSender {

    private final SendGrid sendGrid;
    private final String fromEmail;
    private final String fromName;

    public SendGridEmailSender(
            @Value("${keza.notification.sendgrid.api-key}") String apiKey,
            @Value("${keza.notification.sendgrid.from-email:noreply@keza.com}") String fromEmail,
            @Value("${keza.notification.sendgrid.from-name:Keza}") String fromName) {
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlBody);
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully to {} with subject '{}' (status: {})", to, subject, response.getStatusCode());
            } else {
                log.error("Failed to send email to {} - status: {}, body: {}", to, response.getStatusCode(), response.getBody());
                throw new RuntimeException("SendGrid returned status " + response.getStatusCode() + ": " + response.getBody());
            }
        } catch (IOException e) {
            log.error("IOException sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email via SendGrid", e);
        }
    }
}
