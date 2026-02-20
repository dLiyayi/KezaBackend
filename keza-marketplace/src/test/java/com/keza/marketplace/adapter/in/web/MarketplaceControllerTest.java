package com.keza.marketplace.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.keza.infrastructure.handler.GlobalExceptionHandler;
import com.keza.marketplace.application.dto.CreateListingRequest;
import com.keza.marketplace.application.dto.ListingResponse;
import com.keza.marketplace.application.dto.MarketplaceTransactionResponse;
import com.keza.marketplace.application.usecase.MarketplaceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketplaceController")
class MarketplaceControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private MarketplaceUseCase marketplaceUseCase;

    @InjectMocks
    private MarketplaceController marketplaceController;

    private UUID userId;
    private UUID listingId;
    private UUID investmentId;
    private UUID campaignId;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(marketplaceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        userId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        investmentId = UUID.randomUUID();
        campaignId = UUID.randomUUID();
        authentication = new TestingAuthenticationToken(userId.toString(), null);
    }

    private ListingResponse buildListingResponse() {
        return ListingResponse.builder()
                .id(listingId)
                .sellerId(userId)
                .investmentId(investmentId)
                .campaignId(campaignId)
                .sharesListed(500)
                .pricePerShare(new BigDecimal("150"))
                .totalPrice(new BigDecimal("75000"))
                .status("ACTIVE")
                .companyConsent(true)
                .sellerFee(new BigDecimal("1500"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/marketplace/listings")
    class CreateListingEndpoint {

        @Test
        @DisplayName("should return 201 Created when listing is created successfully")
        void shouldReturn201WhenCreated() throws Exception {
            CreateListingRequest request = CreateListingRequest.builder()
                    .investmentId(investmentId)
                    .sharesListed(500)
                    .pricePerShare(new BigDecimal("150"))
                    .companyConsent(true)
                    .build();

            when(marketplaceUseCase.createListing(eq(userId), any(CreateListingRequest.class)))
                    .thenReturn(buildListingResponse());

            mockMvc.perform(post("/api/v1/marketplace/listings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(authentication))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sharesListed").value(500));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/marketplace/listings/{id}")
    class GetListingEndpoint {

        @Test
        @DisplayName("should return 200 OK with listing details")
        void shouldReturn200WithListing() throws Exception {
            when(marketplaceUseCase.getListing(listingId)).thenReturn(buildListingResponse());

            mockMvc.perform(get("/api/v1/marketplace/listings/{id}", listingId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(listingId.toString()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/marketplace/listings/{id}")
    class CancelListingEndpoint {

        @Test
        @DisplayName("should return 200 OK when listing is cancelled")
        void shouldReturn200WhenCancelled() throws Exception {
            ListingResponse cancelledResponse = buildListingResponse();
            cancelledResponse.setStatus("CANCELLED");

            when(marketplaceUseCase.cancelListing(eq(listingId), eq(userId))).thenReturn(cancelledResponse);

            mockMvc.perform(delete("/api/v1/marketplace/listings/{id}", listingId)
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/marketplace/listings/{id}/buy")
    class BuyListingEndpoint {

        @Test
        @DisplayName("should return 200 OK when purchase completes")
        void shouldReturn200WhenPurchased() throws Exception {
            MarketplaceTransactionResponse txResponse = MarketplaceTransactionResponse.builder()
                    .id(UUID.randomUUID())
                    .listingId(listingId)
                    .buyerId(userId)
                    .sellerId(UUID.randomUUID())
                    .shares(500)
                    .pricePerShare(new BigDecimal("150"))
                    .totalAmount(new BigDecimal("75000"))
                    .sellerFee(new BigDecimal("1500"))
                    .netAmount(new BigDecimal("73500"))
                    .status("COMPLETED")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(marketplaceUseCase.buyListing(eq(listingId), eq(userId))).thenReturn(txResponse);

            mockMvc.perform(post("/api/v1/marketplace/listings/{id}/buy", listingId)
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/marketplace/listings")
    class GetActiveListingsEndpoint {

        @Test
        @DisplayName("should return 200 OK with paginated active listings")
        void shouldReturn200WithActiveListings() throws Exception {
            when(marketplaceUseCase.getActiveListings(any(), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            mockMvc.perform(get("/api/v1/marketplace/listings")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
