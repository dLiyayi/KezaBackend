package com.keza.user.application.dto;

import com.keza.common.enums.AccountType;
import com.keza.common.enums.EmploymentStatus;
import com.keza.common.enums.MaritalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenInvestmentAccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @Size(max = 100)
    private String citizenship;

    private MaritalStatus maritalStatus;

    @NotNull(message = "Employment status is required")
    private EmploymentStatus employmentStatus;

    @NotNull(message = "Annual income is required")
    @DecimalMin(value = "0", message = "Annual income must be non-negative")
    private BigDecimal annualIncome;

    @NotNull(message = "Net worth is required")
    @DecimalMin(value = "0", message = "Net worth must be non-negative")
    private BigDecimal netWorth;

    @Size(max = 50)
    private String investmentExperience;

    @Size(max = 20)
    private String riskTolerance;
}
