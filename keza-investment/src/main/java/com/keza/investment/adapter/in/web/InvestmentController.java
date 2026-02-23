package com.keza.investment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import com.keza.investment.application.dto.CreateInvestmentRequest;
import com.keza.investment.application.dto.InvestmentResponse;
import com.keza.investment.application.dto.PortfolioResponse;
import com.keza.investment.application.usecase.InvestmentUseCase;
import com.keza.investment.application.usecase.PortfolioUseCase;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentUseCase investmentUseCase;
    private final PortfolioUseCase portfolioUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<InvestmentResponse>> createInvestment(
            Authentication authentication,
            @Valid @RequestBody CreateInvestmentRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        InvestmentResponse response = investmentUseCase.createInvestment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Investment created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvestmentResponse>> getInvestment(
            @PathVariable UUID id) {
        InvestmentResponse response = investmentUseCase.getInvestment(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<InvestmentResponse>>> getUserInvestments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InvestmentResponse> investments = investmentUseCase.getUserInvestments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(investments)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvestmentResponse>> cancelInvestment(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        InvestmentResponse response = investmentUseCase.cancelInvestment(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Investment cancelled successfully"));
    }

    @GetMapping("/campaigns/{campaignId}")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<PagedResponse<InvestmentResponse>>> getCampaignInvestments(
            @PathVariable UUID campaignId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID issuerId = (UUID) authentication.getPrincipal();
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InvestmentResponse> investments = investmentUseCase.getCampaignInvestments(campaignId, issuerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(investments)));
    }

    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        PortfolioResponse response = portfolioUseCase.getPortfolio(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/portfolio/export")
    public void exportPortfolioCsv(Authentication authentication, HttpServletResponse response)
            throws IOException {
        UUID userId = (UUID) authentication.getPrincipal();
        PortfolioResponse portfolio = portfolioUseCase.getPortfolio(userId);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"portfolio-export.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Investment ID,Campaign,Company,Industry,Amount (KES),Shares,Share Price,Status,Date");

        if (portfolio.getInvestments() != null) {
            for (InvestmentResponse inv : portfolio.getInvestments()) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\"%n",
                        inv.getId(),
                        escapeCsv(inv.getCampaignTitle()),
                        escapeCsv(inv.getCampaignCompanyName()),
                        escapeCsv(inv.getCampaignIndustry()),
                        inv.getAmount(),
                        inv.getShares(),
                        inv.getSharePrice(),
                        inv.getStatus(),
                        inv.getCreatedAt());
            }
        }

        writer.println();
        writer.printf("Total Invested,\"%s\"%n", portfolio.getTotalInvested());
        writer.printf("Active Investments,%d%n", portfolio.getActiveInvestments());
        writer.printf("Total Investment Count,%d%n", portfolio.getTotalInvestmentCount());
        if (portfolio.getRoiPercentage() != null) {
            writer.printf("ROI,\"%s%%\"%n", portfolio.getRoiPercentage());
        }

        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
