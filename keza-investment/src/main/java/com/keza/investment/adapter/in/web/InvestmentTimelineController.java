package com.keza.investment.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import com.keza.investment.application.dto.InvestmentEventResponse;
import com.keza.investment.application.usecase.InvestmentEventUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
public class InvestmentTimelineController {

    private final InvestmentEventUseCase investmentEventUseCase;

    @GetMapping("/{investmentId}/timeline")
    public ResponseEntity<ApiResponse<List<InvestmentEventResponse>>> getInvestmentTimeline(
            @PathVariable UUID investmentId) {
        List<InvestmentEventResponse> timeline = investmentEventUseCase.getInvestmentTimeline(investmentId);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<PagedResponse<InvestmentEventResponse>>> getUserActivityFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = (UUID) authentication.getPrincipal();
        size = Math.min(size, 100);
        Page<InvestmentEventResponse> events = investmentEventUseCase.getUserActivityFeed(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(events)));
    }
}
