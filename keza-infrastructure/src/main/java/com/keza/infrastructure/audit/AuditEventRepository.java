package com.keza.infrastructure.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(
            String entityType, String entityId, Pageable pageable);

    Page<AuditEvent> findByPerformedByOrderByPerformedAtDesc(String performedBy, Pageable pageable);
}
