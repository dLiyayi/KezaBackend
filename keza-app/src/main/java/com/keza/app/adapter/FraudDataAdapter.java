package com.keza.app.adapter;

import com.keza.ai.domain.port.out.FraudDataPort;
import com.keza.common.enums.TransactionStatus;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.investment.domain.port.out.TransactionRepository;
import com.keza.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Real implementation of {@link FraudDataPort} that queries actual repositories.
 * <p>
 * This adapter lives in keza-app (which depends on all modules) and provides
 * real data for fraud detection by querying InvestmentRepository, UserRepository,
 * and TransactionRepository. Marked as {@code @Primary} so it takes precedence
 * over the default stub in keza-ai.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class FraudDataAdapter implements FraudDataPort {

    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public long countInvestmentsByUserSince(UUID userId, Instant since) {
        return investmentRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("investorId"), userId),
                        cb.greaterThan(root.get("createdAt"), since)
                )
        );
    }

    @Override
    public BigDecimal getAverageInvestmentAmount(UUID userId) {
        var cb = investmentRepository.count(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("investorId"), userId)
        );
        if (cb == 0) {
            return BigDecimal.ZERO;
        }

        // Use Specification to compute the average via findAll + stream.
        // For production scale, consider adding a custom @Query method with AVG().
        Specification<Investment> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("investorId"), userId);

        List<Investment> investments = investmentRepository.findAll(spec);

        return investments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(investments.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public long countFailedPaymentsByUserSince(UUID userId, Instant since) {
        return transactionRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("userId"), userId),
                        cb.equal(root.get("status"), TransactionStatus.FAILED),
                        cb.greaterThan(root.get("createdAt"), since)
                )
        );
    }

    @Override
    public long countUsersWithPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return 0;
        }
        return userRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("phone"), phone),
                        cb.isFalse(root.get("deleted"))
                )
        );
    }

    @Override
    public long countUsersWithNationalId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            return 0;
        }
        return userRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("nationalId"), nationalId),
                        cb.isFalse(root.get("deleted"))
                )
        );
    }
}
