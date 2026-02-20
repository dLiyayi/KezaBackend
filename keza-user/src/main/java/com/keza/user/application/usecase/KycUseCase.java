package com.keza.user.application.usecase;

import com.keza.common.enums.DocumentType;
import com.keza.common.enums.KycStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ForbiddenException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.audit.AuditLogger;
import com.keza.infrastructure.config.RabbitMQConfig;
import com.keza.infrastructure.config.StorageConfig;
import com.keza.infrastructure.config.StorageService;
import com.keza.user.application.dto.KycDocumentResponse;
import com.keza.user.domain.event.KycStatusChangedEvent;
import com.keza.user.domain.model.KycDocument;
import com.keza.user.domain.model.KycDocumentStatus;
import com.keza.user.domain.model.User;
import com.keza.user.domain.port.out.KycDocumentRepository;
import com.keza.user.domain.port.out.UserRepository;
import com.keza.user.domain.service.KycStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycUseCase {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf"
    );
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(15);

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageConfig storageConfig;
    private final RabbitTemplate rabbitTemplate;
    private final KycStateMachine kycStateMachine;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    @Transactional
    public KycDocumentResponse uploadDocument(UUID userId, DocumentType type, MultipartFile file) {
        // Validate user exists
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Validate file
        validateFile(file);

        // Build S3 key: kyc/{userId}/{docType}/{uuid}.ext
        String extension = getFileExtension(file.getOriginalFilename());
        String fileKey = String.format("kyc/%s/%s/%s.%s",
                userId, type.name().toLowerCase(), UUID.randomUUID(), extension);

        // Upload to S3
        String bucket = storageConfig.getBuckets().get("kyc");
        try {
            storageService.upload(bucket, fileKey, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessRuleException("FILE_UPLOAD_FAILED", "Failed to upload document: " + e.getMessage());
        }

        // Create and save KYC document entity
        KycDocument document = KycDocument.builder()
                .userId(userId)
                .documentType(type)
                .fileKey(fileKey)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .status(KycDocumentStatus.PENDING)
                .build();

        document = kycDocumentRepository.save(document);

        // Update user KYC status to SUBMITTED if currently PENDING
        if (user.getKycStatus() == KycStatus.PENDING) {
            String oldStatus = user.getKycStatus().name();
            user.setKycStatus(KycStatus.SUBMITTED);
            userRepository.save(user);
            eventPublisher.publishEvent(new KycStatusChangedEvent(
                    userId, oldStatus, KycStatus.SUBMITTED.name(), document.getId()));
        }

        // Publish message to RabbitMQ for async processing
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.KYC_EXCHANGE,
                RabbitMQConfig.KYC_ROUTING_KEY,
                document.getId()
        );

        auditLogger.log("KYC_DOCUMENT_UPLOADED", "KycDocument", document.getId().toString(),
                String.format("User %s uploaded %s document: %s", userId, type, file.getOriginalFilename()));

        log.info("KYC document uploaded: documentId={}, userId={}, type={}", document.getId(), userId, type);

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getDocuments(UUID userId) {
        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);
        return documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getDocumentUrl(UUID userId, UUID documentId) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KycDocument", documentId));

        if (!document.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this document");
        }

        String bucket = storageConfig.getBuckets().get("kyc");
        return storageService.generatePresignedUrl(bucket, document.getFileKey(), PRESIGNED_URL_EXPIRATION);
    }

    @Transactional
    public KycDocumentResponse adminReviewDocument(UUID documentId, UUID reviewerId, boolean approved, String reason) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KycDocument", documentId));

        KycDocumentStatus targetStatus = approved ? KycDocumentStatus.APPROVED : KycDocumentStatus.REJECTED;

        // If document is PENDING, transition to IN_REVIEW first
        if (document.getStatus() == KycDocumentStatus.PENDING) {
            kycStateMachine.validateTransition(document.getStatus(), KycDocumentStatus.IN_REVIEW);
            document.setStatus(KycDocumentStatus.IN_REVIEW);
        }

        // Now transition to the final status
        kycStateMachine.validateTransition(document.getStatus(), targetStatus);

        String oldStatus = document.getStatus().name();
        document.setStatus(targetStatus);
        document.setReviewedBy(reviewerId);
        document.setReviewedAt(Instant.now());

        if (!approved && reason != null) {
            document.setRejectionReason(reason);
        }

        UUID userId = document.getUserId();
        KycDocument savedDocument = kycDocumentRepository.save(document);

        auditLogger.log("KYC_DOCUMENT_REVIEWED", "KycDocument", documentId.toString(),
                oldStatus, targetStatus.name(),
                String.format("Reviewer %s %s document. Reason: %s",
                        reviewerId, approved ? "approved" : "rejected", reason));

        // Check if all required documents are now approved
        if (approved) {
            checkAndUpdateUserKycStatus(userId);
        }

        // If rejected, update user KYC status back to REJECTED
        if (!approved) {
            User user = userRepository.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            if (user.getKycStatus() != KycStatus.REJECTED) {
                String oldUserStatus = user.getKycStatus().name();
                user.setKycStatus(KycStatus.REJECTED);
                userRepository.save(user);
                eventPublisher.publishEvent(new KycStatusChangedEvent(
                        user.getId(), oldUserStatus, KycStatus.REJECTED.name(), documentId));
            }
        }

        log.info("KYC document reviewed: documentId={}, status={}, reviewer={}",
                documentId, targetStatus, reviewerId);

        return mapToResponse(savedDocument);
    }

    @Transactional
    public void checkAndUpdateUserKycStatus(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Get all required document types
        Set<DocumentType> requiredTypes = kycStateMachine.getRequiredDocumentTypes();

        // Check if each required type has at least one approved document
        boolean allApproved = requiredTypes.stream().allMatch(type -> {
            List<KycDocument> docs = kycDocumentRepository.findByUserIdAndDocumentType(userId, type);
            return docs.stream().anyMatch(d -> d.getStatus() == KycDocumentStatus.APPROVED);
        });

        if (allApproved && user.getKycStatus() != KycStatus.APPROVED) {
            String oldStatus = user.getKycStatus().name();
            user.setKycStatus(KycStatus.APPROVED);
            userRepository.save(user);
            eventPublisher.publishEvent(new KycStatusChangedEvent(
                    userId, oldStatus, KycStatus.APPROVED.name(), null));

            auditLogger.log("KYC_AUTO_APPROVED", "User", userId.toString(),
                    oldStatus, KycStatus.APPROVED.name(),
                    "All required KYC documents approved - user KYC status auto-updated");

            log.info("User KYC status auto-approved: userId={}", userId);
        }
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getPendingDocuments() {
        return kycDocumentRepository.findByStatus(KycDocumentStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("EMPTY_FILE", "File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("FILE_TOO_LARGE",
                    String.format("File size %d exceeds maximum allowed size of %d bytes",
                            file.getSize(), MAX_FILE_SIZE));
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException("INVALID_FILE_TYPE",
                    String.format("File type '%s' is not allowed. Allowed types: JPG, PNG, PDF",
                            file.getContentType()));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private KycDocumentResponse mapToResponse(KycDocument document) {
        return KycDocumentResponse.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .documentType(document.getDocumentType().name())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .contentType(document.getContentType())
                .status(document.getStatus().name())
                .rejectionReason(document.getRejectionReason())
                .extractedData(document.getExtractedData())
                .aiConfidenceScore(document.getAiConfidenceScore())
                .reviewedBy(document.getReviewedBy())
                .reviewedAt(document.getReviewedAt())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
