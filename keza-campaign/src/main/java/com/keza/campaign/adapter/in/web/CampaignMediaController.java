package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.CampaignResponse;
import com.keza.campaign.application.usecase.CampaignMediaUseCase;
import com.keza.campaign.domain.model.CampaignMedia;
import com.keza.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/media")
@RequiredArgsConstructor
public class CampaignMediaController {

    private final CampaignMediaUseCase campaignMediaUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<CampaignResponse.CampaignMediaResponse>> uploadMedia(
            @PathVariable UUID campaignId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", required = false) CampaignMedia.MediaType mediaType,
            Authentication authentication) {
        UUID issuerId = (UUID) authentication.getPrincipal();
        CampaignResponse.CampaignMediaResponse response = campaignMediaUseCase.uploadMedia(campaignId, issuerId, file, mediaType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Media uploaded successfully"));
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable UUID campaignId,
            @PathVariable UUID mediaId,
            Authentication authentication) {
        UUID issuerId = (UUID) authentication.getPrincipal();
        campaignMediaUseCase.deleteMedia(campaignId, mediaId, issuerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Media deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CampaignResponse.CampaignMediaResponse>>> getMedia(
            @PathVariable UUID campaignId) {
        List<CampaignResponse.CampaignMediaResponse> media = campaignMediaUseCase.getMediaForCampaign(campaignId);
        return ResponseEntity.ok(ApiResponse.success(media));
    }
}
