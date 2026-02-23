package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.AnnouncementRequest;
import com.keza.admin.application.dto.AnnouncementResponse;
import com.keza.admin.domain.model.Announcement;
import com.keza.admin.domain.port.out.AnnouncementRepository;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AuditLogger auditLogger;

    @Transactional
    public AnnouncementResponse createAnnouncement(UUID authorId, AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "GENERAL")
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .expiresAt(request.getExpiresAt())
                .targetAudience(request.getTargetAudience() != null ? request.getTargetAudience() : "ALL")
                .authorId(authorId)
                .build();

        announcement = announcementRepository.save(announcement);
        auditLogger.log("CREATE_ANNOUNCEMENT", "Announcement", announcement.getId().toString(),
                "Announcement created: " + request.getTitle());
        log.info("Announcement {} created by {}", announcement.getId(), authorId);
        return mapToResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID announcementId, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", announcementId));

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        if (request.getType() != null) announcement.setType(request.getType());
        if (request.getPriority() != null) announcement.setPriority(request.getPriority());
        if (request.getExpiresAt() != null) announcement.setExpiresAt(request.getExpiresAt());
        if (request.getTargetAudience() != null) announcement.setTargetAudience(request.getTargetAudience());

        announcement = announcementRepository.save(announcement);
        log.info("Announcement {} updated", announcementId);
        return mapToResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse publishAnnouncement(UUID announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", announcementId));

        announcement.setPublished(true);
        announcement.setPublishedAt(Instant.now());

        announcement = announcementRepository.save(announcement);
        auditLogger.log("PUBLISH_ANNOUNCEMENT", "Announcement", announcementId.toString(),
                "false", "true", "Announcement published");
        log.info("Announcement {} published", announcementId);
        return mapToResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse unpublishAnnouncement(UUID announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", announcementId));

        announcement.setPublished(false);

        announcement = announcementRepository.save(announcement);
        log.info("Announcement {} unpublished", announcementId);
        return mapToResponse(announcement);
    }

    @Transactional
    public void deleteAnnouncement(UUID announcementId) {
        if (!announcementRepository.existsById(announcementId)) {
            throw new ResourceNotFoundException("Announcement", announcementId);
        }
        announcementRepository.deleteById(announcementId);
        auditLogger.log("DELETE_ANNOUNCEMENT", "Announcement", announcementId.toString(),
                "Announcement deleted");
        log.info("Announcement {} deleted", announcementId);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAllAnnouncements(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getActiveAnnouncements(Pageable pageable) {
        return announcementRepository
                .findByPublishedTrueAndExpiresAtIsNullOrPublishedTrueAndExpiresAtAfterOrderByPublishedAtDesc(
                        Instant.now(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncement(UUID announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", announcementId));
        return mapToResponse(announcement);
    }

    private AnnouncementResponse mapToResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .published(announcement.isPublished())
                .publishedAt(announcement.getPublishedAt())
                .expiresAt(announcement.getExpiresAt())
                .authorId(announcement.getAuthorId())
                .targetAudience(announcement.getTargetAudience())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
