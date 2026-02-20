package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.FraudAlertResponse;
import com.keza.ai.domain.model.FraudAlert;
import com.keza.ai.domain.model.FraudAlertStatus;
import com.keza.ai.domain.port.out.FraudAlertRepository;
import com.keza.common.dto.PagedResponse;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAlertUseCase {

    private final FraudAlertRepository fraudAlertRepository;

    /**
     * Returns a paginated list of fraud alerts, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public PagedResponse<FraudAlertResponse> getAlerts(FraudAlertStatus status, Pageable pageable) {
        Page<FraudAlert> page;

        if (status != null) {
            page = fraudAlertRepository.findByStatus(status, pageable);
        } else {
            page = fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Page<FraudAlertResponse> responsePage = page.map(this::mapToResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Returns a single fraud alert by ID.
     */
    @Transactional(readOnly = true)
    public FraudAlertResponse getAlert(UUID id) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", id));
        return mapToResponse(alert);
    }

    /**
     * Updates the status of a fraud alert (admin action).
     */
    @Transactional
    public FraudAlertResponse updateAlertStatus(UUID alertId, String status, String notes, UUID adminId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", alertId));

        FraudAlertStatus newStatus;
        try {
            newStatus = FraudAlertStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_STATUS",
                    "Invalid fraud alert status: " + status + ". Valid values: NEW, INVESTIGATING, RESOLVED, DISMISSED");
        }

        // Validate state transitions
        validateStatusTransition(alert.getStatus(), newStatus);

        alert.setStatus(newStatus);

        if (newStatus == FraudAlertStatus.RESOLVED || newStatus == FraudAlertStatus.DISMISSED) {
            alert.setResolvedBy(adminId);
            alert.setResolvedAt(Instant.now());
            alert.setResolutionNotes(notes);
        }

        if (notes != null && !notes.isBlank()) {
            alert.setResolutionNotes(notes);
        }

        alert = fraudAlertRepository.save(alert);
        log.info("Fraud alert {} status updated to {} by admin {}", alertId, newStatus, adminId);

        return mapToResponse(alert);
    }

    // ---- Private helpers ----

    private void validateStatusTransition(FraudAlertStatus current, FraudAlertStatus target) {
        // Prevent re-opening resolved/dismissed alerts
        if ((current == FraudAlertStatus.RESOLVED || current == FraudAlertStatus.DISMISSED)
                && target == FraudAlertStatus.NEW) {
            throw new BusinessRuleException("INVALID_TRANSITION",
                    "Cannot transition from " + current + " back to NEW");
        }
    }

    private FraudAlertResponse mapToResponse(FraudAlert alert) {
        return FraudAlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity().name())
                .description(alert.getDescription())
                .details(alert.getDetails())
                .status(alert.getStatus().name())
                .resolvedBy(alert.getResolvedBy())
                .resolvedAt(alert.getResolvedAt())
                .resolutionNotes(alert.getResolutionNotes())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
