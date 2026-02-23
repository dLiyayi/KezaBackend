package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.AdminCampaignResponse;
import com.keza.admin.domain.port.out.AdminCampaignRepository;
import com.keza.common.dto.PagedResponse;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCampaignUseCase {

    private final AdminCampaignRepository adminCampaignRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public PagedResponse<AdminCampaignResponse> listCampaigns(String status, String industry, String search, Pageable pageable) {
        Page<Map<String, Object>> page = adminCampaignRepository.findCampaigns(status, industry, search, pageable);

        List<AdminCampaignResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<AdminCampaignResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminCampaignResponse getCampaign(UUID campaignId) {
        Map<String, Object> data = adminCampaignRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
        return mapToResponse(data);
    }

    @Transactional
    public AdminCampaignResponse assignReviewer(UUID campaignId, UUID reviewerId) {
        int updated = adminCampaignRepository.assignReviewer(campaignId, reviewerId);
        if (updated == 0) {
            throw new BusinessRuleException("ASSIGN_FAILED",
                    "Campaign not found or not in REVIEW status");
        }

        auditLogger.log("ASSIGN_REVIEWER", "Campaign", campaignId.toString(),
                null, reviewerId.toString(), "Reviewer assigned to campaign");

        log.info("Reviewer {} assigned to campaign {}", reviewerId, campaignId);
        return getCampaign(campaignId);
    }

    private AdminCampaignResponse mapToResponse(Map<String, Object> data) {
        return AdminCampaignResponse.builder()
                .id((UUID) data.get("id"))
                .title((String) data.get("title"))
                .slug((String) data.get("slug"))
                .companyName((String) data.get("companyName"))
                .industry((String) data.get("industry"))
                .status((String) data.get("status"))
                .targetAmount(data.get("targetAmount") instanceof BigDecimal bd ? bd : null)
                .raisedAmount(data.get("raisedAmount") instanceof BigDecimal bd ? bd : null)
                .investorCount(data.get("investorCount") instanceof Number n ? n.intValue() : null)
                .issuerId(data.get("issuerId") instanceof UUID uid ? uid : null)
                .issuerFirstName((String) data.get("issuerFirstName"))
                .issuerLastName((String) data.get("issuerLastName"))
                .issuerEmail((String) data.get("issuerEmail"))
                .startDate(data.get("startDate") instanceof Instant inst ? inst : null)
                .endDate(data.get("endDate") instanceof Instant inst ? inst : null)
                .createdAt(data.get("createdAt") instanceof Instant inst ? inst : null)
                .updatedAt(data.get("updatedAt") instanceof Instant inst ? inst : null)
                .build();
    }
}
