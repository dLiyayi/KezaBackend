package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.CampaignUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CampaignUpdateRepository extends JpaRepository<CampaignUpdate, UUID> {

    Page<CampaignUpdate> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);

    Page<CampaignUpdate> findByCampaignIdAndPublishedTrueOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);

    long countByCampaignId(UUID campaignId);
}
