package com.keza.investment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.handler.GlobalExceptionHandler;
import com.keza.investment.application.dto.CreateInvestmentRequest;
import com.keza.investment.application.dto.InvestmentResponse;
import com.keza.investment.application.dto.PortfolioResponse;
import com.keza.investment.application.usecase.InvestmentUseCase;
import com.keza.investment.application.usecase.PortfolioUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("InvestmentController")
class InvestmentControllerTest {

    private MockMvc mockMvc;
    private InvestmentUseCase investmentUseCase;
    private PortfolioUseCase portfolioUseCase;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        investmentUseCase = mock(InvestmentUseCase.class);
        portfolioUseCase = mock(PortfolioUseCase.class);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // for Instant serialization

        userId = UUID.randomUUID();

        InvestmentController controller = new InvestmentController(investmentUseCase, portfolioUseCase);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication mockAuthentication() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId);
        return auth;
    }

    private InvestmentResponse sampleResponse() {
        return InvestmentResponse.builder()
                .id(UUID.randomUUID())
                .investorId(userId)
                .campaignId(UUID.randomUUID())
                .amount(new BigDecimal("10000"))
                .shares(100)
                .sharePrice(new BigDecimal("100"))
                .status(InvestmentStatus.PENDING.name())
                .paymentMethod("MPESA")
                .coolingOffExpiresAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/investments")
    class CreateInvestment {

        @Test
        @DisplayName("should return 201 CREATED with investment response")
        void shouldCreateInvestmentSuccessfully() throws Exception {
            InvestmentResponse response = sampleResponse();
            when(investmentUseCase.createInvestment(eq(userId), any(CreateInvestmentRequest.class)))
                    .thenReturn(response);

            UUID campaignId = UUID.randomUUID();
            String requestJson = objectMapper.writeValueAsString(
                    CreateInvestmentRequest.builder()
                            .campaignId(campaignId)
                            .amount(new BigDecimal("10000"))
                            .paymentMethod("MPESA")
                            .build()
            );

            mockMvc.perform(post("/api/v1/investments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson)
                            .principal(mockAuthentication()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Investment created successfully"))
                    .andExpect(jsonPath("$.data.shares").value(100))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 422 when business rule exception is thrown")
        void shouldReturn422OnBusinessRuleViolation() throws Exception {
            when(investmentUseCase.createInvestment(eq(userId), any(CreateInvestmentRequest.class)))
                    .thenThrow(new BusinessRuleException("KYC_NOT_APPROVED", "KYC not approved"));

            String requestJson = objectMapper.writeValueAsString(
                    CreateInvestmentRequest.builder()
                            .campaignId(UUID.randomUUID())
                            .amount(new BigDecimal("10000"))
                            .paymentMethod("MPESA")
                            .build()
            );

            mockMvc.perform(post("/api/v1/investments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson)
                            .principal(mockAuthentication()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("KYC_NOT_APPROVED"));
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenCampaignNotFound() throws Exception {
            UUID campaignId = UUID.randomUUID();
            when(investmentUseCase.createInvestment(eq(userId), any(CreateInvestmentRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Campaign", campaignId));

            String requestJson = objectMapper.writeValueAsString(
                    CreateInvestmentRequest.builder()
                            .campaignId(campaignId)
                            .amount(new BigDecimal("10000"))
                            .paymentMethod("MPESA")
                            .build()
            );

            mockMvc.perform(post("/api/v1/investments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson)
                            .principal(mockAuthentication()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/investments/{id}")
    class GetInvestment {

        @Test
        @DisplayName("should return 200 with investment details")
        void shouldReturnInvestment() throws Exception {
            UUID investmentId = UUID.randomUUID();
            InvestmentResponse response = sampleResponse();
            when(investmentUseCase.getInvestment(investmentId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/investments/{id}", investmentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 404 when investment not found")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID investmentId = UUID.randomUUID();
            when(investmentUseCase.getInvestment(investmentId))
                    .thenThrow(new ResourceNotFoundException("Investment", investmentId));

            mockMvc.perform(get("/api/v1/investments/{id}", investmentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/investments")
    class GetUserInvestments {

        @Test
        @DisplayName("should return 200 with paginated investment list")
        void shouldReturnPaginatedInvestments() throws Exception {
            InvestmentResponse response = sampleResponse();
            Page<InvestmentResponse> page = new PageImpl<>(List.of(response));
            when(investmentUseCase.getUserInvestments(eq(userId), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/investments")
                            .param("page", "0")
                            .param("size", "20")
                            .principal(mockAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/investments/{id}/cancel")
    class CancelInvestment {

        @Test
        @DisplayName("should return 200 with cancelled investment")
        void shouldCancelSuccessfully() throws Exception {
            UUID investmentId = UUID.randomUUID();
            InvestmentResponse response = sampleResponse();
            response.setStatus("CANCELLED");
            when(investmentUseCase.cancelInvestment(investmentId, userId)).thenReturn(response);

            mockMvc.perform(post("/api/v1/investments/{id}/cancel", investmentId)
                            .principal(mockAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Investment cancelled successfully"))
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("should return 422 when cooling-off period expired")
        void shouldReturn422WhenCoolingOffExpired() throws Exception {
            UUID investmentId = UUID.randomUUID();
            when(investmentUseCase.cancelInvestment(investmentId, userId))
                    .thenThrow(new BusinessRuleException("COOLING_OFF_EXPIRED",
                            "The 48-hour cooling-off period has expired."));

            mockMvc.perform(post("/api/v1/investments/{id}/cancel", investmentId)
                            .principal(mockAuthentication()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code").value("COOLING_OFF_EXPIRED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/investments/portfolio")
    class GetPortfolio {

        @Test
        @DisplayName("should return 200 with portfolio data")
        void shouldReturnPortfolio() throws Exception {
            PortfolioResponse portfolioResponse = PortfolioResponse.builder()
                    .totalInvested(new BigDecimal("50000"))
                    .activeInvestments(3)
                    .sectorDistribution(Map.of("FinTech", new BigDecimal("60.00"), "AgriTech", new BigDecimal("40.00")))
                    .investments(Collections.emptyList())
                    .build();
            when(portfolioUseCase.getPortfolio(userId)).thenReturn(portfolioResponse);

            mockMvc.perform(get("/api/v1/investments/portfolio")
                            .principal(mockAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalInvested").value(50000))
                    .andExpect(jsonPath("$.data.activeInvestments").value(3))
                    .andExpect(jsonPath("$.data.sectorDistribution.FinTech").value(60.00));
        }
    }
}
