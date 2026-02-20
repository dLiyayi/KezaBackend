package com.keza.user.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.common.enums.DocumentType;
import com.keza.user.application.dto.KycDocumentResponse;
import com.keza.user.application.usecase.KycUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycUseCase kycUseCase;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycDocumentResponse>> uploadDocument(
            Authentication authentication,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file) {
        UUID userId = (UUID) authentication.getPrincipal();
        KycDocumentResponse response = kycUseCase.uploadDocument(userId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getDocuments(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<KycDocumentResponse> response = kycUseCase.getDocuments(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/documents/{id}/url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDocumentUrl(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        String url = kycUseCase.getDocumentUrl(userId, id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @PostMapping("/admin/documents/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> reviewDocument(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam("approved") boolean approved,
            @RequestParam(value = "reason", required = false) String reason) {
        UUID reviewerId = (UUID) authentication.getPrincipal();
        KycDocumentResponse response = kycUseCase.adminReviewDocument(id, reviewerId, approved, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Document reviewed successfully"));
    }

    @GetMapping("/admin/documents/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getPendingDocuments() {
        List<KycDocumentResponse> response = kycUseCase.getPendingDocuments();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
