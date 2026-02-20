package com.keza.marketplace.application.usecase;

import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ForbiddenException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.marketplace.application.dto.CreateListingRequest;
import com.keza.marketplace.application.dto.ListingResponse;
import com.keza.marketplace.application.dto.MarketplaceTransactionResponse;
import com.keza.marketplace.domain.event.ListingCreatedEvent;
import com.keza.marketplace.domain.model.ListingStatus;
import com.keza.marketplace.domain.model.MarketplaceListing;
import com.keza.marketplace.domain.model.MarketplaceTransaction;
import com.keza.marketplace.domain.port.out.MarketplaceListingRepository;
import com.keza.marketplace.domain.port.out.MarketplaceTransactionRepository;
import com.keza.marketplace.domain.service.MarketplaceService;
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
import org.springframework.context.ApplicationEventPublisher;

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
@DisplayName("MarketplaceUseCase")
class MarketplaceUseCaseTest {

    @Mock
    private MarketplaceListingRepository listingRepository;

    @Mock
    private MarketplaceTransactionRepository transactionRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private MarketplaceService marketplaceService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MarketplaceUseCase marketplaceUseCase;

    @Captor
    private ArgumentCaptor<MarketplaceListing> listingCaptor;

    @Captor
    private ArgumentCaptor<MarketplaceTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<ListingCreatedEvent> eventCaptor;

    private UUID sellerId;
    private UUID buyerId;
    private UUID investmentId;
    private UUID campaignId;
    private Investment investment;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        investmentId = UUID.randomUUID();
        campaignId = UUID.randomUUID();

        investment = Investment.builder()
                .investorId(sellerId)
                .campaignId(campaignId)
                .amount(new BigDecimal("100000"))
                .shares(1000)
                .sharePrice(new BigDecimal("100"))
                .status(InvestmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        investment.setId(investmentId);
    }

    @Nested
    @DisplayName("createListing")
    class CreateListing {

        private CreateListingRequest createValidRequest() {
            return CreateListingRequest.builder()
                    .investmentId(investmentId)
                    .sharesListed(500)
                    .pricePerShare(new BigDecimal("150"))
                    .companyConsent(true)
                    .build();
        }

        @Test
        @DisplayName("should create listing successfully for valid request")
        void shouldCreateListingSuccessfully() {
            CreateListingRequest request = createValidRequest();
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(investment));
            doNothing().when(marketplaceService).validateListing(investment, true);
            when(listingRepository.existsByInvestmentIdAndStatusIn(eq(investmentId), any())).thenReturn(false);
            when(marketplaceService.calculateSellerFee(any())).thenReturn(new BigDecimal("1500.00"));
            when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> {
                MarketplaceListing listing = invocation.getArgument(0);
                listing.setId(UUID.randomUUID());
                listing.setCreatedAt(Instant.now());
                listing.setUpdatedAt(Instant.now());
                return listing;
            });
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            ListingResponse response = marketplaceUseCase.createListing(sellerId, request);

            assertThat(response).isNotNull();
            assertThat(response.getSellerId()).isEqualTo(sellerId);
            assertThat(response.getSharesListed()).isEqualTo(500);
            assertThat(response.getStatus()).isEqualTo("ACTIVE");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            ListingCreatedEvent event = eventCaptor.getValue();
            assertThat(event.sellerId()).isEqualTo(sellerId);
            assertThat(event.sharesListed()).isEqualTo(500);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when investment not found")
        void shouldThrowWhenInvestmentNotFound() {
            CreateListingRequest request = createValidRequest();
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> marketplaceUseCase.createListing(sellerId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Investment");
        }

        @Test
        @DisplayName("should throw ForbiddenException when seller does not own the investment")
        void shouldThrowWhenNotOwner() {
            UUID otherUser = UUID.randomUUID();
            CreateListingRequest request = createValidRequest();
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(investment));

            assertThatThrownBy(() -> marketplaceUseCase.createListing(otherUser, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("only list your own investments");
        }

        @Test
        @DisplayName("should throw BusinessRuleException when listing more shares than owned")
        void shouldThrowWhenInsufficientShares() {
            CreateListingRequest request = CreateListingRequest.builder()
                    .investmentId(investmentId)
                    .sharesListed(2000) // More than the 1000 owned
                    .pricePerShare(new BigDecimal("150"))
                    .companyConsent(true)
                    .build();

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(investment));
            doNothing().when(marketplaceService).validateListing(investment, true);

            assertThatThrownBy(() -> marketplaceUseCase.createListing(sellerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot list 2000 shares")
                    .hasMessageContaining("only own 1000");
        }

        @Test
        @DisplayName("should throw BusinessRuleException when active listing already exists")
        void shouldThrowWhenDuplicateListing() {
            CreateListingRequest request = createValidRequest();
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(investment));
            doNothing().when(marketplaceService).validateListing(investment, true);
            when(listingRepository.existsByInvestmentIdAndStatusIn(eq(investmentId), any())).thenReturn(true);

            assertThatThrownBy(() -> marketplaceUseCase.createListing(sellerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("active listing already exists");
        }

        @Test
        @DisplayName("should delegate validation to MarketplaceService")
        void shouldDelegateValidation() {
            CreateListingRequest request = createValidRequest();
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(investment));
            doThrow(new BusinessRuleException("HOLDING_PERIOD_NOT_MET", "too early"))
                    .when(marketplaceService).validateListing(investment, true);

            assertThatThrownBy(() -> marketplaceUseCase.createListing(sellerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("too early");
        }
    }

    @Nested
    @DisplayName("cancelListing")
    class CancelListing {

        @Test
        @DisplayName("should cancel an active listing successfully")
        void shouldCancelActiveListingSuccessfully() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .investmentId(investmentId)
                    .campaignId(campaignId)
                    .sharesListed(500)
                    .pricePerShare(new BigDecimal("150"))
                    .totalPrice(new BigDecimal("75000"))
                    .status(ListingStatus.ACTIVE)
                    .sellerFee(new BigDecimal("1500"))
                    .build();
            listing.setId(listingId);
            listing.setCreatedAt(Instant.now());
            listing.setUpdatedAt(Instant.now());

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
            when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> inv.getArgument(0));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            ListingResponse response = marketplaceUseCase.cancelListing(listingId, sellerId);

            assertThat(response.getStatus()).isEqualTo("CANCELLED");
            verify(listingRepository).save(listingCaptor.capture());
            assertThat(listingCaptor.getValue().getStatus()).isEqualTo(ListingStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when listing not found")
        void shouldThrowWhenListingNotFound() {
            UUID listingId = UUID.randomUUID();
            when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> marketplaceUseCase.cancelListing(listingId, sellerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when cancelling another user's listing")
        void shouldThrowWhenNotOwnerOfListing() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(UUID.randomUUID()) // different seller
                    .status(ListingStatus.ACTIVE)
                    .build();
            listing.setId(listingId);

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> marketplaceUseCase.cancelListing(listingId, sellerId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("only cancel your own");
        }

        @Test
        @DisplayName("should throw BusinessRuleException when cancelling non-active listing")
        void shouldThrowWhenCancellingNonActiveListing() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .status(ListingStatus.SOLD)
                    .build();
            listing.setId(listingId);

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> marketplaceUseCase.cancelListing(listingId, sellerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Only active listings can be cancelled");
        }
    }

    @Nested
    @DisplayName("buyListing")
    class BuyListing {

        @Test
        @DisplayName("should successfully purchase an active listing")
        void shouldPurchaseSuccessfully() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .investmentId(investmentId)
                    .campaignId(campaignId)
                    .sharesListed(500)
                    .pricePerShare(new BigDecimal("150"))
                    .totalPrice(new BigDecimal("75000"))
                    .status(ListingStatus.ACTIVE)
                    .sellerFee(new BigDecimal("1500"))
                    .build();
            listing.setId(listingId);

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
            when(transactionRepository.save(any(MarketplaceTransaction.class))).thenAnswer(inv -> {
                MarketplaceTransaction tx = inv.getArgument(0);
                tx.setId(UUID.randomUUID());
                tx.setCreatedAt(Instant.now());
                tx.setUpdatedAt(Instant.now());
                return tx;
            });
            when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> inv.getArgument(0));

            MarketplaceTransactionResponse response = marketplaceUseCase.buyListing(listingId, buyerId);

            assertThat(response).isNotNull();
            assertThat(response.getBuyerId()).isEqualTo(buyerId);
            assertThat(response.getSellerId()).isEqualTo(sellerId);
            assertThat(response.getShares()).isEqualTo(500);
            assertThat(response.getStatus()).isEqualTo("COMPLETED");

            // Verify listing was marked as SOLD
            verify(listingRepository, atLeastOnce()).save(listingCaptor.capture());
            MarketplaceListing savedListing = listingCaptor.getAllValues().stream()
                    .filter(l -> l.getStatus() == ListingStatus.SOLD)
                    .findFirst()
                    .orElse(null);
            assertThat(savedListing).isNotNull();
            assertThat(savedListing.getBuyerId()).isEqualTo(buyerId);
            assertThat(savedListing.getSoldAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw when listing is not active")
        void shouldThrowWhenListingNotActive() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .status(ListingStatus.SOLD)
                    .build();
            listing.setId(listingId);

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> marketplaceUseCase.buyListing(listingId, buyerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no longer available");
        }

        @Test
        @DisplayName("should throw when buyer is the seller (self-purchase)")
        void shouldThrowOnSelfPurchase() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .status(ListingStatus.ACTIVE)
                    .build();
            listing.setId(listingId);

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> marketplaceUseCase.buyListing(listingId, sellerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot purchase your own listing");
        }

        @Test
        @DisplayName("should throw and expire listing when listing has expired")
        void shouldThrowWhenListingExpired() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .status(ListingStatus.ACTIVE)
                    .expiresAt(Instant.now().minusSeconds(3600)) // expired 1 hour ago
                    .build();
            listing.setId(listingId);

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
            when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> marketplaceUseCase.buyListing(listingId, buyerId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("expired");

            verify(listingRepository).save(listingCaptor.capture());
            assertThat(listingCaptor.getValue().getStatus()).isEqualTo(ListingStatus.EXPIRED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when listing not found")
        void shouldThrowWhenListingNotFound() {
            UUID listingId = UUID.randomUUID();
            when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> marketplaceUseCase.buyListing(listingId, buyerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should calculate correct net amount after fee deduction")
        void shouldCalculateCorrectNetAmount() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .investmentId(investmentId)
                    .campaignId(campaignId)
                    .sharesListed(100)
                    .pricePerShare(new BigDecimal("100"))
                    .totalPrice(new BigDecimal("10000"))
                    .status(ListingStatus.ACTIVE)
                    .sellerFee(new BigDecimal("200"))
                    .build();
            listing.setId(listingId);

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
            when(transactionRepository.save(any(MarketplaceTransaction.class))).thenAnswer(inv -> {
                MarketplaceTransaction tx = inv.getArgument(0);
                tx.setId(UUID.randomUUID());
                tx.setCreatedAt(Instant.now());
                tx.setUpdatedAt(Instant.now());
                return tx;
            });
            when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> inv.getArgument(0));

            MarketplaceTransactionResponse response = marketplaceUseCase.buyListing(listingId, buyerId);

            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(response.getSellerFee()).isEqualByComparingTo(new BigDecimal("200"));
            // netAmount = 10000 - 200 = 9800, rounded to 2 decimal places
            assertThat(response.getNetAmount()).isEqualByComparingTo(new BigDecimal("9800"));
        }
    }

    @Nested
    @DisplayName("getListing")
    class GetListing {

        @Test
        @DisplayName("should return listing response when found")
        void shouldReturnListingWhenFound() {
            UUID listingId = UUID.randomUUID();
            MarketplaceListing listing = MarketplaceListing.builder()
                    .sellerId(sellerId)
                    .investmentId(investmentId)
                    .campaignId(campaignId)
                    .sharesListed(500)
                    .pricePerShare(new BigDecimal("150"))
                    .totalPrice(new BigDecimal("75000"))
                    .status(ListingStatus.ACTIVE)
                    .sellerFee(new BigDecimal("1500"))
                    .build();
            listing.setId(listingId);
            listing.setCreatedAt(Instant.now());
            listing.setUpdatedAt(Instant.now());

            when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            ListingResponse response = marketplaceUseCase.getListing(listingId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(listingId);
            assertThat(response.getSharesListed()).isEqualTo(500);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when listing not found")
        void shouldThrowWhenNotFound() {
            UUID listingId = UUID.randomUUID();
            when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> marketplaceUseCase.getListing(listingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
