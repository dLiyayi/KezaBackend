package com.keza.campaign.domain.port.out;

import com.keza.campaign.domain.model.IssuerApplication;
import com.keza.common.enums.IssuerApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssuerApplicationRepository extends JpaRepository<IssuerApplication, UUID>, JpaSpecificationExecutor<IssuerApplication> {

    Optional<IssuerApplication> findByUserIdAndDeletedFalse(UUID userId);

    Optional<IssuerApplication> findByIdAndDeletedFalse(UUID id);

    Page<IssuerApplication> findByStatusAndDeletedFalse(IssuerApplicationStatus status, Pageable pageable);

    Page<IssuerApplication> findByDeletedFalse(Pageable pageable);

    boolean existsByUserIdAndStatusInAndDeletedFalse(UUID userId, Collection<IssuerApplicationStatus> statuses);
}
