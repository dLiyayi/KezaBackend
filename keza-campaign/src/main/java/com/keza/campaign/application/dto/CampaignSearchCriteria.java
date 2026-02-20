package com.keza.campaign.application.dto;

import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.OfferingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSearchCriteria {

    private String industry;
    private OfferingType offeringType;
    private CampaignStatus status;
    private String keyword;
    private BigDecimal minTarget;
    private BigDecimal maxTarget;
}
