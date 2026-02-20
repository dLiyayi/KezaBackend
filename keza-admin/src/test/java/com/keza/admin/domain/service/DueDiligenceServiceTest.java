package com.keza.admin.domain.service;

import com.keza.admin.domain.model.DDCheckStatus;
import com.keza.admin.domain.model.DueDiligenceCheck;
import com.keza.admin.domain.model.DueDiligenceReport;
import com.keza.admin.domain.port.out.DueDiligenceCheckRepository;
import com.keza.admin.domain.port.out.DueDiligenceReportRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DueDiligenceService")
class DueDiligenceServiceTest {

    @Mock
    private DueDiligenceCheckRepository checkRepository;

    @Mock
    private DueDiligenceReportRepository reportRepository;

    @InjectMocks
    private DueDiligenceService dueDiligenceService;

    @Captor
    private ArgumentCaptor<List<DueDiligenceCheck>> checksCaptor;

    private UUID campaignId;
    private UUID adminId;
    private UUID reviewerId;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("initializeChecksForCampaign")
    class InitializeChecksForCampaign {

        @Test
        @DisplayName("should create exactly 144 checks across 5 categories")
        void shouldCreate144Checks() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DueDiligenceCheck> result = dueDiligenceService.initializeChecksForCampaign(campaignId);

            assertThat(result).hasSize(144);
        }

        @Test
        @DisplayName("should create checks with correct category distribution")
        void shouldHaveCorrectCategoryDistribution() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DueDiligenceCheck> result = dueDiligenceService.initializeChecksForCampaign(campaignId);

            Map<String, Long> categoryCounts = new LinkedHashMap<>();
            result.forEach(c -> categoryCounts.merge(c.getCategory(), 1L, Long::sum));

            assertThat(categoryCounts.get("LEGAL")).isEqualTo(30);
            assertThat(categoryCounts.get("FINANCIAL")).isEqualTo(35);
            assertThat(categoryCounts.get("MANAGEMENT")).isEqualTo(25);
            assertThat(categoryCounts.get("MARKET")).isEqualTo(30);
            assertThat(categoryCounts.get("OPERATIONAL")).isEqualTo(24);
        }

        @Test
        @DisplayName("should initialize all checks with PENDING status")
        void shouldInitializeAllAsPending() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DueDiligenceCheck> result = dueDiligenceService.initializeChecksForCampaign(campaignId);

            assertThat(result).allMatch(c -> c.getStatus() == DDCheckStatus.PENDING);
        }

        @Test
        @DisplayName("should assign sequential sort orders")
        void shouldAssignSequentialSortOrders() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DueDiligenceCheck> result = dueDiligenceService.initializeChecksForCampaign(campaignId);

            List<Integer> sortOrders = result.stream().map(DueDiligenceCheck::getSortOrder).toList();
            assertThat(sortOrders).isEqualTo(IntStream.range(0, 144).boxed().toList());
        }

        @Test
        @DisplayName("should assign campaign ID to all checks")
        void shouldAssignCampaignIdToAll() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DueDiligenceCheck> result = dueDiligenceService.initializeChecksForCampaign(campaignId);

            assertThat(result).allMatch(c -> c.getCampaignId().equals(campaignId));
        }

        @Test
        @DisplayName("should assign positive weights to all checks")
        void shouldAssignPositiveWeights() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DueDiligenceCheck> result = dueDiligenceService.initializeChecksForCampaign(campaignId);

            assertThat(result).allMatch(c -> c.getWeight().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("should assign non-blank check names and descriptions")
        void shouldAssignNonBlankNamesAndDescriptions() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DueDiligenceCheck> result = dueDiligenceService.initializeChecksForCampaign(campaignId);

            assertThat(result).allMatch(c -> c.getCheckName() != null && !c.getCheckName().isBlank());
            assertThat(result).allMatch(c -> c.getDescription() != null && !c.getDescription().isBlank());
        }

        @Test
        @DisplayName("should call saveAll on the repository")
        void shouldCallSaveAll() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());
            when(checkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            dueDiligenceService.initializeChecksForCampaign(campaignId);

            verify(checkRepository).saveAll(checksCaptor.capture());
            assertThat(checksCaptor.getValue()).hasSize(144);
        }

        @Test
        @DisplayName("should throw BusinessRuleException if checks already exist")
        void shouldThrowIfAlreadyInitialized() {
            DueDiligenceCheck existing = DueDiligenceCheck.builder()
                    .campaignId(campaignId)
                    .category("LEGAL")
                    .checkName("Existing check")
                    .build();
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of(existing));

            assertThatThrownBy(() -> dueDiligenceService.initializeChecksForCampaign(campaignId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already initialized");
        }
    }

    @Nested
    @DisplayName("updateCheck")
    class UpdateCheck {

        @Test
        @DisplayName("should update check status, notes, reviewer, and timestamp")
        void shouldUpdateCheckFields() {
            DueDiligenceCheck check = DueDiligenceCheck.builder()
                    .campaignId(campaignId)
                    .category("LEGAL")
                    .checkName("Registration")
                    .status(DDCheckStatus.PENDING)
                    .build();
            check.setId(UUID.randomUUID());

            when(checkRepository.findById(check.getId())).thenReturn(Optional.of(check));
            when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceCheck result = dueDiligenceService.updateCheck(
                    check.getId(), DDCheckStatus.PASSED, "All verified", reviewerId);

            assertThat(result.getStatus()).isEqualTo(DDCheckStatus.PASSED);
            assertThat(result.getNotes()).isEqualTo("All verified");
            assertThat(result.getCheckedBy()).isEqualTo(reviewerId);
            assertThat(result.getCheckedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for non-existent check")
        void shouldThrowWhenCheckNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(checkRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dueDiligenceService.updateCheck(
                    unknownId, DDCheckStatus.PASSED, "notes", reviewerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("DueDiligenceCheck");
        }

        @Test
        @DisplayName("should persist the updated check via save")
        void shouldPersistUpdatedCheck() {
            DueDiligenceCheck check = DueDiligenceCheck.builder()
                    .campaignId(campaignId)
                    .category("FINANCIAL")
                    .checkName("Revenue")
                    .status(DDCheckStatus.PENDING)
                    .build();
            check.setId(UUID.randomUUID());

            when(checkRepository.findById(check.getId())).thenReturn(Optional.of(check));
            when(checkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dueDiligenceService.updateCheck(check.getId(), DDCheckStatus.FAILED, "Missing data", reviewerId);

            verify(checkRepository).save(check);
        }
    }

    @Nested
    @DisplayName("generateReport")
    class GenerateReport {

        private List<DueDiligenceCheck> buildChecksForReport(int passed, int failed, int na) {
            List<DueDiligenceCheck> checks = new ArrayList<>();
            int sortOrder = 0;
            for (int i = 0; i < passed; i++) {
                DueDiligenceCheck c = DueDiligenceCheck.builder()
                        .campaignId(campaignId)
                        .category("LEGAL")
                        .checkName("Passed-" + i)
                        .status(DDCheckStatus.PASSED)
                        .weight(BigDecimal.valueOf(1.0))
                        .sortOrder(sortOrder++)
                        .build();
                c.setId(UUID.randomUUID());
                checks.add(c);
            }
            for (int i = 0; i < failed; i++) {
                DueDiligenceCheck c = DueDiligenceCheck.builder()
                        .campaignId(campaignId)
                        .category("FINANCIAL")
                        .checkName("Failed-" + i)
                        .status(DDCheckStatus.FAILED)
                        .weight(BigDecimal.valueOf(1.0))
                        .sortOrder(sortOrder++)
                        .build();
                c.setId(UUID.randomUUID());
                checks.add(c);
            }
            for (int i = 0; i < na; i++) {
                DueDiligenceCheck c = DueDiligenceCheck.builder()
                        .campaignId(campaignId)
                        .category("OPERATIONAL")
                        .checkName("NA-" + i)
                        .status(DDCheckStatus.NOT_APPLICABLE)
                        .weight(BigDecimal.valueOf(1.0))
                        .sortOrder(sortOrder++)
                        .build();
                c.setId(UUID.randomUUID());
                checks.add(c);
            }
            return checks;
        }

        @Test
        @DisplayName("should throw BusinessRuleException when no checks exist")
        void shouldThrowWhenNoChecks() {
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> dueDiligenceService.generateReport(campaignId, adminId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No due diligence checks found");
        }

        @Test
        @DisplayName("should throw BusinessRuleException when checks are still PENDING")
        void shouldThrowWhenPendingChecksExist() {
            List<DueDiligenceCheck> checks = List.of(
                    DueDiligenceCheck.builder()
                            .campaignId(campaignId)
                            .category("LEGAL")
                            .checkName("Pending check")
                            .status(DDCheckStatus.PENDING)
                            .weight(BigDecimal.ONE)
                            .build());

            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId))
                    .thenReturn(checks);

            assertThatThrownBy(() -> dueDiligenceService.generateReport(campaignId, adminId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("should calculate LOW risk and APPROVE recommendation for high score")
        void shouldCalculateLowRiskForHighScore() {
            // 95 passed, 3 failed, 2 NA -> score = 95/(95+3)*100 = 96.94%
            List<DueDiligenceCheck> checks = buildChecksForReport(95, 3, 2);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getRiskLevel()).isEqualTo("LOW");
            assertThat(report.getRecommendation()).isEqualTo("APPROVE");
            assertThat(report.getOverallScore()).isGreaterThan(BigDecimal.valueOf(85));
            assertThat(report.getTotalChecks()).isEqualTo(100);
            assertThat(report.getPassedChecks()).isEqualTo(95);
            assertThat(report.getFailedChecks()).isEqualTo(3);
            assertThat(report.getNaChecks()).isEqualTo(2);
        }

        @Test
        @DisplayName("should calculate MEDIUM risk and CONDITIONAL_APPROVE for medium score")
        void shouldCalculateMediumRisk() {
            // 75 passed, 15 failed, 10 NA -> score = 75/90*100 = 83.33%
            // score >= 70 -> MEDIUM risk, score >= 60 and failed <= 15 -> CONDITIONAL_APPROVE
            List<DueDiligenceCheck> checks = buildChecksForReport(75, 15, 10);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getRiskLevel()).isEqualTo("MEDIUM");
            assertThat(report.getRecommendation()).isEqualTo("CONDITIONAL_APPROVE");
        }

        @Test
        @DisplayName("should calculate HIGH risk and REJECT for low score with many failures")
        void shouldCalculateHighRiskWithManyFailures() {
            // 40 passed, 50 failed, 10 NA -> score = 40/90*100 = 44.44%
            List<DueDiligenceCheck> checks = buildChecksForReport(40, 50, 10);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getRiskLevel()).isEqualTo("CRITICAL");
            assertThat(report.getRecommendation()).isEqualTo("REJECT");
        }

        @Test
        @DisplayName("should calculate CRITICAL risk for very low score")
        void shouldCalculateCriticalRisk() {
            // 20 passed, 70 failed, 10 NA -> score = 20/90*100 = 22.22%
            List<DueDiligenceCheck> checks = buildChecksForReport(20, 70, 10);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getRiskLevel()).isEqualTo("CRITICAL");
            assertThat(report.getRecommendation()).isEqualTo("REJECT");
            assertThat(report.getOverallScore()).isLessThan(BigDecimal.valueOf(50));
        }

        @Test
        @DisplayName("should include category breakdown in summary")
        void shouldIncludeCategoryBreakdownInSummary() {
            List<DueDiligenceCheck> checks = buildChecksForReport(90, 5, 5);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getSummary()).contains("Due Diligence Report Summary");
            assertThat(report.getSummary()).contains("Category Breakdown");
            assertThat(report.getSummary()).contains("pass rate");
        }

        @Test
        @DisplayName("should list failed checks in summary")
        void shouldListFailedChecksInSummary() {
            List<DueDiligenceCheck> checks = buildChecksForReport(80, 10, 10);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getSummary()).contains("Failed Checks");
            assertThat(report.getSummary()).contains("FINANCIAL");
        }

        @Test
        @DisplayName("should set generatedBy and generatedAt on report")
        void shouldSetGeneratedFields() {
            List<DueDiligenceCheck> checks = buildChecksForReport(90, 5, 5);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Instant before = Instant.now();
            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getGeneratedBy()).isEqualTo(adminId);
            assertThat(report.getGeneratedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("should update existing report if one already exists")
        void shouldUpdateExistingReport() {
            List<DueDiligenceCheck> checks = buildChecksForReport(90, 5, 5);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);

            DueDiligenceReport existingReport = DueDiligenceReport.builder()
                    .campaignId(campaignId)
                    .totalChecks(100)
                    .overallScore(BigDecimal.valueOf(50))
                    .build();
            existingReport.setId(UUID.randomUUID());
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.of(existingReport));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getId()).isEqualTo(existingReport.getId());
            assertThat(report.getPassedChecks()).isEqualTo(90);
            verify(reportRepository).save(existingReport);
        }

        @Test
        @DisplayName("should handle all NOT_APPLICABLE checks gracefully")
        void shouldHandleAllNAChecks() {
            List<DueDiligenceCheck> checks = buildChecksForReport(0, 0, 10);
            when(checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)).thenReturn(checks);
            when(reportRepository.findByCampaignId(campaignId)).thenReturn(Optional.empty());
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DueDiligenceReport report = dueDiligenceService.generateReport(campaignId, adminId);

            assertThat(report.getOverallScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(report.getNaChecks()).isEqualTo(10);
        }
    }
}
