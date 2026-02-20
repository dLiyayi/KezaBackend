package com.keza.marketplace.domain.port.out;

import com.keza.marketplace.domain.model.MarketplaceTransaction;
import com.keza.marketplace.domain.model.MarketplaceTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceTransactionRepository extends JpaRepository<MarketplaceTransaction, UUID> {

    Optional<MarketplaceTransaction> findByListingId(UUID listingId);

    List<MarketplaceTransaction> findByListingIdAndStatusIn(UUID listingId, List<MarketplaceTransactionStatus> statuses);

    Page<MarketplaceTransaction> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    Page<MarketplaceTransaction> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);
}
