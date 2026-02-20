package com.keza.campaign.application.dto;

import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.OfferingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {

    private UUID id;
    private UUID issuerId;
    private String title;
    private String slug;
    private String tagline;
    private String description;
    private String industry;
    private String companyName;
    private String companyRegistrationNumber;
    private String companyWebsite;
    private String companyAddress;
    private OfferingType offeringType;
    private BigDecimal targetAmount;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private BigDecimal raisedAmount;
    private BigDecimal sharePrice;
    private Long totalShares;
    private Long soldShares;
    private BigDecimal minInvestment;
    private BigDecimal maxInvestment;
    private Integer investorCount;
    private CampaignStatus status;
    private Integer wizardStep;
    private Instant startDate;
    private Instant endDate;
    private Instant fundedAt;
    private String pitchVideoUrl;
    private String financialProjections;
    private String riskFactors;
    private String useOfFunds;
    private String teamMembers;
    private List<CampaignMediaResponse> media;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignMediaResponse {
        private UUID id;
        private String fileKey;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private String mediaType;
        private Integer sortOrder;
    }
}
