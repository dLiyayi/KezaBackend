package com.keza.ai.domain.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port interface for accessing cross-module data needed by fraud detection.
 * <p>
 * This port is defined in keza-ai so the fraud detection service can query
 * investment, user, and payment data without directly depending on those modules.
 * A default (stub) implementation is provided in keza-ai, while the real
 * implementation lives in keza-app which has access to all module repositories.
 */
public interface FraudDataPort {

    /**
     * Count the number of investments made by a user since the given timestamp.
     *
     * @param userId the user ID
     * @param since  the earliest timestamp to consider
     * @return the number of investments
     */
    long countInvestmentsByUserSince(UUID userId, Instant since);

    /**
     * Get the average investment amount for a user across all their investments.
     *
     * @param userId the user ID
     * @return the average amount, or BigDecimal.ZERO if the user has no investments
     */
    BigDecimal getAverageInvestmentAmount(UUID userId);

    /**
     * Count the number of failed payment transactions for a user since the given timestamp.
     *
     * @param userId the user ID
     * @param since  the earliest timestamp to consider
     * @return the number of failed payments
     */
    long countFailedPaymentsByUserSince(UUID userId, Instant since);

    /**
     * Count the number of (non-deleted) users sharing the given phone number.
     *
     * @param phone the phone number to check
     * @return the number of users with this phone
     */
    long countUsersWithPhone(String phone);

    /**
     * Count the number of (non-deleted) users sharing the given national ID.
     *
     * @param nationalId the national ID to check
     * @return the number of users with this national ID
     */
    long countUsersWithNationalId(String nationalId);
}
