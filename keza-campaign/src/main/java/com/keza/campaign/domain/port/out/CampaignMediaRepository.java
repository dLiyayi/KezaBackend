package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.CampaignMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignMediaRepository extends JpaRepository<CampaignMedia, UUID> {

    List<CampaignMedia> findByCampaignIdOrderBySortOrderAsc(UUID campaignId);

    void deleteByCampaignId(UUID campaignId);
}
