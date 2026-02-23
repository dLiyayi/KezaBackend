package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.AnnouncementResponse;
import com.keza.admin.application.usecase.AnnouncementUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class PublicAnnouncementController {

    private final AnnouncementUseCase announcementUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getActiveAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(size, 50);
        Page<AnnouncementResponse> announcements = announcementUseCase.getActiveAnnouncements(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(announcements)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse response = announcementUseCase.getAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
