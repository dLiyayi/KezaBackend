package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.DocumentValidationRequest;
import com.keza.ai.application.dto.DocumentValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "false", matchIfMissing = true)
public class StubDocumentValidationUseCase extends DocumentValidationUseCaseBase {

    @Override
    public DocumentValidationResponse validateDocument(DocumentValidationRequest request) {
        log.info("Stub document validation for {} (AI disabled)", request.getDocumentId());

        return DocumentValidationResponse.builder()
                .documentId(request.getDocumentId())
                .status("NEEDS_REVIEW")
                .confidenceScore(0.0)
                .issues(Collections.emptyList())
                .suggestions(List.of("AI validation is not enabled. Document submitted for manual review."))
                .qualityCheck(DocumentValidationResponse.QualityCheck.builder()
                        .imageClarity(true)
                        .documentComplete(true)
                        .tamperDetected(false)
                        .correctDocumentType(true)
                        .overallQuality("ACCEPTABLE")
                        .build())
                .build();
    }
}
