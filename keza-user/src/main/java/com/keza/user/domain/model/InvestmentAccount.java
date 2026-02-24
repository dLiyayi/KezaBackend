package com.keza.user.domain.model;

import com.keza.common.domain.model.SoftDeletableEntity;
import com.keza.common.enums.AccountType;
import com.keza.common.enums.EmploymentStatus;
import com.keza.common.enums.InvestmentAccountStatus;
import com.keza.common.enums.MaritalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investment_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentAccount extends SoftDeletableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private AccountType accountType = AccountType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvestmentAccountStatus status = InvestmentAccountStatus.PENDING;

    @Column(length = 100)
    private String citizenship;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 20)
    private EmploymentStatus employmentStatus;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "net_worth", precision = 15, scale = 2)
    private BigDecimal netWorth;

    @Column(name = "investment_experience", length = 50)
    private String investmentExperience;

    @Column(name = "risk_tolerance", length = 20)
    private String riskTolerance;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "processing_notes", columnDefinition = "TEXT")
    private String processingNotes;
}
