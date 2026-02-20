package com.keza.ai.adapter.in.web;

import com.keza.ai.application.dto.FraudAlertResponse;
import com.keza.ai.application.usecase.FraudAlertUseCase;
import com.keza.ai.domain.model.FraudAlertStatus;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/fraud-alerts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAlertUseCase fraudAlertUseCase;

    /**
     * Lists fraud alerts with optional status filter and pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<FraudAlertResponse>>> listAlerts(
            @RequestParam(value = "status", required = false) FraudAlertStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<FraudAlertResponse> alerts = fraudAlertUseCase.getAlerts(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Gets a single fraud alert by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> getAlert(@PathVariable("id") UUID id) {
        FraudAlertResponse alert = fraudAlertUseCase.getAlert(id);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    /**
     * Updates the status of a fraud alert. Requires ADMIN role.
     * Request body should contain "status" (required) and "notes" (optional).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> updateStatus(
            Authentication authentication,
            @PathVariable("id") UUID alertId,
            @RequestBody Map<String, String> request) {
        UUID adminId = (UUID) authentication.getPrincipal();
        String status = request.get("status");
        String notes = request.get("notes");

        FraudAlertResponse response = fraudAlertUseCase.updateAlertStatus(alertId, status, notes, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Fraud alert status updated"));
    }
}
