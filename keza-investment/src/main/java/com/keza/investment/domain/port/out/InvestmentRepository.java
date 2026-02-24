package com.keza.investment.domain.port.out;

import com.keza.investment.domain.model.Investment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, UUID>, JpaSpecificationExecutor<Investment> {

    Optional<Investment> findByInvestorIdAndCampaignId(UUID investorId, UUID campaignId);

    Page<Investment> findByInvestorIdOrderByCreatedAtDesc(UUID investorId, Pageable pageable);

    boolean existsByInvestorIdAndCampaignId(UUID investorId, UUID campaignId);

    Page<Investment> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);
}
