package com.keza.ai.adapter.in.web;

import com.keza.ai.application.dto.DocumentValidationRequest;
import com.keza.ai.application.dto.DocumentValidationResponse;
import com.keza.ai.application.usecase.DocumentValidationUseCaseBase;
import com.keza.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/documents")
@RequiredArgsConstructor
public class AiDocumentController {

    private final DocumentValidationUseCaseBase documentValidationUseCase;

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<DocumentValidationResponse>> validateDocument(
            @Valid @RequestBody DocumentValidationRequest request) {
        DocumentValidationResponse response = documentValidationUseCase.validateDocument(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
