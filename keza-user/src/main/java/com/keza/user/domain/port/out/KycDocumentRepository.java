package com.keza.user.domain.port.out;

import com.keza.common.enums.DocumentType;
import com.keza.user.domain.model.KycDocument;
import com.keza.user.domain.model.KycDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByUserId(UUID userId);

    List<KycDocument> findByUserIdAndDocumentType(UUID userId, DocumentType documentType);

    long countByUserIdAndStatus(UUID userId, KycDocumentStatus status);

    List<KycDocument> findByStatus(KycDocumentStatus status);
}
