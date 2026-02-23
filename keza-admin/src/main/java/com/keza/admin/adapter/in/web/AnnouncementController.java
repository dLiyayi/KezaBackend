package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.AnnouncementRequest;
import com.keza.admin.application.dto.AnnouncementResponse;
import com.keza.admin.application.usecase.AnnouncementUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AnnouncementController {

    private final AnnouncementUseCase announcementUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            Authentication authentication,
            @Valid @RequestBody AnnouncementRequest request) {
        UUID authorId = UUID.fromString(authentication.getName());
        AnnouncementResponse response = announcementUseCase.createAnnouncement(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Announcement created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable UUID id,
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementUseCase.updateAnnouncement(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement updated successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> publishAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse response = announcementUseCase.publishAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement published successfully"));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> unpublishAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse response = announcementUseCase.unpublishAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement unpublished"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable UUID id) {
        announcementUseCase.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Announcement deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Page<AnnouncementResponse> announcements = announcementUseCase.getAllAnnouncements(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(announcements)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse response = announcementUseCase.getAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
