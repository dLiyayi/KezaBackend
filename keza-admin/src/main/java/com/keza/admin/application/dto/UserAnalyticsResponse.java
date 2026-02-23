package com.keza.admin.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAnalyticsResponse {

    private long totalUsers;
    private long totalInvestors;
    private long totalIssuers;
    private long verifiedUsers;
    private long unverifiedUsers;
    private long lockedUsers;
    private long kycPending;
    private long kycSubmitted;
    private long kycApproved;
    private long kycRejected;
    private long registrationsLast7Days;
    private long registrationsLast30Days;
}
