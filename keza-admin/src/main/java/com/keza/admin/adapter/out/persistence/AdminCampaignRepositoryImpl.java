package com.keza.admin.adapter.out.persistence;

import com.keza.admin.domain.port.out.AdminCampaignRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Repository
@Transactional(readOnly = true)
public class AdminCampaignRepositoryImpl implements AdminCampaignRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Map<String, Object>> findCampaigns(String status, String industry, String search, Pageable pageable) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.id, c.title, c.slug, c.company_name, c.industry, c.status, " +
                "c.target_amount, c.raised_amount, c.investor_count, c.issuer_id, " +
                "c.start_date, c.end_date, c.created_at, c.updated_at, " +
                "u.first_name AS issuer_first_name, u.last_name AS issuer_last_name, u.email AS issuer_email " +
                "FROM campaigns c " +
                "LEFT JOIN users u ON c.issuer_id = u.id " +
                "WHERE c.deleted = false ");

        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM campaigns c WHERE c.deleted = false ");

        Map<String, Object> params = new LinkedHashMap<>();

        if (status != null && !status.isBlank()) {
            sql.append("AND c.status = :status ");
            countSql.append("AND c.status = :status ");
            params.put("status", status);
        }
        if (industry != null && !industry.isBlank()) {
            sql.append("AND c.industry = :industry ");
            countSql.append("AND c.industry = :industry ");
            params.put("industry", industry);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(c.title) LIKE :search OR LOWER(c.company_name) LIKE :search) ");
            countSql.append("AND (LOWER(c.title) LIKE :search OR LOWER(c.company_name) LIKE :search) ");
            params.put("search", "%" + search.toLowerCase() + "%");
        }

        sql.append("ORDER BY c.created_at DESC LIMIT :limit OFFSET :offset");

        Query query = em.createNativeQuery(sql.toString());
        Query countQuery = em.createNativeQuery(countSql.toString());

        params.forEach((k, v) -> {
            query.setParameter(k, v);
            countQuery.setParameter(k, v);
        });
        query.setParameter("limit", pageable.getPageSize());
        query.setParameter("offset", (int) pageable.getOffset());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        List<Map<String, Object>> results = rows.stream().map(this::mapCampaignRow).toList();
        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public Optional<Map<String, Object>> findCampaignById(UUID campaignId) {
        String sql = "SELECT c.id, c.title, c.slug, c.company_name, c.industry, c.status, " +
                "c.target_amount, c.raised_amount, c.investor_count, c.issuer_id, " +
                "c.start_date, c.end_date, c.created_at, c.updated_at, " +
                "u.first_name AS issuer_first_name, u.last_name AS issuer_last_name, u.email AS issuer_email " +
                "FROM campaigns c " +
                "LEFT JOIN users u ON c.issuer_id = u.id " +
                "WHERE c.id = :id AND c.deleted = false";

        Query query = em.createNativeQuery(sql);
        query.setParameter("id", campaignId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapCampaignRow(rows.get(0)));
    }

    @Override
    @Transactional
    public int assignReviewer(UUID campaignId, UUID reviewerId) {
        String sql = "UPDATE campaigns SET updated_by = :reviewerId, updated_at = NOW() " +
                "WHERE id = :campaignId AND deleted = false AND status = 'REVIEW'";
        Query query = em.createNativeQuery(sql);
        query.setParameter("reviewerId", reviewerId.toString());
        query.setParameter("campaignId", campaignId);
        return query.executeUpdate();
    }

    @Override
    public long countCampaignsByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM campaigns WHERE status = :status AND deleted = false";
        Query query = em.createNativeQuery(sql);
        query.setParameter("status", status);
        return ((Number) query.getSingleResult()).longValue();
    }

    private Map<String, Object> mapCampaignRow(Object[] row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row[0]);
        map.put("title", row[1]);
        map.put("slug", row[2]);
        map.put("companyName", row[3]);
        map.put("industry", row[4]);
        map.put("status", row[5]);
        map.put("targetAmount", row[6]);
        map.put("raisedAmount", row[7]);
        map.put("investorCount", row[8]);
        map.put("issuerId", row[9]);
        map.put("startDate", row[10]);
        map.put("endDate", row[11]);
        map.put("createdAt", row[12]);
        map.put("updatedAt", row[13]);
        map.put("issuerFirstName", row[14]);
        map.put("issuerLastName", row[15]);
        map.put("issuerEmail", row[16]);
        return map;
    }
}
