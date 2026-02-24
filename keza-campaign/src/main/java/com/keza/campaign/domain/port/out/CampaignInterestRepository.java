package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.CampaignInterest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignInterestRepository extends JpaRepository<CampaignInterest, UUID> {

    Page<CampaignInterest> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);

    long countByCampaignId(UUID campaignId);

    @Query("SELECT COALESCE(SUM(ci.intendedAmount), 0) FROM CampaignInterest ci WHERE ci.campaignId = :campaignId")
    BigDecimal sumIntendedAmountByCampaignId(@Param("campaignId") UUID campaignId);

    boolean existsByCampaignIdAndUserId(UUID campaignId, UUID userId);

    List<CampaignInterest> findByCampaignId(UUID campaignId);
}
