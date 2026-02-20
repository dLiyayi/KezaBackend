package com.keza.admin.domain.port.out;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * Admin-local repository interface for analytics queries via native SQL.
 * Queries users, campaigns, and investments tables directly.
 */
@Repository
public interface AdminAnalyticsRepository {

    long countTotalUsers();

    long countInvestors();

    long countIssuers();

    long countTotalCampaigns();

    long countActiveCampaigns();

    BigDecimal sumTotalInvested();

    long countPendingKyc();

    long countPendingCampaigns();
}
