package com.keza.ai.domain.service;

import com.keza.ai.domain.model.FraudAlert;
import com.keza.ai.domain.model.FraudAlertStatus;
import com.keza.ai.domain.model.FraudSeverity;
import com.keza.ai.domain.port.out.FraudAlertRepository;
import com.keza.ai.domain.port.out.FraudDataPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
 * Performs velocity checks, amount anomaly detection, duplicate account detection,
 * failed payment pattern analysis, and geographic anomaly stubs
 * to identify potentially fraudulent activity on the platform.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;
    private final FraudDataPort fraudDataPort;
    private final ObjectMapper objectMapper;

    // Configurable thresholds
    private static final int MAX_INVESTMENTS_PER_HOUR = 5;
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("1000000"); // 1M KES
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000000"); // 5M KES
    private static final BigDecimal MINIMUM_SUSPICIOUS_AMOUNT = new BigDecimal("50000"); // 50K KES
    private static final int AMOUNT_ANOMALY_MULTIPLIER = 3;
    private static final int MAX_FAILED_PAYMENTS_PER_DAY = 5;

    /**
     * Checks for fraud indicators during user registration.
     * Detects duplicate phone numbers and duplicate national IDs.
     *
     * @param userId     the newly registered user's ID
     * @param phone      the user's phone number (nullable)
     * @param nationalId the user's national ID (nullable)
     */
    @Transactional
    public void checkRegistration(UUID userId, String phone, String nationalId) {
        log.info("Running fraud detection checks for registration: user={}", userId);

        // Check for duplicate phone numbers
        if (phone != null && !phone.isBlank()) {
            long usersWithPhone = fraudDataPort.countUsersWithPhone(phone);
            if (usersWithPhone > 1) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("phone", phone);
                details.put("duplicateCount", usersWithPhone);
                details.put("checkTime", Instant.now().toString());

                createAlert(
                        userId,
                        "DUPLICATE_PHONE",
                        "HIGH",
                        String.format("Duplicate phone number detected: %d users share phone %s",
                                usersWithPhone, phone),
                        details
                );
            }
        }

        // Check for duplicate national IDs
        if (nationalId != null && !nationalId.isBlank()) {
            long usersWithNationalId = fraudDataPort.countUsersWithNationalId(nationalId);
            if (usersWithNationalId > 1) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("nationalId", nationalId);
                details.put("duplicateCount", usersWithNationalId);
                details.put("checkTime", Instant.now().toString());

                createAlert(
                        userId,
                        "DUPLICATE_NATIONAL_ID",
                        "CRITICAL",
                        String.format("Duplicate national ID detected: %d users share national ID %s",
                                usersWithNationalId, nationalId),
                        details
                );
            }
        }

        log.info("Registration fraud checks completed for user {}", userId);
    }

    /**
     * Backward-compatible overload for callers that do not provide phone/nationalId.
     *
     * @param userId the newly registered user's ID
     */
    @Transactional
    public void checkRegistration(UUID userId) {
        checkRegistration(userId, null, null);
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

        // 2. Amount anomaly: unusually large investment (absolute thresholds + relative to user average)
        checkAmountAnomaly(userId, amount);

        log.info("Investment fraud checks completed for user {}", userId);
    }

    /**
     * Checks for fraud indicators when a payment is initiated.
     * Detects patterns of repeated failed payments that may indicate fraud or abuse.
     *
     * @param userId        the user initiating the payment
     * @param transactionId the transaction ID
     */
    @Transactional
    public void checkPayment(UUID userId, UUID transactionId) {
        log.info("Running fraud detection checks for payment: user={}, transaction={}", userId, transactionId);

        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        long failedPayments = fraudDataPort.countFailedPaymentsByUserSince(userId, twentyFourHoursAgo);

        if (failedPayments > MAX_FAILED_PAYMENTS_PER_DAY) {
            // Avoid duplicate alerts: check if we already raised one recently
            long recentAlerts = fraudAlertRepository.countRecentAlerts(
                    userId, "FAILED_PAYMENT_PATTERN", twentyFourHoursAgo);
            if (recentAlerts == 0) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("failedPaymentsInLast24h", failedPayments);
                details.put("threshold", MAX_FAILED_PAYMENTS_PER_DAY);
                details.put("transactionId", transactionId.toString());
                details.put("checkTime", Instant.now().toString());

                createAlert(
                        userId,
                        "FAILED_PAYMENT_PATTERN",
                        "HIGH",
                        String.format("High payment failure rate: %d failed payments in the last 24 hours (threshold: %d)",
                                failedPayments, MAX_FAILED_PAYMENTS_PER_DAY),
                        details
                );
            }
        }

        log.info("Payment fraud checks completed for user {}", userId);
    }

    /**
     * Runs a daily batch fraud analysis.
     * Scheduled to execute at 2 AM daily.
     * <p>
     * This method can be extended to perform aggregate pattern detection,
     * cross-user analysis, and other computationally expensive checks
     * that are not suitable for real-time triggers.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(readOnly = true)
    public void runDailyBatchCheck() {
        log.info("Starting daily batch fraud detection analysis");

        // Placeholder for aggregate analysis:
        // - Detect coordinated investment patterns across users
        // - Flag accounts with consistently suspicious behavior over time
        // - Re-check geographic anomalies from collected IP data
        // - Generate summary reports for the compliance team

        log.info("Daily batch fraud detection analysis completed");
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

        // If we already raised a velocity alert in the last hour, skip
        long recentVelocityAlerts = fraudAlertRepository.countRecentAlerts(userId, "VELOCITY_VIOLATION", oneHourAgo);
        if (recentVelocityAlerts > 0) {
            return;
        }

        // Query actual investment count from the investment repository via the port
        long recentInvestments = fraudDataPort.countInvestmentsByUserSince(userId, oneHourAgo);

        if (recentInvestments >= MAX_INVESTMENTS_PER_HOUR) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("investmentsInLastHour", recentInvestments);
            details.put("threshold", MAX_INVESTMENTS_PER_HOUR);
            details.put("checkTime", Instant.now().toString());

            createAlert(
                    userId,
                    "VELOCITY_VIOLATION",
                    "HIGH",
                    String.format("User made %d investments in the last hour (threshold: %d)",
                            recentInvestments, MAX_INVESTMENTS_PER_HOUR),
                    details
            );
        }
    }

    private void checkAmountAnomaly(UUID userId, BigDecimal amount) {
        if (amount == null) return;

        // Absolute threshold checks
        if (amount.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) >= 0) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("amount", amount.toPlainString());
            details.put("threshold", VERY_HIGH_AMOUNT_THRESHOLD.toPlainString());
            details.put("currency", "KES");

            createAlert(
                    userId,
                    "AMOUNT_ANOMALY",
                    "CRITICAL",
                    String.format("Very high investment amount detected: KES %s (threshold: KES %s)",
                            amount.toPlainString(), VERY_HIGH_AMOUNT_THRESHOLD.toPlainString()),
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
                    String.format("High investment amount detected: KES %s (threshold: KES %s)",
                            amount.toPlainString(), HIGH_AMOUNT_THRESHOLD.toPlainString()),
                    details
            );
        }

        // Relative threshold check: flag if amount > 3x user's average AND above minimum suspicious amount
        if (amount.compareTo(MINIMUM_SUSPICIOUS_AMOUNT) > 0) {
            BigDecimal userAverage = fraudDataPort.getAverageInvestmentAmount(userId);
            if (userAverage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = userAverage.multiply(BigDecimal.valueOf(AMOUNT_ANOMALY_MULTIPLIER));
                if (amount.compareTo(threshold) > 0) {
                    // Avoid duplicate: check if we already raised a relative anomaly alert recently
                    Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
                    long recentAlerts = fraudAlertRepository.countRecentAlerts(
                            userId, "AMOUNT_ANOMALY_RELATIVE", oneHourAgo);
                    if (recentAlerts == 0) {
                        Map<String, Object> details = new LinkedHashMap<>();
                        details.put("amount", amount.toPlainString());
                        details.put("userAverage", userAverage.toPlainString());
                        details.put("multiplier", AMOUNT_ANOMALY_MULTIPLIER);
                        details.put("relativeThreshold", threshold.toPlainString());
                        details.put("currency", "KES");

                        createAlert(
                                userId,
                                "AMOUNT_ANOMALY_RELATIVE",
                                "MEDIUM",
                                String.format("Investment amount KES %s is %.1fx the user's average of KES %s",
                                        amount.toPlainString(),
                                        amount.doubleValue() / userAverage.doubleValue(),
                                        userAverage.toPlainString()),
                                details
                        );
                    }
                }
            }
        }
    }
}
