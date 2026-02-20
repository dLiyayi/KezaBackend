package com.keza.marketplace.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ForbiddenException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.common.util.MoneyUtil;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.marketplace.application.dto.*;
import com.keza.marketplace.domain.event.ListingCreatedEvent;
import com.keza.marketplace.domain.model.*;
import com.keza.marketplace.domain.port.out.MarketplaceListingRepository;
import com.keza.marketplace.domain.port.out.MarketplaceTransactionRepository;
import com.keza.marketplace.domain.service.MarketplaceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceUseCase {

    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceTransactionRepository transactionRepository;
    private final InvestmentRepository investmentRepository;
    private final CampaignRepository campaignRepository;
    private final MarketplaceService marketplaceService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ListingResponse createListing(UUID sellerId, CreateListingRequest request) {
        log.info("Creating marketplace listing for seller {} with investment {}", sellerId, request.getInvestmentId());

        Investment investment = investmentRepository.findById(request.getInvestmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Investment", request.getInvestmentId()));

        if (!investment.getInvestorId().equals(sellerId)) {
            throw new ForbiddenException("You can only list your own investments on the marketplace");
        }

        // Validate holding period and company consent
        marketplaceService.validateListing(investment, request.getCompanyConsent());

        // Validate shares available
        if (request.getSharesListed() > investment.getShares()) {
            throw new BusinessRuleException("INSUFFICIENT_SHARES",
                    String.format("Cannot list %d shares. You only own %d shares in this investment.",
                            request.getSharesListed(), investment.getShares()));
        }

        // Check for existing active listing on this investment
        boolean hasActiveListing = listingRepository.existsByInvestmentIdAndStatusIn(
                request.getInvestmentId(), List.of(ListingStatus.ACTIVE));
        if (hasActiveListing) {
            throw new BusinessRuleException("DUPLICATE_LISTING",
                    "An active listing already exists for this investment. Cancel it first before creating a new one.");
        }

        BigDecimal totalPrice = MoneyUtil.round(
                request.getPricePerShare().multiply(BigDecimal.valueOf(request.getSharesListed())));
        BigDecimal sellerFee = marketplaceService.calculateSellerFee(totalPrice);

        MarketplaceListing listing = MarketplaceListing.builder()
                .sellerId(sellerId)
                .investmentId(request.getInvestmentId())
                .campaignId(investment.getCampaignId())
                .sharesListed(request.getSharesListed())
                .pricePerShare(request.getPricePerShare())
                .totalPrice(totalPrice)
                .status(ListingStatus.ACTIVE)
                .companyConsent(request.getCompanyConsent())
                .sellerFee(sellerFee)
                .build();

        listing = listingRepository.save(listing);

        eventPublisher.publishEvent(new ListingCreatedEvent(
                listing.getId(),
                sellerId,
                investment.getCampaignId(),
                investment.getId(),
                request.getSharesListed(),
                request.getPricePerShare(),
                totalPrice));

        log.info("Marketplace listing {} created successfully: {} shares at {} per share, total {}",
                listing.getId(), request.getSharesListed(), request.getPricePerShare(), totalPrice);

        return mapToResponse(listing);
    }

    @Transactional
    public ListingResponse cancelListing(UUID listingId, UUID sellerId) {
        log.info("Cancelling listing {} by seller {}", listingId, sellerId);

        MarketplaceListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceListing", listingId));

        if (!listing.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You can only cancel your own listings");
        }

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessRuleException("INVALID_LISTING_STATUS",
                    "Only active listings can be cancelled. Current status: " + listing.getStatus());
        }

        listing.setStatus(ListingStatus.CANCELLED);
        listing = listingRepository.save(listing);

        log.info("Listing {} cancelled successfully", listingId);
        return mapToResponse(listing);
    }

    @Transactional
    public MarketplaceTransactionResponse buyListing(UUID listingId, UUID buyerId) {
        log.info("Processing purchase of listing {} by buyer {}", listingId, buyerId);

        MarketplaceListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceListing", listingId));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessRuleException("LISTING_NOT_AVAILABLE",
                    "This listing is no longer available. Current status: " + listing.getStatus());
        }

        if (listing.getSellerId().equals(buyerId)) {
            throw new BusinessRuleException("SELF_PURCHASE_NOT_ALLOWED",
                    "You cannot purchase your own listing");
        }

        if (listing.getExpiresAt() != null && Instant.now().isAfter(listing.getExpiresAt())) {
            listing.setStatus(ListingStatus.EXPIRED);
            listingRepository.save(listing);
            throw new BusinessRuleException("LISTING_EXPIRED",
                    "This listing has expired");
        }

        BigDecimal sellerFee = listing.getSellerFee();
        BigDecimal netAmount = MoneyUtil.round(listing.getTotalPrice().subtract(sellerFee));

        // Create marketplace transaction in ESCROW status
        MarketplaceTransaction transaction = MarketplaceTransaction.builder()
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(listing.getSellerId())
                .shares(listing.getSharesListed())
                .pricePerShare(listing.getPricePerShare())
                .totalAmount(listing.getTotalPrice())
                .sellerFee(sellerFee)
                .netAmount(netAmount)
                .status(MarketplaceTransactionStatus.ESCROW)
                .build();

        transaction = transactionRepository.save(transaction);

        // Mark listing as sold
        listing.setStatus(ListingStatus.SOLD);
        listing.setSoldAt(Instant.now());
        listing.setBuyerId(buyerId);
        listingRepository.save(listing);

        // Complete the transaction (ownership transfer)
        transaction.setStatus(MarketplaceTransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);

        log.info("Listing {} purchased successfully by buyer {}. Transaction {} completed. Net to seller: {}",
                listingId, buyerId, transaction.getId(), netAmount);

        return mapToTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> getActiveListings(MarketplaceSearchCriteria criteria, Pageable pageable) {
        Specification<MarketplaceListing> spec = buildSearchSpecification(criteria);
        return listingRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ListingResponse getListing(UUID id) {
        MarketplaceListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceListing", id));
        return mapToResponse(listing);
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> getUserListings(UUID userId, Pageable pageable) {
        return listingRepository.findBySellerIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    private Specification<MarketplaceListing> buildSearchSpecification(MarketplaceSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter for ACTIVE listings
            predicates.add(cb.equal(root.get("status"), ListingStatus.ACTIVE));

            if (criteria.getCampaignId() != null) {
                predicates.add(cb.equal(root.get("campaignId"), criteria.getCampaignId()));
            }

            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPrice"), criteria.getMinPrice()));
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPrice"), criteria.getMaxPrice()));
            }

            // Note: industry filtering requires a join to Campaign or a denormalized field.
            // For now, campaignId-based filtering is the primary mechanism. Industry filtering
            // can be added when the listing entity is enriched or via a subquery approach.

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ListingResponse mapToResponse(MarketplaceListing listing) {
        ListingResponse.ListingResponseBuilder builder = ListingResponse.builder()
                .id(listing.getId())
                .sellerId(listing.getSellerId())
                .investmentId(listing.getInvestmentId())
                .campaignId(listing.getCampaignId())
                .sharesListed(listing.getSharesListed())
                .pricePerShare(listing.getPricePerShare())
                .totalPrice(listing.getTotalPrice())
                .status(listing.getStatus().name())
                .companyConsent(listing.isCompanyConsent())
                .expiresAt(listing.getExpiresAt())
                .soldAt(listing.getSoldAt())
                .buyerId(listing.getBuyerId())
                .sellerFee(listing.getSellerFee())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt());

        // Enrich with campaign data
        try {
            campaignRepository.findByIdAndDeletedFalse(listing.getCampaignId())
                    .ifPresent(campaign -> {
                        builder.campaignTitle(campaign.getTitle());
                        builder.companyName(campaign.getCompanyName());
                        builder.industry(campaign.getIndustry());
                    });
        } catch (Exception e) {
            log.warn("Failed to enrich listing {} with campaign data: {}", listing.getId(), e.getMessage());
        }

        return builder.build();
    }

    private MarketplaceTransactionResponse mapToTransactionResponse(MarketplaceTransaction transaction) {
        return MarketplaceTransactionResponse.builder()
                .id(transaction.getId())
                .listingId(transaction.getListingId())
                .buyerId(transaction.getBuyerId())
                .sellerId(transaction.getSellerId())
                .shares(transaction.getShares())
                .pricePerShare(transaction.getPricePerShare())
                .totalAmount(transaction.getTotalAmount())
                .sellerFee(transaction.getSellerFee())
                .netAmount(transaction.getNetAmount())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
