package com.keza.investment.domain.port.out;

import com.keza.investment.domain.model.InvestmentEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentEventRepository extends JpaRepository<InvestmentEvent, UUID> {

    Page<InvestmentEvent> findByInvestmentIdOrderByCreatedAtDesc(UUID investmentId, Pageable pageable);

    Page<InvestmentEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<InvestmentEvent> findByInvestmentIdOrderByCreatedAtAsc(UUID investmentId);
}
