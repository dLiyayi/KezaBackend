package com.keza.campaign.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.keza.campaign.application.dto.CampaignRequest;
import com.keza.campaign.application.dto.CampaignResponse;
import com.keza.campaign.application.dto.CampaignSearchCriteria;
import com.keza.campaign.application.usecase.CampaignUseCase;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.OfferingType;
import com.keza.common.exception.BusinessRuleException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignController")
class CampaignControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private CampaignUseCase campaignUseCase;

    @InjectMocks
    private CampaignController campaignController;

    private UUID campaignId;
    private UUID issuerId;
    private Authentication authentication;
    private CampaignResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(campaignController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        campaignId = UUID.randomUUID();
        issuerId = UUID.randomUUID();
        authentication = new TestingAuthenticationToken(issuerId.toString(), null);

        sampleResponse = CampaignResponse.builder()
                .id(campaignId)
                .issuerId(issuerId)
                .title("Green Energy Fund")
                .slug("green-energy-fund-abcd1234")
                .tagline("Invest in a greener future")
                .description("A comprehensive green energy investment opportunity")
                .industry("Renewable Energy")
                .companyName("GreenTech Ltd")
                .offeringType(OfferingType.EQUITY)
                .targetAmount(new BigDecimal("500000"))
                .raisedAmount(BigDecimal.ZERO)
                .sharePrice(new BigDecimal("10.00"))
                .totalShares(50000L)
                .soldShares(0L)
                .minInvestment(new BigDecimal("1000"))
                .maxInvestment(new BigDecimal("50000"))
                .investorCount(0)
                .status(CampaignStatus.DRAFT)
                .wizardStep(1)
                .endDate(Instant.now().plus(90, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/campaigns")
    class CreateDraft {

        @Test
        @DisplayName("should create draft campaign and return 201")
        void shouldCreateDraft() throws Exception {
            CampaignResponse draftResponse = CampaignResponse.builder()
                    .id(campaignId)
                    .issuerId(issuerId)
                    .title("Untitled Campaign")
                    .targetAmount(BigDecimal.ZERO)
                    .status(CampaignStatus.DRAFT)
                    .wizardStep(1)
                    .build();

            when(campaignUseCase.createDraft(any(UUID.class))).thenReturn(draftResponse);

            mockMvc.perform(post("/api/v1/campaigns")
                            .principal(authentication))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(campaignId.toString()))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/campaigns/{id}")
    class GetCampaign {

        @Test
        @DisplayName("should return campaign by id")
        void shouldReturnCampaignById() throws Exception {
            when(campaignUseCase.getCampaign(campaignId)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/campaigns/{id}", campaignId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(campaignId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Green Energy Fund"));
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(campaignUseCase.getCampaign(campaignId))
                    .thenThrow(new ResourceNotFoundException("Campaign", campaignId));

            mockMvc.perform(get("/api/v1/campaigns/{id}", campaignId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/campaigns/slug/{slug}")
    class GetCampaignBySlug {

        @Test
        @DisplayName("should return campaign by slug")
        void shouldReturnCampaignBySlug() throws Exception {
            when(campaignUseCase.getCampaignBySlug("green-energy-fund-abcd1234")).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/campaigns/slug/{slug}", "green-energy-fund-abcd1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.slug").value("green-energy-fund-abcd1234"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/campaigns (search)")
    class SearchCampaigns {

        @Test
        @DisplayName("should return paged campaigns")
        void shouldReturnPagedCampaigns() throws Exception {
            Page<CampaignResponse> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 20), 1);
            when(campaignUseCase.searchCampaigns(any(CampaignSearchCriteria.class), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/campaigns"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/campaigns/{id}/wizard/1")
    class UpdateWizardStep1 {

        @Test
        @DisplayName("should update company info and return 200")
        void shouldUpdateCompanyInfo() throws Exception {
            when(campaignUseCase.updateWizardStep(eq(campaignId), eq(1), any())).thenReturn(sampleResponse);

            CampaignRequest.CompanyInfoRequest request = CampaignRequest.CompanyInfoRequest.builder()
                    .companyName("GreenTech Ltd")
                    .registrationNumber("REG-12345")
                    .website("https://greentech.com")
                    .address("123 Green St")
                    .industry("Renewable Energy")
                    .build();

            mockMvc.perform(put("/api/v1/campaigns/{id}/wizard/1", campaignId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/campaigns/{id}/submit")
    class SubmitForReview {

        @Test
        @DisplayName("should submit campaign for review")
        void shouldSubmitForReview() throws Exception {
            CampaignResponse reviewResponse = CampaignResponse.builder()
                    .id(campaignId)
                    .status(CampaignStatus.REVIEW)
                    .build();

            when(campaignUseCase.submitForReview(eq(campaignId), any(UUID.class))).thenReturn(reviewResponse);

            mockMvc.perform(post("/api/v1/campaigns/{id}/submit", campaignId)
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 422 when campaign is incomplete")
        void shouldReturn422WhenIncomplete() throws Exception {
            when(campaignUseCase.submitForReview(eq(campaignId), any(UUID.class)))
                    .thenThrow(new BusinessRuleException("INCOMPLETE_CAMPAIGN", "Campaign title is required"));

            mockMvc.perform(post("/api/v1/campaigns/{id}/submit", campaignId)
                            .principal(authentication))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(campaignUseCase.submitForReview(eq(campaignId), any(UUID.class)))
                    .thenThrow(new ResourceNotFoundException("Campaign", campaignId));

            mockMvc.perform(post("/api/v1/campaigns/{id}/submit", campaignId)
                            .principal(authentication))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/campaigns/{id}/approve")
    class ApproveCampaign {

        @Test
        @DisplayName("should approve campaign and return 200")
        void shouldApproveCampaign() throws Exception {
            CampaignResponse liveResponse = CampaignResponse.builder()
                    .id(campaignId)
                    .status(CampaignStatus.LIVE)
                    .build();

            when(campaignUseCase.approveCampaign(eq(campaignId), any(UUID.class))).thenReturn(liveResponse);

            mockMvc.perform(post("/api/v1/campaigns/{id}/approve", campaignId)
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/campaigns/{id}/reject")
    class RejectCampaign {

        @Test
        @DisplayName("should reject campaign and return 200")
        void shouldRejectCampaign() throws Exception {
            CampaignResponse draftResponse = CampaignResponse.builder()
                    .id(campaignId)
                    .status(CampaignStatus.DRAFT)
                    .build();

            when(campaignUseCase.rejectCampaign(eq(campaignId), any(String.class), any(UUID.class)))
                    .thenReturn(draftResponse);

            CampaignRequest.RejectRequest request = CampaignRequest.RejectRequest.builder()
                    .reason("Incomplete business plan")
                    .build();

            mockMvc.perform(post("/api/v1/campaigns/{id}/reject", campaignId)
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
