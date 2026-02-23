package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.Watchlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    Page<Watchlist> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByUserIdAndCampaignId(UUID userId, UUID campaignId);

    void deleteByUserIdAndCampaignId(UUID userId, UUID campaignId);
}
