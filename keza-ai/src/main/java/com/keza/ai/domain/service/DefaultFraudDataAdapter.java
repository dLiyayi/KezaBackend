package com.keza.ai.domain.service;

import com.keza.ai.domain.port.out.FraudDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Default (stub) implementation of {@link FraudDataPort} that returns safe defaults.
 * <p>
 * This adapter is activated only when no other {@link FraudDataPort} bean is present,
 * allowing the keza-ai module to function standalone (e.g. in tests or when the
 * full application context is not assembled). The real implementation in keza-app
 * is marked as {@code @Primary} and will take precedence at runtime.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(FraudDataPort.class)
public class DefaultFraudDataAdapter implements FraudDataPort {

    public DefaultFraudDataAdapter() {
        log.warn("Using DefaultFraudDataAdapter - fraud detection data queries will return safe defaults. "
                + "Provide a real FraudDataPort implementation for production use.");
    }

    @Override
    public long countInvestmentsByUserSince(UUID userId, Instant since) {
        return 0;
    }

    @Override
    public BigDecimal getAverageInvestmentAmount(UUID userId) {
        return BigDecimal.ZERO;
    }

    @Override
    public long countFailedPaymentsByUserSince(UUID userId, Instant since) {
        return 0;
    }

    @Override
    public long countUsersWithPhone(String phone) {
        return 0;
    }

    @Override
    public long countUsersWithNationalId(String nationalId) {
        return 0;
    }
}
