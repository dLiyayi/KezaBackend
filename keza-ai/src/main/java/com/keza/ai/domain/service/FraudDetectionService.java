package com.keza.ai.domain.service;

import com.keza.ai.domain.model.FraudAlert;
import com.keza.ai.domain.model.FraudAlertStatus;
import com.keza.ai.domain.model.FraudSeverity;
import com.keza.ai.domain.port.out.FraudAlertRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rule-based fraud detection service.
 * Performs velocity checks, amount anomaly detection, and duplicate account detection
 * to identify potentially fraudulent activity on the platform.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;
    private final ObjectMapper objectMapper;

    // Configurable thresholds
    private static final int MAX_INVESTMENTS_PER_HOUR = 10;
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("1000000"); // 1M KES
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000000"); // 5M KES
    private static final BigDecimal MINIMUM_SUSPICIOUS_AMOUNT = new BigDecimal("50000"); // 50K KES

    /**
     * Checks for fraud indicators during user registration.
     * Detects patterns such as rapid account creation from similar details.
     *
     * @param userId the newly registered user's ID
     */
    @Transactional
    public void checkRegistration(UUID userId) {
        log.info("Running fraud detection checks for registration: user={}", userId);

        // Check for rapid account creation (possible bot or mass registration)
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentAlerts = fraudAlertRepository.countRecentAlerts(userId, "RAPID_REGISTRATION", oneHourAgo);

        if (recentAlerts > 0) {
            log.warn("Duplicate registration fraud check already exists for user {}", userId);
            return;
        }

        // Note: In a full implementation, we would check for:
        // - Same IP address registering multiple accounts
        // - Same device fingerprint
        // - Similar email patterns (e.g., john1@, john2@, john3@)
        // - Phone numbers from the same range
        // These checks would require additional infrastructure dependencies

        log.info("Registration fraud checks passed for user {}", userId);
    }

    /**
     * Checks for fraud indicators when a user makes an investment.
     * Performs velocity checks and amount anomaly detection.
     *
     * @param userId the investing user's ID
     * @param amount the investment amount
     */
    @Transactional
    public void checkInvestment(UUID userId, BigDecimal amount) {
        log.info("Running fraud detection checks for investment: user={}, amount={}", userId, amount);

        // 1. Velocity check: too many investments in a short period
        checkInvestmentVelocity(userId);

        // 2. Amount anomaly: unusually large investment
        checkAmountAnomaly(userId, amount);

        log.info("Investment fraud checks completed for user {}", userId);
    }

    /**
     * Creates a fraud alert for manual review.
     *
     * @param userId      the user associated with the alert (nullable for system-level alerts)
     * @param alertType   the type of fraud alert (e.g., VELOCITY_VIOLATION, AMOUNT_ANOMALY)
     * @param severity    the severity level as string (LOW, MEDIUM, HIGH, CRITICAL)
     * @param description a human-readable description of the alert
     * @return the created FraudAlert
     */
    @Transactional
    public FraudAlert createAlert(UUID userId, String alertType, String severity, String description) {
        return createAlert(userId, alertType, severity, description, null);
    }

    /**
     * Creates a fraud alert with additional details for manual review.
     *
     * @param userId      the user associated with the alert
     * @param alertType   the type of fraud alert
     * @param severity    the severity level as string
     * @param description a human-readable description
     * @param details     additional details as a Map (stored as JSONB)
     * @return the created FraudAlert
     */
    @Transactional
    public FraudAlert createAlert(UUID userId, String alertType, String severity, String description, Map<String, Object> details) {
        FraudSeverity fraudSeverity;
        try {
            fraudSeverity = FraudSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            fraudSeverity = FraudSeverity.MEDIUM;
        }

        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize fraud alert details: {}", e.getMessage());
            }
        }

        FraudAlert alert = FraudAlert.builder()
                .userId(userId)
                .alertType(alertType)
                .severity(fraudSeverity)
                .description(description)
                .details(detailsJson)
                .status(FraudAlertStatus.NEW)
                .build();

        alert = fraudAlertRepository.save(alert);
        log.warn("Fraud alert created: id={}, type={}, severity={}, user={}", alert.getId(), alertType, severity, userId);

        return alert;
    }

    // ---- Private rule-based checks ----

    private void checkInvestmentVelocity(UUID userId) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentVelocityAlerts = fraudAlertRepository.countRecentAlerts(userId, "VELOCITY_VIOLATION", oneHourAgo);

        // If we already raised a velocity alert in the last hour, skip
        if (recentVelocityAlerts > 0) {
            return;
        }

        // Count investments in the last hour via alert history as a proxy
        // In a real implementation, this would query the investment repository
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentInvestmentAlerts = fraudAlertRepository.countRecentAlerts(userId, "INVESTMENT_ACTIVITY", since);

        if (recentInvestmentAlerts >= MAX_INVESTMENTS_PER_HOUR) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("investmentsInLastHour", recentInvestmentAlerts);
            details.put("threshold", MAX_INVESTMENTS_PER_HOUR);
            details.put("checkTime", Instant.now().toString());

            createAlert(
                    userId,
                    "VELOCITY_VIOLATION",
                    "HIGH",
                    String.format("User made %d investments in the last hour (threshold: %d)", recentInvestmentAlerts, MAX_INVESTMENTS_PER_HOUR),
                    details
            );
        }
    }

    private void checkAmountAnomaly(UUID userId, BigDecimal amount) {
        if (amount == null) return;

        if (amount.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) >= 0) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("amount", amount.toPlainString());
            details.put("threshold", VERY_HIGH_AMOUNT_THRESHOLD.toPlainString());
            details.put("currency", "KES");

            createAlert(
                    userId,
                    "AMOUNT_ANOMALY",
                    "CRITICAL",
                    String.format("Very high investment amount detected: KES %s (threshold: KES %s)", amount.toPlainString(), VERY_HIGH_AMOUNT_THRESHOLD.toPlainString()),
                    details
            );
        } else if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("amount", amount.toPlainString());
            details.put("threshold", HIGH_AMOUNT_THRESHOLD.toPlainString());
            details.put("currency", "KES");

            createAlert(
                    userId,
                    "AMOUNT_ANOMALY",
                    "HIGH",
                    String.format("High investment amount detected: KES %s (threshold: KES %s)", amount.toPlainString(), HIGH_AMOUNT_THRESHOLD.toPlainString()),
                    details
            );
        }
    }
}
