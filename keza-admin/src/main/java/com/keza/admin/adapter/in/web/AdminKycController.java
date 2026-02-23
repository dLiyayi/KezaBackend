package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.AdminKycDocumentResponse;
import com.keza.admin.application.usecase.AdminKycUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminKycController {

    private final AdminKycUseCase adminKycUseCase;

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<PagedResponse<AdminKycDocumentResponse>>> listKycDocuments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<AdminKycDocumentResponse> response = adminKycUseCase.listKycDocuments(
                status, documentType, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
