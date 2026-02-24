package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.IssuerOnboardingStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssuerOnboardingStepRepository extends JpaRepository<IssuerOnboardingStep, UUID> {

    List<IssuerOnboardingStep> findByApplicationIdOrderByPhaseAscStepNameAsc(UUID applicationId);
}
