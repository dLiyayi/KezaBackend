package com.keza.marketplace.domain.port.out;

import com.keza.marketplace.domain.model.ListingStatus;
import com.keza.marketplace.domain.model.MarketplaceListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, UUID>,
        JpaSpecificationExecutor<MarketplaceListing> {

    Page<MarketplaceListing> findByStatusOrderByCreatedAtDesc(ListingStatus status, Pageable pageable);

    Page<MarketplaceListing> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<MarketplaceListing> findByCampaignIdAndStatusOrderByCreatedAtDesc(UUID campaignId, ListingStatus status, Pageable pageable);

    Optional<MarketplaceListing> findByIdAndSellerId(UUID id, UUID sellerId);

    List<MarketplaceListing> findByInvestmentIdAndStatus(UUID investmentId, ListingStatus status);

    boolean existsByInvestmentIdAndStatusIn(UUID investmentId, List<ListingStatus> statuses);
}
