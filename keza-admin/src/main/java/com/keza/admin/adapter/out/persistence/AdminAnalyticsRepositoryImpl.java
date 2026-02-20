package com.keza.admin.adapter.out.persistence;

import com.keza.admin.domain.port.out.AdminAnalyticsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Repository
@Transactional(readOnly = true)
public class AdminAnalyticsRepositoryImpl implements AdminAnalyticsRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public long countTotalUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countInvestors() {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type = 'INVESTOR' AND deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countIssuers() {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type = 'ISSUER' AND deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countTotalCampaigns() {
        String sql = "SELECT COUNT(*) FROM campaigns";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countActiveCampaigns() {
        String sql = "SELECT COUNT(*) FROM campaigns WHERE status = 'ACTIVE'";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public BigDecimal sumTotalInvested() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM investments WHERE status IN ('CONFIRMED', 'COMPLETED')";
        Query query = em.createNativeQuery(sql);
        Object result = query.getSingleResult();
        if (result instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(result.toString());
    }

    @Override
    public long countPendingKyc() {
        String sql = "SELECT COUNT(*) FROM users WHERE kyc_status IN ('PENDING', 'SUBMITTED', 'IN_REVIEW') AND deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countPendingCampaigns() {
        String sql = "SELECT COUNT(*) FROM campaigns WHERE status IN ('DRAFT', 'UNDER_REVIEW')";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }
}
