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
public class ListingResponse {

    private UUID id;
    private UUID sellerId;
    private UUID investmentId;
    private UUID campaignId;
    private String campaignTitle;
    private String companyName;
    private String industry;
    private long sharesListed;
    private BigDecimal pricePerShare;
    private BigDecimal totalPrice;
    private String status;
    private boolean companyConsent;
    private Instant expiresAt;
    private Instant soldAt;
    private UUID buyerId;
    private BigDecimal sellerFee;
    private Instant createdAt;
    private Instant updatedAt;
}
