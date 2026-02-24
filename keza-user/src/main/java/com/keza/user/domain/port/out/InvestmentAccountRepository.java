package com.keza.user.domain.port.out;

import com.keza.common.enums.InvestmentAccountStatus;
import com.keza.user.domain.model.InvestmentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccount, UUID> {

    List<InvestmentAccount> findByUserIdAndDeletedFalse(UUID userId);

    Optional<InvestmentAccount> findByIdAndDeletedFalse(UUID id);

    Optional<InvestmentAccount> findByUserIdAndStatusAndDeletedFalse(UUID userId, InvestmentAccountStatus status);

    boolean existsByUserIdAndStatusAndDeletedFalse(UUID userId, InvestmentAccountStatus status);
}
