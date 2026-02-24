package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.FundDisbursement;
import com.keza.common.enums.DisbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FundDisbursementRepository extends JpaRepository<FundDisbursement, UUID> {

    List<FundDisbursement> findByCampaignIdAndDeletedFalseOrderByRequestedAtDesc(UUID campaignId);

    Optional<FundDisbursement> findByIdAndDeletedFalse(UUID id);

    @Query("SELECT COALESCE(SUM(fd.amount), 0) FROM FundDisbursement fd WHERE fd.campaignId = :campaignId AND fd.status = :status AND fd.deleted = false")
    BigDecimal sumAmountByCampaignIdAndStatus(@Param("campaignId") UUID campaignId, @Param("status") DisbursementStatus status);
}
