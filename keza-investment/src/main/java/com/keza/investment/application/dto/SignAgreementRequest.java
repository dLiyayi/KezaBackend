package com.keza.investment.application.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignAgreementRequest {

    @AssertTrue(message = "You must acknowledge the risk disclosures")
    private boolean riskAcknowledged;

    @NotBlank(message = "Agreement version is required")
    private String agreementVersion;

    private String ipAddress;

    private String userAgent;
}
