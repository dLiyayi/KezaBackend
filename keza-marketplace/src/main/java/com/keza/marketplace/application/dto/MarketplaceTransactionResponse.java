package com.keza.marketplace.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketplaceTransactionResponse {

    private UUID id;
    private UUID listingId;
    private UUID buyerId;
    private UUID sellerId;
    private long shares;
    private BigDecimal pricePerShare;
    private BigDecimal totalAmount;
    private BigDecimal sellerFee;
    private BigDecimal netAmount;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
