package com.keza.campaign.domain.model;

import com.keza.common.domain.model.SoftDeletableEntity;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.OfferingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign extends SoftDeletableEntity {

    @Column(name = "issuer_id", nullable = false)
    private UUID issuerId;

    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String slug;

    @Column(length = 500)
    private String tagline;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String industry;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_registration_number", length = 100)
    private String companyRegistrationNumber;

    @Column(name = "company_website", length = 500)
    private String companyWebsite;

    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "offering_type", nullable = false, length = 50)
    @Builder.Default
    private OfferingType offeringType = OfferingType.EQUITY;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "minimum_amount", precision = 15, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_amount", precision = 15, scale = 2)
    private BigDecimal maximumAmount;

    @Column(name = "raised_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal raisedAmount = BigDecimal.ZERO;

    @Column(name = "share_price", precision = 15, scale = 4)
    private BigDecimal sharePrice;

    @Column(name = "total_shares")
    private Long totalShares;

    @Column(name = "sold_shares", nullable = false)
    @Builder.Default
    private Long soldShares = 0L;

    @Column(name = "min_investment", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minInvestment = new BigDecimal("1000");

    @Column(name = "max_investment", precision = 15, scale = 2)
    private BigDecimal maxInvestment;

    @Column(name = "investor_count", nullable = false)
    @Builder.Default
    private Integer investorCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "wizard_step", nullable = false)
    @Builder.Default
    private Integer wizardStep = 1;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "funded_at")
    private Instant fundedAt;

    @Column(name = "pitch_video_url", length = 500)
    private String pitchVideoUrl;

    @Column(name = "financial_projections", columnDefinition = "JSONB")
    private String financialProjections;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "use_of_funds", columnDefinition = "JSONB")
    private String useOfFunds;

    @Column(name = "team_members", columnDefinition = "JSONB")
    private String teamMembers;

    @OneToMany(mappedBy = "campaignId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CampaignMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "campaignId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CampaignUpdate> updates = new ArrayList<>();
}
