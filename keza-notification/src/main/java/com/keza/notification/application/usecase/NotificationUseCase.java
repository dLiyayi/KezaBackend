package com.keza.notification.application.usecase;

import com.keza.common.dto.PagedResponse;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.notification.application.dto.NotificationPreferenceRequest;
import com.keza.notification.application.dto.NotificationPreferenceResponse;
import com.keza.notification.application.dto.NotificationResponse;
import com.keza.notification.domain.model.Notification;
import com.keza.notification.domain.model.NotificationPreference;
import com.keza.notification.domain.port.out.NotificationPreferenceRepository;
import com.keza.notification.domain.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notification.markAsRead();
        notification = notificationRepository.save(notification);

        log.info("Notification {} marked as read for user {}", notificationId, userId);
        return mapToResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId, Instant.now());
        log.info("Marked {} notifications as read for user {}", updated, userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return mapToPreferenceResponse(pref);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, NotificationPreferenceRequest request) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getSmsEnabled() != null) pref.setSmsEnabled(request.getSmsEnabled());
        if (request.getPushEnabled() != null) pref.setPushEnabled(request.getPushEnabled());
        if (request.getMarketingEnabled() != null) pref.setMarketingEnabled(request.getMarketingEnabled());

        pref = preferenceRepository.save(pref);
        log.info("Updated notification preferences for user {}", userId);
        return mapToPreferenceResponse(pref);
    }

    private NotificationPreference createDefaultPreferences(UUID userId) {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .smsEnabled(true)
                .pushEnabled(true)
                .marketingEnabled(false)
                .build();
        return preferenceRepository.save(pref);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .channel(notification.getChannel().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceResponse mapToPreferenceResponse(NotificationPreference pref) {
        return NotificationPreferenceResponse.builder()
                .id(pref.getId())
                .emailEnabled(pref.isEmailEnabled())
                .smsEnabled(pref.isSmsEnabled())
                .pushEnabled(pref.isPushEnabled())
                .marketingEnabled(pref.isMarketingEnabled())
                .build();
    }
}
