package com.keza.campaign.application.dto;

import com.keza.common.enums.BusinessStage;
import com.keza.common.enums.RegulationType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuerApplicationRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255)
    private String companyName;

    @Size(max = 100)
    private String companyRegistrationNumber;

    @Size(max = 500)
    private String companyWebsite;

    @Size(max = 100)
    private String industry;

    @NotNull(message = "Business stage is required")
    private BusinessStage businessStage;

    @NotNull(message = "Funding goal is required")
    @DecimalMin(value = "0.01", message = "Funding goal must be positive")
    private BigDecimal fundingGoal;

    @NotNull(message = "Regulation type is required")
    private RegulationType regulationType;

    private String pitchSummary;
}
