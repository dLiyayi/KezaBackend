package com.keza.user.adapter.out.external;

import com.keza.common.exception.ResourceNotFoundException;
import com.keza.user.domain.model.KycDocument;
import com.keza.user.domain.model.KycDocumentStatus;
import com.keza.user.domain.port.in.DocumentProcessingPort;
import com.keza.user.domain.port.out.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Stub implementation of DocumentProcessingPort used when AI processing is disabled.
 * Leaves documents in PENDING status for manual review by an admin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubDocumentProcessor implements DocumentProcessingPort {

    private final KycDocumentRepository kycDocumentRepository;

    @Override
    @Transactional
    public void processDocument(UUID documentId) {
        log.info("Stub document processor invoked for documentId={}. " +
                "AI processing is disabled; document will remain PENDING for manual review.", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KycDocument", documentId));

        // Ensure the document stays in PENDING status for manual admin review.
        // In a real AI processor, this would analyze the document and potentially
        // transition it to IN_REVIEW, APPROVED, or REJECTED with extracted data
        // and a confidence score.
        if (document.getStatus() == KycDocumentStatus.PENDING) {
            log.info("Document {} remains in PENDING status awaiting manual review. " +
                    "Type: {}, User: {}", documentId, document.getDocumentType(), document.getUserId());
        }
    }
}
