package com.keza.app.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.ai.domain.model.DocumentQualityResult;
import com.keza.ai.domain.model.DocumentValidationResult;
import com.keza.ai.domain.service.AiDocumentValidationService;
import com.keza.ai.domain.service.DocumentTextExtractor;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.config.StorageConfig;
import com.keza.infrastructure.config.StorageService;
import com.keza.user.domain.event.KycStatusChangedEvent;
import com.keza.user.domain.model.KycDocument;
import com.keza.user.domain.model.KycDocumentStatus;
import com.keza.user.domain.port.in.DocumentProcessingPort;
import com.keza.user.domain.port.out.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI-powered implementation of {@link DocumentProcessingPort} that orchestrates
 * the full document validation pipeline:
 * <ol>
 *   <li>Load KYC document from the database</li>
 *   <li>Download the file from S3/MinIO storage</li>
 *   <li>Extract text content (PDF text extraction, image OCR placeholder)</li>
 *   <li>Assess document quality (file size, content type checks)</li>
 *   <li>Run AI-powered validation with Spring AI</li>
 *   <li>Update the document status based on AI recommendation</li>
 *   <li>Publish events for auto-approved documents</li>
 * </ol>
 * <p>
 * This adapter lives in keza-app (which depends on all modules) and bridges
 * keza-ai services with keza-user domain entities. Marked as {@code @Primary}
 * to take precedence over the {@code StubDocumentProcessor} when AI is enabled.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class AiDocumentProcessor implements DocumentProcessingPort {

    private final KycDocumentRepository kycDocumentRepository;
    private final StorageService storageService;
    private final StorageConfig storageConfig;
    private final AiDocumentValidationService aiDocumentValidationService;
    private final DocumentTextExtractor documentTextExtractor;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void processDocument(UUID documentId) {
        log.info("Starting AI document processing pipeline for documentId={}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KycDocument", documentId));

        try {
            // Step 1: Download file from S3
            String bucket = storageConfig.getBuckets().get("kyc");
            log.info("Downloading document from S3: bucket='{}', key='{}'", bucket, document.getFileKey());
            InputStream fileStream = storageService.download(bucket, document.getFileKey());

            // Step 2: Extract text content
            log.info("Extracting text from document: file='{}', contentType='{}'",
                    document.getFileName(), document.getContentType());
            String extractedText = documentTextExtractor.extractText(
                    fileStream, document.getContentType(), document.getFileName());
            log.info("Text extraction complete for documentId={}: {} characters extracted",
                    documentId, extractedText.length());

            // Step 3: Assess document quality
            DocumentQualityResult qualityResult = aiDocumentValidationService.assessDocumentQuality(
                    document.getContentType(), document.getFileSize(), document.getFileName());

            if (!qualityResult.passesMinimumQuality()) {
                log.warn("Document {} failed quality assessment: score={}, issues={}",
                        documentId, qualityResult.qualityScore(), qualityResult.issues());

                Map<String, Object> qualityData = new LinkedHashMap<>();
                qualityData.put("qualityScore", qualityResult.qualityScore());
                qualityData.put("qualityIssues", qualityResult.issues());
                qualityData.put("status", "QUALITY_CHECK_FAILED");

                document.setExtractedData(objectMapper.writeValueAsString(qualityData));
                document.setStatus(KycDocumentStatus.PENDING);
                document.setAiConfidenceScore(BigDecimal.valueOf(qualityResult.qualityScore())
                        .setScale(4, RoundingMode.HALF_UP));
                kycDocumentRepository.save(document);

                log.info("Document {} set to PENDING due to quality issues", documentId);
                return;
            }

            // Step 4: Run AI validation
            log.info("Running AI validation for documentId={}, docType={}",
                    documentId, document.getDocumentType());
            DocumentValidationResult validationResult = aiDocumentValidationService.validateDocument(
                    document.getDocumentType().name(),
                    document.getFileName(),
                    document.getContentType(),
                    document.getFileSize(),
                    extractedText);

            // Step 5: Update document with results
            BigDecimal confidenceScore = BigDecimal.valueOf(validationResult.confidenceScore())
                    .setScale(4, RoundingMode.HALF_UP);
            document.setAiConfidenceScore(confidenceScore);

            Map<String, Object> extractedData = new LinkedHashMap<>();
            extractedData.put("extractedFields", validationResult.extractedFields());
            extractedData.put("detectedDocumentType", validationResult.detectedDocumentType());
            extractedData.put("issues", validationResult.issues());
            extractedData.put("isValid", validationResult.isValid());
            extractedData.put("qualityScore", qualityResult.qualityScore());
            extractedData.put("qualityIssues", qualityResult.issues());
            extractedData.put("recommendation", validationResult.recommendation());
            document.setExtractedData(objectMapper.writeValueAsString(extractedData));

            KycDocumentStatus oldStatus = document.getStatus();
            KycDocumentStatus newStatus = switch (validationResult.recommendation()) {
                case "AUTO_APPROVE" -> KycDocumentStatus.APPROVED;
                case "QUICK_REVIEW" -> KycDocumentStatus.IN_REVIEW;
                default -> KycDocumentStatus.PENDING;
            };
            document.setStatus(newStatus);

            kycDocumentRepository.save(document);

            log.info("Document {} processed: confidence={}, recommendation={}, status={}",
                    documentId, confidenceScore, validationResult.recommendation(), newStatus);

            // Step 6: Publish event if auto-approved
            if (newStatus == KycDocumentStatus.APPROVED) {
                log.info("Document {} auto-approved with confidence {}. Publishing KycStatusChangedEvent.",
                        documentId, confidenceScore);
                eventPublisher.publishEvent(new KycStatusChangedEvent(
                        document.getUserId(),
                        oldStatus.name(),
                        newStatus.name(),
                        documentId));
            }

        } catch (Exception e) {
            log.error("AI document processing failed for documentId={}: {}", documentId, e.getMessage(), e);

            try {
                Map<String, Object> errorData = new LinkedHashMap<>();
                errorData.put("error", e.getMessage());
                errorData.put("errorType", e.getClass().getSimpleName());
                errorData.put("status", "PROCESSING_FAILED");

                document.setExtractedData(objectMapper.writeValueAsString(errorData));
                document.setStatus(KycDocumentStatus.PENDING);
                kycDocumentRepository.save(document);
            } catch (Exception jsonException) {
                log.error("Failed to store error data for documentId={}: {}",
                        documentId, jsonException.getMessage());
                document.setStatus(KycDocumentStatus.PENDING);
                kycDocumentRepository.save(document);
            }
        }
    }
}
