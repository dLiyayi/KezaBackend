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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DueDiligenceUseCase")
class DueDiligenceUseCaseTest {

    @Mock
    private DueDiligenceService dueDiligenceService;

    @Mock
    private DueDiligenceCheckRepository checkRepository;

    @Mock
    private DueDiligenceReportRepository reportRepository;

    @InjectMocks
    private DueDiligenceUseCase dueDiligenceUseCase;

    private UUID campaignId;
    private UUID reviewerId;
    private UUID checkId;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        checkId = UUID.randomUUID();
    }

    private DueDiligenceCheck buildCheck(DDCheckStatus status, String category, String name, int sortOrder) {
        DueDiligenceCheck check = DueDiligenceCheck.builder()
                .campaignId(campaignId)
                .category(category)
                .checkName(name)
                .description("Test description for " + name)
                .status(status)
                .weight(BigDecimal.valueOf(1.5))
                .sortOrder(sortOrder)
                .build();
        check.setId(UUID.randomUUID());
        check.setCreatedAt(Instant.now());
        check.setUpdatedAt(Instant.now());
        return check;
    }

    @Nested
    @DisplayName("getChecksForCampaign")
    class GetChecksForCampaign {

        @Test
        @DisplayName("should return existing checks when they are already initialized")
        void shouldReturnExistingChecks() {
            List<DueDiligenceCheck> existingChecks = List.of(
                    buildCheck(DDCheckStatus.PENDING, "LEGAL", "Business Registration", 0),
                    buildCheck(DDCheckStatus.PASSED, "FINANCIAL", "Revenue Growth", 1));
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(existingChecks);

            List<DueDiligenceCheckResponse> result = dueDiligenceUseCase.getChecksForCampaign(campaignId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCategory()).isEqualTo("LEGAL");
            assertThat(result.get(0).getCheckName()).isEqualTo("Business Registration");
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
            assertThat(result.get(1).getStatus()).isEqualTo("PASSED");
            verify(dueDiligenceService, never()).initializeChecksForCampaign(any());
        }

        @Test
        @DisplayName("should auto-initialize checks when none exist")
        void shouldAutoInitializeWhenEmpty() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());

            List<DueDiligenceCheck> initializedChecks = List.of(
                    buildCheck(DDCheckStatus.PENDING, "LEGAL", "Business Registration", 0));
            when(dueDiligenceService.initializeChecksForCampaign(campaignId))
                    .thenReturn(initializedChecks);

            List<DueDiligenceCheckResponse> result = dueDiligenceUseCase.getChecksForCampaign(campaignId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("LEGAL");
            verify(dueDiligenceService).initializeChecksForCampaign(campaignId);
        }

        @Test
        @DisplayName("should map all check response fields correctly")
        void shouldMapAllResponseFields() {
            DueDiligenceCheck check = buildCheck(DDCheckStatus.PASSED, "LEGAL", "Tax Check", 5);
            check.setNotes("All clear");
            check.setCheckedBy(reviewerId);
            check.setCheckedAt(Instant.now());
            check.setAiResult("PASS");
            check.setAiConfidence(BigDecimal.valueOf(0.95));

            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of(check));

            List<DueDiligenceCheckResponse> result = dueDiligenceUseCase.getChecksForCampaign(campaignId);
            DueDiligenceCheckResponse resp = result.get(0);

            assertThat(resp.getId()).isEqualTo(check.getId());
            assertThat(resp.getCampaignId()).isEqualTo(campaignId);
            assertThat(resp.getCategory()).isEqualTo("LEGAL");
            assertThat(resp.getCheckName()).isEqualTo("Tax Check");
            assertThat(resp.getDescription()).isEqualTo("Test description for Tax Check");
            assertThat(resp.getStatus()).isEqualTo("PASSED");
            assertThat(resp.getNotes()).isEqualTo("All clear");
            assertThat(resp.getCheckedBy()).isEqualTo(reviewerId);
            assertThat(resp.getCheckedAt()).isNotNull();
            assertThat(resp.getAiResult()).isEqualTo("PASS");
            assertThat(resp.getAiConfidence()).isEqualByComparingTo(BigDecimal.valueOf(0.95));
            assertThat(resp.getWeight()).isEqualByComparingTo(BigDecimal.valueOf(1.5));
            assertThat(resp.getSortOrder()).isEqualTo(5);
            assertThat(resp.getCreatedAt()).isNotNull();
            assertThat(resp.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateCheck")
    class UpdateCheck {

        @Test
        @DisplayName("should update check status and return response")
        void shouldUpdateCheckAndReturnResponse() {
            UpdateCheckRequest request = UpdateCheckRequest.builder()
                    .status("PASSED")
                    .notes("Verified successfully")
                    .build();

            DueDiligenceCheck updatedCheck = buildCheck(DDCheckStatus.PASSED, "LEGAL", "Registration Check", 0);
            updatedCheck.setId(checkId);
            updatedCheck.setNotes("Verified successfully");
            updatedCheck.setCheckedBy(reviewerId);
            updatedCheck.setCheckedAt(Instant.now());

            when(dueDiligenceService.updateCheck(checkId, DDCheckStatus.PASSED, "Verified successfully", reviewerId))
                    .thenReturn(updatedCheck);

            DueDiligenceCheckResponse result = dueDiligenceUseCase.updateCheck(checkId, request, reviewerId);

            assertThat(result.getId()).isEqualTo(checkId);
            assertThat(result.getStatus()).isEqualTo("PASSED");
            assertThat(result.getNotes()).isEqualTo("Verified successfully");
        }

        @Test
        @DisplayName("should handle FAILED status update")
        void shouldHandleFailedStatus() {
            UpdateCheckRequest request = UpdateCheckRequest.builder()
                    .status("FAILED")
                    .notes("Documents expired")
                    .build();

            DueDiligenceCheck updatedCheck = buildCheck(DDCheckStatus.FAILED, "LEGAL", "License Check", 0);
            updatedCheck.setId(checkId);

            when(dueDiligenceService.updateCheck(checkId, DDCheckStatus.FAILED, "Documents expired", reviewerId))
                    .thenReturn(updatedCheck);

            DueDiligenceCheckResponse result = dueDiligenceUseCase.updateCheck(checkId, request, reviewerId);

            assertThat(result.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("should handle NOT_APPLICABLE status update")
        void shouldHandleNotApplicableStatus() {
            UpdateCheckRequest request = UpdateCheckRequest.builder()
                    .status("NOT_APPLICABLE")
                    .notes("Not relevant for this company type")
                    .build();

            DueDiligenceCheck updatedCheck = buildCheck(DDCheckStatus.NOT_APPLICABLE, "OPERATIONAL", "Inventory", 0);
            updatedCheck.setId(checkId);

            when(dueDiligenceService.updateCheck(checkId, DDCheckStatus.NOT_APPLICABLE,
                    "Not relevant for this company type", reviewerId))
                    .thenReturn(updatedCheck);

            DueDiligenceCheckResponse result = dueDiligenceUseCase.updateCheck(checkId, request, reviewerId);

            assertThat(result.getStatus()).isEqualTo("NOT_APPLICABLE");
        }

        @Test
        @DisplayName("should handle case-insensitive status input")
        void shouldHandleCaseInsensitiveStatus() {
            UpdateCheckRequest request = UpdateCheckRequest.builder()
                    .status("passed")
                    .notes("OK")
                    .build();

            DueDiligenceCheck updatedCheck = buildCheck(DDCheckStatus.PASSED, "LEGAL", "Check", 0);
            updatedCheck.setId(checkId);

            when(dueDiligenceService.updateCheck(checkId, DDCheckStatus.PASSED, "OK", reviewerId))
                    .thenReturn(updatedCheck);

            DueDiligenceCheckResponse result = dueDiligenceUseCase.updateCheck(checkId, request, reviewerId);

            assertThat(result.getStatus()).isEqualTo("PASSED");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for invalid status")
        void shouldThrowForInvalidStatus() {
            UpdateCheckRequest request = UpdateCheckRequest.builder()
                    .status("INVALID_STATUS")
                    .notes("test")
                    .build();

            assertThatThrownBy(() -> dueDiligenceUseCase.updateCheck(checkId, request, reviewerId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("generateReport")
    class GenerateReport {

        @Test
        @DisplayName("should generate report and return response")
        void shouldGenerateReportAndReturnResponse() {
            UUID adminId = UUID.randomUUID();
            DueDiligenceReport report = DueDiligenceReport.builder()
                    .campaignId(campaignId)
                    .totalChecks(144)
                    .passedChecks(130)
                    .failedChecks(4)
                    .naChecks(10)
                    .overallScore(BigDecimal.valueOf(97.01))
                    .riskLevel("LOW")
                    .recommendation("APPROVE")
                    .summary("Due Diligence Report Summary")
                    .generatedBy(adminId)
                    .generatedAt(Instant.now())
                    .build();
            report.setId(UUID.randomUUID());
            report.setCreatedAt(Instant.now());
            report.setUpdatedAt(Instant.now());

            when(dueDiligenceService.generateReport(campaignId, adminId)).thenReturn(report);

            DueDiligenceReportResponse result = dueDiligenceUseCase.generateReport(campaignId, adminId);

            assertThat(result.getCampaignId()).isEqualTo(campaignId);
            assertThat(result.getTotalChecks()).isEqualTo(144);
            assertThat(result.getPassedChecks()).isEqualTo(130);
            assertThat(result.getFailedChecks()).isEqualTo(4);
            assertThat(result.getNaChecks()).isEqualTo(10);
            assertThat(result.getOverallScore()).isEqualByComparingTo(BigDecimal.valueOf(97.01));
            assertThat(result.getRiskLevel()).isEqualTo("LOW");
            assertThat(result.getRecommendation()).isEqualTo("APPROVE");
            assertThat(result.getSummary()).isNotBlank();
            assertThat(result.getGeneratedBy()).isEqualTo(adminId);
            assertThat(result.getGeneratedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getReport")
    class GetReport {

        @Test
        @DisplayName("should return existing report for campaign")
        void shouldReturnExistingReport() {
            DueDiligenceReport report = DueDiligenceReport.builder()
                    .campaignId(campaignId)
                    .totalChecks(144)
                    .passedChecks(120)
                    .failedChecks(14)
                    .naChecks(10)
                    .overallScore(BigDecimal.valueOf(89.55))
                    .riskLevel("LOW")
                    .recommendation("APPROVE")
                    .summary("Summary text")
                    .build();
            report.setId(UUID.randomUUID());
            report.setCreatedAt(Instant.now());
            report.setUpdatedAt(Instant.now());

            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.of(report));

            DueDiligenceReportResponse result = dueDiligenceUseCase.getReport(campaignId);

            assertThat(result.getCampaignId()).isEqualTo(campaignId);
            assertThat(result.getRiskLevel()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when report does not exist")
        void shouldThrowWhenReportNotFound() {
            UUID unknownCampaign = UUID.randomUUID();
            when(reportRepository.findByCampaignId(unknownCampaign)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dueDiligenceUseCase.getReport(unknownCampaign))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("DueDiligenceReport");
        }
    }
}
