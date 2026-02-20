package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.*;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.CampaignMedia;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.campaign.domain.service.CampaignStateMachine;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.common.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignUseCase {

    private final CampaignRepository campaignRepository;
    private final CampaignStateMachine stateMachine;

    @Transactional
    public CampaignResponse createDraft(UUID issuerId) {
        Campaign campaign = Campaign.builder()
                .issuerId(issuerId)
                .title("Untitled Campaign")
                .targetAmount(BigDecimal.ZERO)
                .status(CampaignStatus.DRAFT)
                .wizardStep(1)
                .build();

        campaign = campaignRepository.save(campaign);
        log.info("Draft campaign created: {} by issuer: {}", campaign.getId(), issuerId);
        return mapToResponse(campaign);
    }

    @Transactional
    @CacheEvict(value = "campaigns", allEntries = true)
    public CampaignResponse updateWizardStep(UUID campaignId, int step, Object stepData) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Campaign can only be edited in DRAFT status");
        }

        switch (step) {
            case 1 -> applyCompanyInfo(campaign, (CampaignRequest.CompanyInfoRequest) stepData);
            case 2 -> applyOfferingDetails(campaign, (CampaignRequest.OfferingDetailsRequest) stepData);
            case 3 -> applyPitchContent(campaign, (CampaignRequest.PitchContentRequest) stepData);
            case 4 -> applyFinancialProjections(campaign, (CampaignRequest.FinancialProjectionsRequest) stepData);
            case 5 -> applyDocuments(campaign, (CampaignRequest.DocumentsRequest) stepData);
            case 6 -> applyReviewSubmit(campaign, (CampaignRequest.ReviewSubmitRequest) stepData);
            default -> throw new BusinessRuleException("INVALID_STEP",
                    "Invalid wizard step: " + step + ". Must be between 1 and 6");
        }

        if (step > campaign.getWizardStep()) {
            campaign.setWizardStep(step);
        }

        campaign = campaignRepository.save(campaign);
        log.info("Campaign {} updated at wizard step {}", campaignId, step);
        return mapToResponse(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID id) {
        Campaign campaign = findCampaignOrThrow(id);
        return mapToResponse(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignBySlug(String slug) {
        Campaign campaign = campaignRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "slug", slug));
        return mapToResponse(campaign);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "campaigns", key = "#criteria.toString() + #pageable.toString()")
    public Page<CampaignResponse> searchCampaigns(CampaignSearchCriteria criteria, Pageable pageable) {
        Specification<Campaign> spec = buildSpecification(criteria);
        return campaignRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional
    @CacheEvict(value = "campaigns", allEntries = true)
    public CampaignResponse submitForReview(UUID campaignId, UUID issuerId) {
        Campaign campaign = findCampaignOrThrow(campaignId);
        validateOwnership(campaign, issuerId);
        validateCampaignCompleteness(campaign);

        stateMachine.transition(campaign, CampaignStatus.REVIEW, issuerId);
        campaign = campaignRepository.save(campaign);

        log.info("Campaign {} submitted for review by {}", campaignId, issuerId);
        return mapToResponse(campaign);
    }

    @Transactional
    @CacheEvict(value = "campaigns", allEntries = true)
    public CampaignResponse approveCampaign(UUID campaignId, UUID adminId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        if (campaign.getStartDate() == null) {
            campaign.setStartDate(Instant.now());
        }

        stateMachine.transition(campaign, CampaignStatus.LIVE, adminId);
        campaign = campaignRepository.save(campaign);

        log.info("Campaign {} approved by admin {}", campaignId, adminId);
        return mapToResponse(campaign);
    }

    @Transactional
    @CacheEvict(value = "campaigns", allEntries = true)
    public CampaignResponse rejectCampaign(UUID campaignId, String reason, UUID adminId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        stateMachine.transition(campaign, CampaignStatus.DRAFT, adminId);
        campaign = campaignRepository.save(campaign);

        log.info("Campaign {} rejected by admin {} with reason: {}", campaignId, adminId, reason);
        return mapToResponse(campaign);
    }

    // --- Private helpers ---

    private Campaign findCampaignOrThrow(UUID id) {
        return campaignRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }

    private void validateOwnership(Campaign campaign, UUID issuerId) {
        if (!campaign.getIssuerId().equals(issuerId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not own this campaign");
        }
    }

    private void validateCampaignCompleteness(Campaign campaign) {
        if (campaign.getTitle() == null || campaign.getTitle().equals("Untitled Campaign")) {
            throw new BusinessRuleException("INCOMPLETE_CAMPAIGN", "Campaign title is required");
        }
        if (campaign.getDescription() == null || campaign.getDescription().isBlank()) {
            throw new BusinessRuleException("INCOMPLETE_CAMPAIGN", "Campaign description is required");
        }
        if (campaign.getTargetAmount() == null || campaign.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INCOMPLETE_CAMPAIGN", "Target amount must be set and positive");
        }
        if (campaign.getEndDate() == null) {
            throw new BusinessRuleException("INCOMPLETE_CAMPAIGN", "End date is required");
        }
        if (campaign.getCompanyName() == null || campaign.getCompanyName().isBlank()) {
            throw new BusinessRuleException("INCOMPLETE_CAMPAIGN", "Company name is required");
        }
    }

    private void applyCompanyInfo(Campaign campaign, CampaignRequest.CompanyInfoRequest request) {
        campaign.setCompanyName(request.getCompanyName());
        campaign.setCompanyRegistrationNumber(request.getRegistrationNumber());
        campaign.setCompanyWebsite(request.getWebsite());
        campaign.setCompanyAddress(request.getAddress());
        campaign.setIndustry(request.getIndustry());
    }

    private void applyOfferingDetails(Campaign campaign, CampaignRequest.OfferingDetailsRequest request) {
        campaign.setOfferingType(request.getOfferingType());
        campaign.setTargetAmount(request.getTargetAmount());
        campaign.setSharePrice(request.getSharePrice());
        campaign.setTotalShares(request.getTotalShares());
        campaign.setMinInvestment(request.getMinInvestment());
        campaign.setMaxInvestment(request.getMaxInvestment());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
    }

    private void applyPitchContent(Campaign campaign, CampaignRequest.PitchContentRequest request) {
        campaign.setTitle(request.getTitle());
        campaign.setSlug(SlugUtil.slugify(request.getTitle()) + "-" + campaign.getId().toString().substring(0, 8));
        campaign.setTagline(request.getTagline());
        campaign.setDescription(request.getDescription());
        campaign.setPitchVideoUrl(request.getPitchVideoUrl());
    }

    private void applyFinancialProjections(Campaign campaign, CampaignRequest.FinancialProjectionsRequest request) {
        campaign.setFinancialProjections(request.getFinancialProjections());
        campaign.setUseOfFunds(request.getUseOfFunds());
        campaign.setRiskFactors(request.getRiskFactors());
    }

    private void applyDocuments(Campaign campaign, CampaignRequest.DocumentsRequest request) {
        if (!request.isAcknowledged()) {
            throw new BusinessRuleException("DOCUMENTS_NOT_ACKNOWLEDGED",
                    "You must acknowledge that all required documents have been uploaded");
        }
    }

    private void applyReviewSubmit(Campaign campaign, CampaignRequest.ReviewSubmitRequest request) {
        if (!request.isConfirmed()) {
            throw new BusinessRuleException("SUBMISSION_NOT_CONFIRMED",
                    "You must confirm before submitting for review");
        }
    }

    private Specification<Campaign> buildSpecification(CampaignSearchCriteria criteria) {
        Specification<Campaign> spec = Specification.where(
                (root, query, cb) -> cb.isFalse(root.get("deleted")));

        if (criteria.getIndustry() != null && !criteria.getIndustry().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("industry"), criteria.getIndustry()));
        }

        if (criteria.getOfferingType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("offeringType"), criteria.getOfferingType()));
        }

        if (criteria.getStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), criteria.getStatus()));
        }

        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            String keyword = "%" + criteria.getKeyword().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword),
                    cb.like(cb.lower(root.get("companyName")), keyword)
            ));
        }

        if (criteria.getMinTarget() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("targetAmount"), criteria.getMinTarget()));
        }

        if (criteria.getMaxTarget() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("targetAmount"), criteria.getMaxTarget()));
        }

        return spec;
    }

    private CampaignResponse mapToResponse(Campaign campaign) {
        List<CampaignResponse.CampaignMediaResponse> mediaResponses = null;
        if (campaign.getMedia() != null) {
            mediaResponses = campaign.getMedia().stream()
                    .map(m -> CampaignResponse.CampaignMediaResponse.builder()
                            .id(m.getId())
                            .fileKey(m.getFileKey())
                            .fileName(m.getFileName())
                            .fileSize(m.getFileSize())
                            .contentType(m.getContentType())
                            .mediaType(m.getMediaType() != null ? m.getMediaType().name() : null)
                            .sortOrder(m.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        return CampaignResponse.builder()
                .id(campaign.getId())
                .issuerId(campaign.getIssuerId())
                .title(campaign.getTitle())
                .slug(campaign.getSlug())
                .tagline(campaign.getTagline())
                .description(campaign.getDescription())
                .industry(campaign.getIndustry())
                .companyName(campaign.getCompanyName())
                .companyRegistrationNumber(campaign.getCompanyRegistrationNumber())
                .companyWebsite(campaign.getCompanyWebsite())
                .companyAddress(campaign.getCompanyAddress())
                .offeringType(campaign.getOfferingType())
                .targetAmount(campaign.getTargetAmount())
                .minimumAmount(campaign.getMinimumAmount())
                .maximumAmount(campaign.getMaximumAmount())
                .raisedAmount(campaign.getRaisedAmount())
                .sharePrice(campaign.getSharePrice())
                .totalShares(campaign.getTotalShares())
                .soldShares(campaign.getSoldShares())
                .minInvestment(campaign.getMinInvestment())
                .maxInvestment(campaign.getMaxInvestment())
                .investorCount(campaign.getInvestorCount())
                .status(campaign.getStatus())
                .wizardStep(campaign.getWizardStep())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .fundedAt(campaign.getFundedAt())
                .pitchVideoUrl(campaign.getPitchVideoUrl())
                .financialProjections(campaign.getFinancialProjections())
                .riskFactors(campaign.getRiskFactors())
                .useOfFunds(campaign.getUseOfFunds())
                .teamMembers(campaign.getTeamMembers())
                .media(mediaResponses)
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .build();
    }
}
