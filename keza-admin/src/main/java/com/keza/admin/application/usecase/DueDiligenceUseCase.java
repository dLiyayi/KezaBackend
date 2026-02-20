package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.DueDiligenceCheckResponse;
import com.keza.admin.application.dto.DueDiligenceReportResponse;
import com.keza.admin.application.dto.UpdateCheckRequest;
import com.keza.admin.domain.model.DDCheckStatus;
import com.keza.admin.domain.model.DueDiligenceCheck;
import com.keza.admin.domain.model.DueDiligenceReport;
import com.keza.admin.domain.port.out.DueDiligenceCheckRepository;
import com.keza.admin.domain.port.out.DueDiligenceReportRepository;
import com.keza.admin.domain.service.DueDiligenceService;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.audit.Audited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DueDiligenceUseCase {

    private final DueDiligenceService dueDiligenceService;
    private final DueDiligenceCheckRepository checkRepository;
    private final DueDiligenceReportRepository reportRepository;

    @Transactional(readOnly = true)
    public List<DueDiligenceCheckResponse> getChecksForCampaign(UUID campaignId) {
        List<DueDiligenceCheck> checks = checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId);
        if (checks.isEmpty()) {
            // Auto-initialize checks if none exist
            checks = dueDiligenceService.initializeChecksForCampaign(campaignId);
        }
        return checks.stream().map(this::toCheckResponse).toList();
    }

    @Audited(action = "UPDATE_DD_CHECK", entityType = "DueDiligenceCheck")
    @Transactional
    public DueDiligenceCheckResponse updateCheck(UUID checkId, UpdateCheckRequest request, UUID reviewerId) {
        DDCheckStatus status = DDCheckStatus.valueOf(request.getStatus().toUpperCase());
        DueDiligenceCheck check = dueDiligenceService.updateCheck(checkId, status, request.getNotes(), reviewerId);
        return toCheckResponse(check);
    }

    @Audited(action = "GENERATE_DD_REPORT", entityType = "DueDiligenceReport")
    @Transactional
    public DueDiligenceReportResponse generateReport(UUID campaignId, UUID adminId) {
        DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);
        return toReportResponse(report);
    }

    @Transactional(readOnly = true)
    public DueDiligenceReportResponse getReport(UUID campaignId) {
        DueDiligenceReport report = reportRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("DueDiligenceReport", "campaignId", campaignId.toString()));
        return toReportResponse(report);
    }

    private DueDiligenceCheckResponse toCheckResponse(DueDiligenceCheck check) {
        return DueDiligenceCheckResponse.builder()
                .id(check.getId())
                .campaignId(check.getCampaignId())
                .category(check.getCategory())
                .checkName(check.getCheckName())
                .description(check.getDescription())
                .status(check.getStatus().name())
                .notes(check.getNotes())
                .checkedBy(check.getCheckedBy())
                .checkedAt(check.getCheckedAt())
                .aiResult(check.getAiResult())
                .aiConfidence(check.getAiConfidence())
                .weight(check.getWeight())
                .sortOrder(check.getSortOrder())
                .createdAt(check.getCreatedAt())
                .updatedAt(check.getUpdatedAt())
                .build();
    }

    private DueDiligenceReportResponse toReportResponse(DueDiligenceReport report) {
        return DueDiligenceReportResponse.builder()
                .id(report.getId())
                .campaignId(report.getCampaignId())
                .totalChecks(report.getTotalChecks())
                .passedChecks(report.getPassedChecks())
                .failedChecks(report.getFailedChecks())
                .naChecks(report.getNaChecks())
                .overallScore(report.getOverallScore())
                .riskLevel(report.getRiskLevel())
                .recommendation(report.getRecommendation())
                .summary(report.getSummary())
                .generatedBy(report.getGeneratedBy())
                .generatedAt(report.getGeneratedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
