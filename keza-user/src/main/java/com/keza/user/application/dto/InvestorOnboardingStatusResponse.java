package com.keza.user.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorOnboardingStatusResponse {

    private boolean emailVerified;
    private boolean profileComplete;
    private boolean investmentAccountOpen;
    private boolean kycApproved;
    private boolean accredited;
    private boolean readyToInvest;
    private String currentPhase;
    private String nextStep;
}
