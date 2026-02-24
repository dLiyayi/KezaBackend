package com.keza.user.domain.port.out;

import com.keza.common.enums.AccreditationStatus;
import com.keza.user.domain.model.AccreditationVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccreditationVerificationRepository extends JpaRepository<AccreditationVerification, UUID> {

    List<AccreditationVerification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AccreditationVerification> findByStatus(AccreditationStatus status);

    boolean existsByUserIdAndStatus(UUID userId, AccreditationStatus status);
}
