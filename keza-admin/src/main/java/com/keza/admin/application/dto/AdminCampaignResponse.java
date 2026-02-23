package com.keza.admin.application.dto;

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
public class AdminCampaignResponse {

    private UUID id;
    private String title;
    private String slug;
    private String companyName;
    private String industry;
    private String status;
    private BigDecimal targetAmount;
    private BigDecimal raisedAmount;
    private Integer investorCount;
    private UUID issuerId;
    private String issuerFirstName;
    private String issuerLastName;
    private String issuerEmail;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;
    private Instant updatedAt;
}
