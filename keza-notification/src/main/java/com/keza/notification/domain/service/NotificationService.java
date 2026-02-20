package com.keza.notification.domain.service;

import com.keza.notification.domain.model.Notification;
import com.keza.notification.domain.model.NotificationChannel;
import com.keza.notification.domain.model.NotificationPreference;
import com.keza.notification.domain.port.out.EmailSender;
import com.keza.notification.domain.port.out.NotificationPreferenceRepository;
import com.keza.notification.domain.port.out.NotificationRepository;
import com.keza.notification.domain.port.out.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    /**
     * Critical notification types that bypass user preferences.
     */
    private static final Set<String> CRITICAL_TYPES = Set.of(
            "PASSWORD_RESET",
            "ACCOUNT_LOCKED",
            "SECURITY_ALERT",
            "EMAIL_VERIFICATION"
    );

    @Transactional
    public Notification sendNotification(UUID userId, String type, String title, String message,
                                         NotificationChannel channel) {
        return sendNotification(userId, type, title, message, channel, null);
    }

    @Transactional
    public Notification sendNotification(UUID userId, String type, String title, String message,
                                         NotificationChannel channel, String data) {
        // Check user preferences for non-critical notifications
        if (!CRITICAL_TYPES.contains(type) && !isChannelEnabled(userId, channel)) {
            log.info("Notification skipped for user {} - channel {} disabled by preference", userId, channel);
            return null;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .channel(channel)
                .title(title)
                .message(message)
                .data(data)
                .build();

        notification = notificationRepository.save(notification);

        // Dispatch to the appropriate channel
        try {
            dispatch(notification);
            notification.markAsSent();
            notification = notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to dispatch notification {} via {}: {}", notification.getId(), channel, e.getMessage(), e);
        }

        return notification;
    }

    private void dispatch(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL -> {
                log.info("Dispatching email notification to user {}: {}", notification.getUserId(), notification.getTitle());
                // Email dispatch requires user email - handled at a higher level or via event data
            }
            case SMS -> {
                log.info("Dispatching SMS notification to user {}: {}", notification.getUserId(), notification.getTitle());
                // SMS dispatch requires phone number - handled at a higher level or via event data
            }
            case PUSH -> {
                log.info("Dispatching push notification to user {}: {}", notification.getUserId(), notification.getTitle());
                // Push notification dispatch - future implementation
            }
            case IN_APP -> {
                log.debug("In-app notification stored for user {}: {}", notification.getUserId(), notification.getTitle());
                // In-app notifications are stored in the database and fetched by the client
            }
        }
    }

    /**
     * Sends an email notification with full email details.
     */
    @Transactional
    public Notification sendEmailNotification(UUID userId, String type, String title, String message,
                                               String recipientEmail, String htmlBody, String data) {
        if (!CRITICAL_TYPES.contains(type) && !isChannelEnabled(userId, NotificationChannel.EMAIL)) {
            log.info("Email notification skipped for user {} - email disabled by preference", userId);
            return null;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .channel(NotificationChannel.EMAIL)
                .title(title)
                .message(message)
                .data(data)
                .build();

        notification = notificationRepository.save(notification);

        try {
            emailSender.send(recipientEmail, title, htmlBody);
            notification.markAsSent();
            notification = notificationRepository.save(notification);
            log.info("Email notification sent to {} for user {}", recipientEmail, userId);
        } catch (Exception e) {
            log.error("Failed to send email to {} for notification {}: {}", recipientEmail, notification.getId(), e.getMessage(), e);
        }

        return notification;
    }

    /**
     * Sends an SMS notification with full SMS details.
     */
    @Transactional
    public Notification sendSmsNotification(UUID userId, String type, String title, String message,
                                             String phoneNumber, String data) {
        if (!CRITICAL_TYPES.contains(type) && !isChannelEnabled(userId, NotificationChannel.SMS)) {
            log.info("SMS notification skipped for user {} - SMS disabled by preference", userId);
            return null;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .channel(NotificationChannel.SMS)
                .title(title)
                .message(message)
                .data(data)
                .build();

        notification = notificationRepository.save(notification);

        try {
            smsSender.send(phoneNumber, message);
            notification.markAsSent();
            notification = notificationRepository.save(notification);
            log.info("SMS notification sent to {} for user {}", phoneNumber, userId);
        } catch (Exception e) {
            log.error("Failed to send SMS to {} for notification {}: {}", phoneNumber, notification.getId(), e.getMessage(), e);
        }

        return notification;
    }

    private boolean isChannelEnabled(UUID userId, NotificationChannel channel) {
        return preferenceRepository.findByUserId(userId)
                .map(pref -> switch (channel) {
                    case EMAIL -> pref.isEmailEnabled();
                    case SMS -> pref.isSmsEnabled();
                    case PUSH -> pref.isPushEnabled();
                    case IN_APP -> true; // In-app notifications are always enabled
                })
                .orElse(true); // Default to enabled if no preferences exist
    }
}
