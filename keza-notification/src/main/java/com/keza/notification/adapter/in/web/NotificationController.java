package com.keza.notification.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import com.keza.notification.application.dto.NotificationPreferenceRequest;
import com.keza.notification.application.dto.NotificationPreferenceResponse;
import com.keza.notification.application.dto.NotificationResponse;
import com.keza.notification.application.usecase.NotificationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = (UUID) authentication.getPrincipal();
        PagedResponse<NotificationResponse> response = notificationUseCase.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        long count = notificationUseCase.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        NotificationResponse response = notificationUseCase.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        notificationUseCase.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        NotificationPreferenceResponse response = notificationUseCase.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        NotificationPreferenceResponse response = notificationUseCase.updatePreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Preferences updated"));
    }
}
