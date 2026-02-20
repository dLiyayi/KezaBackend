package com.keza.marketplace.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateListingRequest {

    @NotNull(message = "Investment ID is required")
    private UUID investmentId;

    @Min(value = 1, message = "Shares listed must be at least 1")
    private long sharesListed;

    @NotNull(message = "Price per share is required")
    @DecimalMin(value = "0.0001", message = "Price per share must be greater than zero")
    private BigDecimal pricePerShare;

    @NotNull(message = "Company consent acknowledgement is required")
    private Boolean companyConsent;
}
