package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.Campaign;
import com.keza.common.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID>, JpaSpecificationExecutor<Campaign> {

    Optional<Campaign> findBySlug(String slug);

    Optional<Campaign> findByIdAndDeletedFalse(UUID id);

    Page<Campaign> findByIssuerIdAndDeletedFalse(UUID issuerId, Pageable pageable);

    @Modifying
    @Query("UPDATE Campaign c SET c.raisedAmount = c.raisedAmount + :amount, " +
            "c.investorCount = c.investorCount + 1, " +
            "c.soldShares = c.soldShares + :shares " +
            "WHERE c.id = :id AND c.version = :version")
    int updateRaisedAmount(@Param("id") UUID id,
                           @Param("amount") BigDecimal amount,
                           @Param("shares") long shares,
                           @Param("version") long version);

    List<Campaign> findByStatusAndEndDateBefore(CampaignStatus status, Instant dateTime);
}
