package com.keza.campaign.application.dto;

import com.keza.common.enums.OfferingType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

public final class CampaignRequest {

    private CampaignRequest() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfoRequest {

        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        private String companyName;

        @Size(max = 100)
        private String registrationNumber;

        @Size(max = 500)
        private String website;

        private String address;

        @Size(max = 100)
        private String industry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferingDetailsRequest {

        @NotNull(message = "Offering type is required")
        private OfferingType offeringType;

        @NotNull(message = "Target amount is required")
        @DecimalMin(value = "0.01", message = "Target amount must be positive")
        private BigDecimal targetAmount;

        @DecimalMin(value = "0.0001", message = "Share price must be positive")
        private BigDecimal sharePrice;

        @Min(value = 1, message = "Total shares must be at least 1")
        private Long totalShares;

        @NotNull(message = "Minimum investment is required")
        @DecimalMin(value = "0.01", message = "Minimum investment must be positive")
        private BigDecimal minInvestment;

        @DecimalMin(value = "0.01", message = "Maximum investment must be positive")
        private BigDecimal maxInvestment;

        private Instant startDate;

        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        private Instant endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PitchContentRequest {

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        private String title;

        @Size(max = 500)
        private String tagline;

        @NotBlank(message = "Description is required")
        private String description;

        @Size(max = 500)
        private String pitchVideoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialProjectionsRequest {

        private String financialProjections;

        private String useOfFunds;

        private String riskFactors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentsRequest {

        private boolean acknowledged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSubmitRequest {

        @AssertTrue(message = "You must confirm submission")
        private boolean confirmed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {

        @NotBlank(message = "Rejection reason is required")
        private String reason;
    }
}
