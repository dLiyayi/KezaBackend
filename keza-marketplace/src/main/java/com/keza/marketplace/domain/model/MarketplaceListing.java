package com.keza.marketplace.domain.model;

import com.keza.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceListing extends BaseEntity {

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "investment_id", nullable = false)
    private UUID investmentId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "shares_listed", nullable = false)
    private long sharesListed;

    @Column(name = "price_per_share", nullable = false, precision = 15, scale = 4)
    private BigDecimal pricePerShare;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "company_consent", nullable = false)
    @Builder.Default
    private boolean companyConsent = false;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sold_at")
    private Instant soldAt;

    @Column(name = "buyer_id")
    private UUID buyerId;

    @Column(name = "seller_fee", precision = 15, scale = 2)
    private BigDecimal sellerFee;
}
