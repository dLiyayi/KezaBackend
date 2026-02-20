package com.keza.admin.domain.port.out;

import com.keza.admin.domain.model.DDCheckStatus;
import com.keza.admin.domain.model.DueDiligenceCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DueDiligenceCheckRepository extends JpaRepository<DueDiligenceCheck, UUID> {

    List<DueDiligenceCheck> findByCampaignIdOrderBySortOrderAsc(UUID campaignId);

    long countByCampaignIdAndStatus(UUID campaignId, DDCheckStatus status);
}
