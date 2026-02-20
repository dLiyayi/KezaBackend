package com.keza.ai.domain.port.out;

import com.keza.ai.domain.model.FraudAlert;
import com.keza.ai.domain.model.FraudAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    Page<FraudAlert> findByStatus(FraudAlertStatus status, Pageable pageable);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<FraudAlert> findByUserIdAndStatus(UUID userId, FraudAlertStatus status);

    @Query("SELECT COUNT(f) FROM FraudAlert f WHERE f.userId = :userId AND f.alertType = :alertType AND f.createdAt > :since")
    long countRecentAlerts(@Param("userId") UUID userId, @Param("alertType") String alertType, @Param("since") Instant since);
}
