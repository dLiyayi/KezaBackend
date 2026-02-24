package com.keza.investment.domain.port.out;

import com.keza.investment.domain.model.SubscriptionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionAgreementRepository extends JpaRepository<SubscriptionAgreement, UUID> {

    Optional<SubscriptionAgreement> findByInvestmentId(UUID investmentId);

    Optional<SubscriptionAgreement> findByUserIdAndCampaignId(UUID userId, UUID campaignId);
}
