package com.keza.investment.domain.port.out;

import com.keza.investment.domain.model.AutoInvestPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutoInvestPreferenceRepository extends JpaRepository<AutoInvestPreference, UUID> {

    Optional<AutoInvestPreference> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<AutoInvestPreference> findByEnabledTrueAndRemainingBudgetGreaterThan(java.math.BigDecimal minBudget);
}
