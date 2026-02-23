package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.CampaignQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CampaignQuestionRepository extends JpaRepository<CampaignQuestion, UUID> {

    Page<CampaignQuestion> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);

    Page<CampaignQuestion> findByCampaignIdAndAnswerIsNotNullOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);
}
