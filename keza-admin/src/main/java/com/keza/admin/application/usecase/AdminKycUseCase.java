package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.AdminKycDocumentResponse;
import com.keza.admin.domain.port.out.AdminKycRepository;
import com.keza.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKycUseCase {

    private final AdminKycRepository adminKycRepository;

    @Transactional(readOnly = true)
    public PagedResponse<AdminKycDocumentResponse> listKycDocuments(String status, String documentType,
                                                                     String search, Pageable pageable) {
        Page<Map<String, Object>> page = adminKycRepository.findKycDocuments(status, documentType, search, pageable);

        List<AdminKycDocumentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<AdminKycDocumentResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private AdminKycDocumentResponse mapToResponse(Map<String, Object> data) {
        return AdminKycDocumentResponse.builder()
                .id(data.get("id") instanceof UUID uid ? uid : null)
                .userId(data.get("userId") instanceof UUID uid ? uid : null)
                .userFirstName((String) data.get("userFirstName"))
                .userLastName((String) data.get("userLastName"))
                .userEmail((String) data.get("userEmail"))
                .documentType((String) data.get("documentType"))
                .fileName((String) data.get("fileName"))
                .fileSize(data.get("fileSize") instanceof Number n ? n.longValue() : null)
                .contentType((String) data.get("contentType"))
                .status((String) data.get("status"))
                .rejectionReason((String) data.get("rejectionReason"))
                .aiConfidenceScore(data.get("aiConfidenceScore") instanceof BigDecimal bd ? bd : null)
                .reviewedBy(data.get("reviewedBy") instanceof UUID uid ? uid : null)
                .reviewedAt(data.get("reviewedAt") instanceof Instant inst ? inst : null)
                .createdAt(data.get("createdAt") instanceof Instant inst ? inst : null)
                .updatedAt(data.get("updatedAt") instanceof Instant inst ? inst : null)
                .build();
    }
}
