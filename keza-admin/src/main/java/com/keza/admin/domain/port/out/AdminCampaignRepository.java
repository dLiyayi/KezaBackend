package com.keza.admin.domain.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminCampaignRepository {

    Page<Map<String, Object>> findCampaigns(String status, String industry, String search, Pageable pageable);

    Optional<Map<String, Object>> findCampaignById(UUID campaignId);

    int assignReviewer(UUID campaignId, UUID reviewerId);

    long countCampaignsByStatus(String status);
}
