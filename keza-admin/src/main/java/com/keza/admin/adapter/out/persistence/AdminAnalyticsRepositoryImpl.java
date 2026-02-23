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

    // Investment analytics

    @Override
    public long countTotalInvestments() {
        String sql = "SELECT COUNT(*) FROM investments";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countInvestmentsByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM investments WHERE status = :status";
        Query query = em.createNativeQuery(sql);
        query.setParameter("status", status);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public BigDecimal averageInvestmentAmount() {
        String sql = "SELECT COALESCE(AVG(amount), 0) FROM investments WHERE status IN ('COMPLETED', 'COOLING_OFF', 'PENDING')";
        Object result = em.createNativeQuery(sql).getSingleResult();
        return result instanceof BigDecimal bd ? bd : new BigDecimal(result.toString());
    }

    @Override
    public long countUniqueInvestors() {
        String sql = "SELECT COUNT(DISTINCT investor_id) FROM investments";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    // Campaign analytics

    @Override
    public long countCampaignsByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM campaigns WHERE status = :status AND deleted = false";
        Query query = em.createNativeQuery(sql);
        query.setParameter("status", status);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public BigDecimal sumTotalRaisedAmount() {
        String sql = "SELECT COALESCE(SUM(raised_amount), 0) FROM campaigns WHERE deleted = false";
        Object result = em.createNativeQuery(sql).getSingleResult();
        return result instanceof BigDecimal bd ? bd : new BigDecimal(result.toString());
    }

    @Override
    public BigDecimal averageFundingPercentage() {
        String sql = "SELECT COALESCE(AVG(CASE WHEN target_amount > 0 THEN (raised_amount / target_amount) * 100 ELSE 0 END), 0) " +
                "FROM campaigns WHERE status IN ('LIVE', 'FUNDED', 'CLOSED') AND deleted = false";
        Object result = em.createNativeQuery(sql).getSingleResult();
        return result instanceof BigDecimal bd ? bd : new BigDecimal(result.toString());
    }

    // User analytics

    @Override
    public long countVerifiedUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE email_verified = true AND deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countLockedUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE locked = true AND deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countUsersByKycStatus(String kycStatus) {
        String sql = "SELECT COUNT(*) FROM users WHERE kyc_status = :kycStatus AND deleted = false";
        Query query = em.createNativeQuery(sql);
        query.setParameter("kycStatus", kycStatus);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public long countRegistrationsSince(int days) {
        String sql = "SELECT COUNT(*) FROM users WHERE created_at >= NOW() - INTERVAL '" + days + " days' AND deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }
}
