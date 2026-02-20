package com.keza.admin.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOverviewResponse {

    private long totalUsers;
    private long totalInvestors;
    private long totalIssuers;
    private long totalCampaigns;
    private long activeCampaigns;
    private BigDecimal totalInvested;
    private long pendingKycCount;
    private long pendingCampaignsCount;
}
