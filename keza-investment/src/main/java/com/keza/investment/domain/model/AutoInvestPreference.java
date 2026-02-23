package com.keza.investment.domain.model;

import com.keza.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "auto_invest_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoInvestPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "budget_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "remaining_budget", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBudget;

    @Column(name = "max_per_campaign", precision = 15, scale = 2)
    private BigDecimal maxPerCampaign;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "industries", columnDefinition = "TEXT[]")
    private List<String> industries;

    @Column(name = "min_target_amount", precision = 15, scale = 2)
    private BigDecimal minTargetAmount;

    @Column(name = "max_target_amount", precision = 15, scale = 2)
    private BigDecimal maxTargetAmount;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "offering_types", columnDefinition = "TEXT[]")
    private List<String> offeringTypes;
}
