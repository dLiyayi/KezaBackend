package com.keza.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.keza.admin.application.dto.DueDiligenceCheckResponse;
import com.keza.admin.application.dto.DueDiligenceReportResponse;
import com.keza.admin.application.dto.UpdateCheckRequest;
import com.keza.admin.application.usecase.DueDiligenceUseCase;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DueDiligenceController")
class DueDiligenceControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private DueDiligenceUseCase dueDiligenceUseCase;

    @InjectMocks
    private DueDiligenceController dueDiligenceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dueDiligenceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/due-diligence/campaigns/{campaignId}/checks")
    class GetChecksForCampaign {

        @Test
        @DisplayName("should return 200 with list of checks")
        void shouldReturnChecks() throws Exception {
            UUID campaignId = UUID.randomUUID();
            List<DueDiligenceCheckResponse> checks = List.of(
                    DueDiligenceCheckResponse.builder()
                            .id(UUID.randomUUID())
                            .campaignId(campaignId)
                            .category("LEGAL")
                            .checkName("Business Registration")
                            .status("PENDING")
                            .weight(BigDecimal.valueOf(1.5))
                            .sortOrder(0)
                            .createdAt(Instant.now())
                            .build(),
                    DueDiligenceCheckResponse.builder()
                            .id(UUID.randomUUID())
                            .campaignId(campaignId)
                            .category("FINANCIAL")
                            .checkName("Revenue Growth")
                            .status("PASSED")
                            .weight(BigDecimal.valueOf(1.4))
                            .sortOrder(1)
                            .createdAt(Instant.now())
                            .build());

            when(dueDiligenceUseCase.getChecksForCampaign(campaignId)).thenReturn(checks);

            mockMvc.perform(get("/api/v1/admin/due-diligence/campaigns/{campaignId}/checks", campaignId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].category").value("LEGAL"))
                    .andExpect(jsonPath("$.data[0].checkName").value("Business Registration"))
                    .andExpect(jsonPath("$.data[1].status").value("PASSED"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no checks exist yet")
        void shouldReturnEmptyListForNewCampaign() throws Exception {
            UUID campaignId = UUID.randomUUID();
            when(dueDiligenceUseCase.getChecksForCampaign(campaignId)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/due-diligence/campaigns/{campaignId}/checks", campaignId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/due-diligence/checks/{checkId}")
    class UpdateCheck {

        @Test
        @DisplayName("should return 200 with updated check")
        void shouldUpdateCheck() throws Exception {
            UUID checkId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            UpdateCheckRequest request = UpdateCheckRequest.builder()
                    .status("PASSED")
                    .notes("Verified")
                    .build();

            DueDiligenceCheckResponse response = DueDiligenceCheckResponse.builder()
                    .id(checkId)
                    .campaignId(UUID.randomUUID())
                    .category("LEGAL")
                    .checkName("Registration Check")
                    .status("PASSED")
                    .notes("Verified")
                    .checkedBy(reviewerId)
                    .checkedAt(Instant.now())
                    .build();

            when(dueDiligenceUseCase.updateCheck(eq(checkId), any(UpdateCheckRequest.class), eq(reviewerId)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/admin/due-diligence/checks/{checkId}", checkId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(new TestingAuthenticationToken(reviewerId, null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Check updated successfully"))
                    .andExpect(jsonPath("$.data.status").value("PASSED"))
                    .andExpect(jsonPath("$.data.notes").value("Verified"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/due-diligence/campaigns/{campaignId}/report")
    class GenerateReport {

        @Test
        @DisplayName("should return 200 with generated report")
        void shouldGenerateReport() throws Exception {
            UUID campaignId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            DueDiligenceReportResponse response = DueDiligenceReportResponse.builder()
                    .id(UUID.randomUUID())
                    .campaignId(campaignId)
                    .totalChecks(144)
                    .passedChecks(130)
                    .failedChecks(4)
                    .naChecks(10)
                    .overallScore(BigDecimal.valueOf(97.01))
                    .riskLevel("LOW")
                    .recommendation("APPROVE")
                    .summary("Report summary")
                    .generatedBy(adminId)
                    .generatedAt(Instant.now())
                    .build();

            when(dueDiligenceUseCase.generateReport(eq(campaignId), eq(adminId)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/admin/due-diligence/campaigns/{campaignId}/report", campaignId)
                            .principal(new TestingAuthenticationToken(adminId, null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Report generated successfully"))
                    .andExpect(jsonPath("$.data.riskLevel").value("LOW"))
                    .andExpect(jsonPath("$.data.recommendation").value("APPROVE"))
                    .andExpect(jsonPath("$.data.totalChecks").value(144));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/due-diligence/campaigns/{campaignId}/report")
    class GetReport {

        @Test
        @DisplayName("should return 200 with existing report")
        void shouldReturnReport() throws Exception {
            UUID campaignId = UUID.randomUUID();
            DueDiligenceReportResponse response = DueDiligenceReportResponse.builder()
                    .id(UUID.randomUUID())
                    .campaignId(campaignId)
                    .totalChecks(144)
                    .overallScore(BigDecimal.valueOf(85.5))
                    .riskLevel("LOW")
                    .recommendation("APPROVE")
                    .build();

            when(dueDiligenceUseCase.getReport(campaignId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/admin/due-diligence/campaigns/{campaignId}/report", campaignId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.riskLevel").value("LOW"));
        }

        @Test
        @DisplayName("should return error when report does not exist")
        void shouldReturnErrorWhenReportNotFound() throws Exception {
            UUID campaignId = UUID.randomUUID();
            when(dueDiligenceUseCase.getReport(campaignId))
                    .thenThrow(new ResourceNotFoundException("DueDiligenceReport", "campaignId", campaignId.toString()));

            mockMvc.perform(get("/api/v1/admin/due-diligence/campaigns/{campaignId}/report", campaignId))
                    .andExpect(status().is4xxClientError());
        }
    }
}
