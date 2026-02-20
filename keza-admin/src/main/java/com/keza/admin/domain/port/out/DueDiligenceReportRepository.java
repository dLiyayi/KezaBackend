package com.keza.admin.domain.port.out;

import com.keza.admin.domain.model.DueDiligenceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DueDiligenceReportRepository extends JpaRepository<DueDiligenceReport, UUID> {

    Optional<DueDiligenceReport> findByCampaignId(UUID campaignId);
}
