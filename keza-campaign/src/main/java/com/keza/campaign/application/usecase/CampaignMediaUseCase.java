package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.CampaignResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.CampaignMedia;
import com.keza.campaign.domain.port.out.CampaignMediaRepository;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.config.StorageConfig;
import com.keza.infrastructure.config.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignMediaUseCase {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofHours(1);

    private static final Map<CampaignMedia.MediaType, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            CampaignMedia.MediaType.IMAGE, Set.of("image/jpeg", "image/png", "image/webp"),
            CampaignMedia.MediaType.VIDEO, Set.of("video/mp4", "video/quicktime"),
            CampaignMedia.MediaType.DOCUMENT, Set.of("application/pdf")
    );

    private final CampaignMediaRepository campaignMediaRepository;
    private final CampaignRepository campaignRepository;
    private final StorageService storageService;
    private final StorageConfig storageConfig;

    @Transactional
    public CampaignResponse.CampaignMediaResponse uploadMedia(UUID campaignId, UUID issuerId, MultipartFile file, CampaignMedia.MediaType mediaType) {
        Campaign campaign = findCampaignOrThrow(campaignId);
        validateOwnership(campaign, issuerId);

        if (file.isEmpty()) {
            throw new BusinessRuleException("EMPTY_FILE", "File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("FILE_TOO_LARGE", "File size must not exceed 50MB");
        }

        String contentType = file.getContentType();
        CampaignMedia.MediaType resolvedType = mediaType != null ? mediaType : resolveMediaType(contentType);

        Set<String> allowed = ALLOWED_CONTENT_TYPES.get(resolvedType);
        if (allowed == null || !allowed.contains(contentType)) {
            throw new BusinessRuleException("INVALID_FILE_TYPE",
                    "File type " + contentType + " is not allowed for media type " + resolvedType);
        }

        String bucket = storageConfig.getBuckets().get("campaign-media");
        String fileKey = "campaigns/" + campaignId + "/" + resolvedType.name().toLowerCase() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        try {
            storageService.upload(bucket, fileKey, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new BusinessRuleException("UPLOAD_FAILED", "Failed to upload file: " + e.getMessage());
        }

        int nextSortOrder = campaignMediaRepository.findByCampaignIdOrderBySortOrderAsc(campaignId).size();

        CampaignMedia media = CampaignMedia.builder()
                .campaignId(campaignId)
                .fileKey(fileKey)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(contentType)
                .mediaType(resolvedType)
                .sortOrder(nextSortOrder)
                .build();

        media = campaignMediaRepository.save(media);
        log.info("Media uploaded: {} for campaign: {}", media.getId(), campaignId);

        return mapToResponse(media, bucket);
    }

    @Transactional
    public void deleteMedia(UUID campaignId, UUID mediaId, UUID issuerId) {
        Campaign campaign = findCampaignOrThrow(campaignId);
        validateOwnership(campaign, issuerId);

        CampaignMedia media = campaignMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignMedia", mediaId));

        if (!media.getCampaignId().equals(campaignId)) {
            throw new BusinessRuleException("INVALID_MEDIA", "Media does not belong to this campaign");
        }

        String bucket = storageConfig.getBuckets().get("campaign-media");
        storageService.delete(bucket, media.getFileKey());

        campaignMediaRepository.delete(media);
        log.info("Media deleted: {} for campaign: {}", mediaId, campaignId);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse.CampaignMediaResponse> getMediaForCampaign(UUID campaignId) {
        findCampaignOrThrow(campaignId);
        String bucket = storageConfig.getBuckets().get("campaign-media");

        return campaignMediaRepository.findByCampaignIdOrderBySortOrderAsc(campaignId).stream()
                .map(m -> mapToResponse(m, bucket))
                .collect(Collectors.toList());
    }

    private CampaignMedia.MediaType resolveMediaType(String contentType) {
        if (contentType == null) {
            throw new BusinessRuleException("INVALID_FILE_TYPE", "Content type is required");
        }
        for (Map.Entry<CampaignMedia.MediaType, Set<String>> entry : ALLOWED_CONTENT_TYPES.entrySet()) {
            if (entry.getValue().contains(contentType)) {
                return entry.getKey();
            }
        }
        throw new BusinessRuleException("INVALID_FILE_TYPE", "Unsupported file type: " + contentType);
    }

    private Campaign findCampaignOrThrow(UUID campaignId) {
        return campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }

    private void validateOwnership(Campaign campaign, UUID issuerId) {
        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not own this campaign");
        }
    }

    private CampaignResponse.CampaignMediaResponse mapToResponse(CampaignMedia media, String bucket) {
        String presignedUrl = storageService.generatePresignedUrl(bucket, media.getFileKey(), PRESIGNED_URL_EXPIRATION);

        return CampaignResponse.CampaignMediaResponse.builder()
                .id(media.getId())
                .fileKey(presignedUrl)
                .fileName(media.getFileName())
                .fileSize(media.getFileSize())
                .contentType(media.getContentType())
                .mediaType(media.getMediaType() != null ? media.getMediaType().name() : null)
                .sortOrder(media.getSortOrder())
                .build();
    }
}
