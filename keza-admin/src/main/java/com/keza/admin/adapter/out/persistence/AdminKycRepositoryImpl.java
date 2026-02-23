package com.keza.admin.adapter.out.persistence;

import com.keza.admin.domain.port.out.AdminKycRepository;
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
public class AdminKycRepositoryImpl implements AdminKycRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Map<String, Object>> findKycDocuments(String status, String documentType, String search, Pageable pageable) {
        StringBuilder sql = new StringBuilder(
                "SELECT d.id, d.user_id, d.document_type, d.file_name, d.file_size, d.content_type, " +
                "d.status, d.rejection_reason, d.ai_confidence_score, d.reviewed_by, d.reviewed_at, " +
                "d.created_at, d.updated_at, " +
                "u.first_name, u.last_name, u.email " +
                "FROM kyc_documents d " +
                "LEFT JOIN users u ON d.user_id = u.id " +
                "WHERE 1=1 ");

        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM kyc_documents d " +
                "LEFT JOIN users u ON d.user_id = u.id " +
                "WHERE 1=1 ");

        Map<String, Object> params = new LinkedHashMap<>();

        if (status != null && !status.isBlank()) {
            sql.append("AND d.status = :status ");
            countSql.append("AND d.status = :status ");
            params.put("status", status);
        }
        if (documentType != null && !documentType.isBlank()) {
            sql.append("AND d.document_type = :documentType ");
            countSql.append("AND d.document_type = :documentType ");
            params.put("documentType", documentType);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(u.email) LIKE :search) ");
            countSql.append("AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(u.email) LIKE :search) ");
            params.put("search", "%" + search.toLowerCase() + "%");
        }

        sql.append("ORDER BY d.created_at DESC LIMIT :limit OFFSET :offset");

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

        List<Map<String, Object>> results = rows.stream().map(this::mapKycRow).toList();
        return new PageImpl<>(results, pageable, total);
    }

    private Map<String, Object> mapKycRow(Object[] row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row[0]);
        map.put("userId", row[1]);
        map.put("documentType", row[2]);
        map.put("fileName", row[3]);
        map.put("fileSize", row[4]);
        map.put("contentType", row[5]);
        map.put("status", row[6]);
        map.put("rejectionReason", row[7]);
        map.put("aiConfidenceScore", row[8]);
        map.put("reviewedBy", row[9]);
        map.put("reviewedAt", row[10]);
        map.put("createdAt", row[11]);
        map.put("updatedAt", row[12]);
        map.put("userFirstName", row[13]);
        map.put("userLastName", row[14]);
        map.put("userEmail", row[15]);
        return map;
    }
}
