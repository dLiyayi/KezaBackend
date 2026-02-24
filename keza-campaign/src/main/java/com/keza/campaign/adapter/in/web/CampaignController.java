package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.*;
import com.keza.campaign.application.usecase.CampaignUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.OfferingType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignUseCase campaignUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createDraft(Authentication authentication) {
        UUID issuerId = UUID.fromString(authentication.getName());
        CampaignResponse response = campaignUseCase.createDraft(issuerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Draft campaign created"));
    }

    @PutMapping("/{id}/wizard/{step}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateWizardStep(
            @PathVariable UUID id,
            @PathVariable int step,
            @RequestBody @Valid Object stepData,
            Authentication authentication) {

        Object typedData = parseStepData(step, stepData);
        CampaignResponse response = campaignUseCase.updateWizardStep(id, step, typedData);
        return ResponseEntity.ok(ApiResponse.success(response, "Wizard step " + step + " updated"));
    }

    @PutMapping("/{id}/wizard/1")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateStep1(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest.CompanyInfoRequest request) {
        CampaignResponse response = campaignUseCase.updateWizardStep(id, 1, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Company info updated"));
    }

    @PutMapping("/{id}/wizard/2")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateStep2(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest.OfferingDetailsRequest request) {
        CampaignResponse response = campaignUseCase.updateWizardStep(id, 2, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Offering details updated"));
    }

    @PutMapping("/{id}/wizard/3")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateStep3(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest.PitchContentRequest request) {
        CampaignResponse response = campaignUseCase.updateWizardStep(id, 3, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Pitch content updated"));
    }

    @PutMapping("/{id}/wizard/4")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateStep4(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest.FinancialProjectionsRequest request) {
        CampaignResponse response = campaignUseCase.updateWizardStep(id, 4, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Financial projections updated"));
    }

    @PutMapping("/{id}/wizard/5")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateStep5(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest.DocumentsRequest request) {
        CampaignResponse response = campaignUseCase.updateWizardStep(id, 5, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Documents acknowledged"));
    }

    @PutMapping("/{id}/wizard/6")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateStep6(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest.ReviewSubmitRequest request) {
        CampaignResponse response = campaignUseCase.updateWizardStep(id, 6, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Review submission confirmed"));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<CampaignResponse>> submitForReview(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID issuerId = UUID.fromString(authentication.getName());
        CampaignResponse response = campaignUseCase.submitForReview(id, issuerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign submitted for review"));
    }

    @GetMapping("/my-campaigns")
    @PreAuthorize("hasRole('ISSUER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<CampaignResponse>>> getIssuerCampaigns(
            Authentication authentication,
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID issuerId = UUID.fromString(authentication.getName());
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CampaignResponse> campaigns = campaignUseCase.getIssuerCampaigns(issuerId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(campaigns)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getFeaturedCampaigns(
            @RequestParam(defaultValue = "6") int limit) {
        limit = Math.min(limit, 20);
        List<CampaignResponse> featured = campaignUseCase.getFeaturedCampaigns(limit);
        return ResponseEntity.ok(ApiResponse.success(featured));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(@PathVariable UUID id) {
        CampaignResponse response = campaignUseCase.getCampaign(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignBySlug(@PathVariable String slug) {
        CampaignResponse response = campaignUseCase.getCampaignBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CampaignResponse>>> searchCampaigns(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) OfferingType offeringType,
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minTarget,
            @RequestParam(required = false) BigDecimal maxTarget,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        size = Math.min(size, 100);

        CampaignSearchCriteria criteria = CampaignSearchCriteria.builder()
                .industry(industry)
                .offeringType(offeringType)
                .status(status)
                .keyword(keyword)
                .minTarget(minTarget)
                .maxTarget(maxTarget)
                .build();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CampaignResponse> results = campaignUseCase.searchCampaigns(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(results)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CampaignResponse>> approveCampaign(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID adminId = UUID.fromString(authentication.getName());
        CampaignResponse response = campaignUseCase.approveCampaign(id, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign approved"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CampaignResponse>> rejectCampaign(
            @PathVariable UUID id,
            @RequestBody @Valid CampaignRequest.RejectRequest request,
            Authentication authentication) {
        UUID adminId = UUID.fromString(authentication.getName());
        CampaignResponse response = campaignUseCase.rejectCampaign(id, request.getReason(), adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign rejected"));
    }

    private Object parseStepData(int step, Object stepData) {
        // This generic endpoint exists for flexibility; prefer the typed step-specific endpoints
        return stepData;
    }
}
