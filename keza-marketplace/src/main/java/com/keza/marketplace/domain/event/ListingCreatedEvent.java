package com.keza.marketplace.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingCreatedEvent(
        UUID listingId,
        UUID sellerId,
        UUID campaignId,
        UUID investmentId,
        long sharesListed,
        BigDecimal pricePerShare,
        BigDecimal totalPrice
) {
}
