package com.keza.investment.application.dto;

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
public class InvestmentCertificateResponse {

    private UUID certificateId;
    private UUID investmentId;
    private String investorName;
    private String campaignTitle;
    private String companyName;
    private String industry;
    private BigDecimal investmentAmount;
    private long shares;
    private BigDecimal sharePrice;
    private String currency;
    private Instant investmentDate;
    private Instant completedDate;
    private String status;
    private String certificateNumber;
    private Instant issuedAt;
}
