package com.keza.marketplace.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import com.keza.marketplace.application.dto.*;
import com.keza.marketplace.application.usecase.MarketplaceUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceUseCase marketplaceUseCase;

    @PostMapping("/listings")
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @RequestBody @Valid CreateListingRequest request,
            Authentication authentication) {

        UUID sellerId = UUID.fromString(authentication.getName());
        ListingResponse response = marketplaceUseCase.createListing(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Listing created successfully"));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<PagedResponse<ListingResponse>>> getActiveListings(
            @RequestParam(required = false) UUID campaignId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String industry,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        size = Math.min(size, 100);

        MarketplaceSearchCriteria criteria = MarketplaceSearchCriteria.builder()
                .campaignId(campaignId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .industry(industry)
                .build();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ListingResponse> results = marketplaceUseCase.getActiveListings(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(results)));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getListing(@PathVariable UUID id) {
        ListingResponse response = marketplaceUseCase.getListing(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> cancelListing(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID sellerId = UUID.fromString(authentication.getName());
        ListingResponse response = marketplaceUseCase.cancelListing(id, sellerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Listing cancelled successfully"));
    }

    @PostMapping("/listings/{id}/buy")
    public ResponseEntity<ApiResponse<MarketplaceTransactionResponse>> buyListing(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID buyerId = UUID.fromString(authentication.getName());
        MarketplaceTransactionResponse response = marketplaceUseCase.buyListing(id, buyerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase completed successfully"));
    }

    @GetMapping("/my-listings")
    public ResponseEntity<ApiResponse<PagedResponse<ListingResponse>>> getUserListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        size = Math.min(size, 100);
        UUID userId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ListingResponse> results = marketplaceUseCase.getUserListings(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(results)));
    }
}
