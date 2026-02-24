package com.keza.user.application.dto;

import com.keza.common.enums.AccountType;
import com.keza.common.enums.EmploymentStatus;
import com.keza.common.enums.InvestmentAccountStatus;
import com.keza.common.enums.MaritalStatus;
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
public class InvestmentAccountResponse {

    private UUID id;
    private UUID userId;
    private AccountType accountType;
    private InvestmentAccountStatus status;
    private String citizenship;
    private MaritalStatus maritalStatus;
    private EmploymentStatus employmentStatus;
    private BigDecimal annualIncome;
    private BigDecimal netWorth;
    private String investmentExperience;
    private String riskTolerance;
    private Instant openedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
