package com.keza.investment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.investment.application.dto.InvestmentCertificateResponse;
import com.keza.investment.application.usecase.InvestmentCertificateUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
public class InvestmentCertificateController {

    private final InvestmentCertificateUseCase investmentCertificateUseCase;

    @GetMapping("/{investmentId}/certificate")
    public ResponseEntity<ApiResponse<InvestmentCertificateResponse>> getCertificate(
            @PathVariable UUID investmentId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        InvestmentCertificateResponse response = investmentCertificateUseCase.generateCertificate(investmentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
