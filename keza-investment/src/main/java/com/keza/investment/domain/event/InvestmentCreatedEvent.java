package com.keza.investment.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentCreatedEvent(
        UUID investmentId,
        UUID investorId,
        UUID campaignId,
        BigDecimal amount
) {
}
