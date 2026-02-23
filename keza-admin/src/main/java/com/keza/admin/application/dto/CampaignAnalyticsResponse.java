package com.keza.admin.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignAnalyticsResponse {

    private long totalCampaigns;
    private long draftCampaigns;
    private long reviewCampaigns;
    private long liveCampaigns;
    private long fundedCampaigns;
    private long closedCampaigns;
    private BigDecimal totalRaisedAmount;
    private BigDecimal averageFundingPercentage;
}
