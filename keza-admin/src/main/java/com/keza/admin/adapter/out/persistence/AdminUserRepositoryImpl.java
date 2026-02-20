package com.keza.admin.adapter.out.persistence;

import com.keza.admin.domain.port.out.AdminUserRepository;
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
public class AdminUserRepositoryImpl implements AdminUserRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Map<String, Object>> findUsers(String kycStatus, String userType, Boolean active, String search, Pageable pageable) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.email, u.phone, u.first_name, u.last_name, u.user_type, " +
                "u.kyc_status, u.email_verified, u.phone_verified, u.active, u.locked, " +
                "u.profile_image_url, u.bio, u.created_at " +
                "FROM users u WHERE u.deleted = false ");

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM users u WHERE u.deleted = false ");

        Map<String, Object> params = new LinkedHashMap<>();

        if (kycStatus != null && !kycStatus.isBlank()) {
            sql.append("AND u.kyc_status = :kycStatus ");
            countSql.append("AND u.kyc_status = :kycStatus ");
            params.put("kycStatus", kycStatus);
        }
        if (userType != null && !userType.isBlank()) {
            sql.append("AND u.user_type = :userType ");
            countSql.append("AND u.user_type = :userType ");
            params.put("userType", userType);
        }
        if (active != null) {
            sql.append("AND u.active = :active ");
            countSql.append("AND u.active = :active ");
            params.put("active", active);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(u.email) LIKE :search) ");
            countSql.append("AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(u.email) LIKE :search) ");
            params.put("search", "%" + search.toLowerCase() + "%");
        }

        sql.append("ORDER BY u.created_at DESC ");
        sql.append("LIMIT :limit OFFSET :offset");

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

        List<Map<String, Object>> results = rows.stream().map(this::mapUserRow).toList();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public Optional<Map<String, Object>> findUserById(UUID userId) {
        String sql = "SELECT u.id, u.email, u.phone, u.first_name, u.last_name, u.user_type, " +
                "u.kyc_status, u.email_verified, u.phone_verified, u.active, u.locked, " +
                "u.profile_image_url, u.bio, u.created_at " +
                "FROM users u WHERE u.id = :userId AND u.deleted = false";

        Query query = em.createNativeQuery(sql);
        query.setParameter("userId", userId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapUserRow(rows.get(0)));
    }

    @Override
    @Transactional
    public int updateUserActive(UUID userId, boolean active) {
        String sql = "UPDATE users SET active = :active, updated_at = NOW() WHERE id = :userId AND deleted = false";
        Query query = em.createNativeQuery(sql);
        query.setParameter("active", active);
        query.setParameter("userId", userId);
        return query.executeUpdate();
    }

    @Override
    @Transactional
    public int updateUserLocked(UUID userId, boolean locked) {
        String sql = "UPDATE users SET locked = :locked, locked_until = NULL, updated_at = NOW() WHERE id = :userId AND deleted = false";
        Query query = em.createNativeQuery(sql);
        query.setParameter("locked", locked);
        query.setParameter("userId", userId);
        return query.executeUpdate();
    }

    @Override
    public List<Map<String, Object>> findUserRoles(UUID userId) {
        String sql = "SELECT r.id, r.name, r.description FROM roles r " +
                "JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = :userId";
        Query query = em.createNativeQuery(sql);
        query.setParameter("userId", userId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", row[0]);
            map.put("name", row[1]);
            map.put("description", row[2]);
            return map;
        }).toList();
    }

    @Override
    @Transactional
    public void assignRole(UUID userId, String roleName) {
        String sql = "INSERT INTO user_roles (user_id, role_id) " +
                "SELECT :userId, r.id FROM roles r WHERE r.name = :roleName " +
                "ON CONFLICT DO NOTHING";
        Query query = em.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("roleName", roleName);
        query.executeUpdate();
    }

    @Override
    @Transactional
    public void removeRole(UUID userId, String roleName) {
        String sql = "DELETE FROM user_roles WHERE user_id = :userId " +
                "AND role_id = (SELECT r.id FROM roles r WHERE r.name = :roleName)";
        Query query = em.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("roleName", roleName);
        query.executeUpdate();
    }

    @Override
    public long countUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE deleted = false";
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @Override
    public long countUsersByType(String userType) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type = :userType AND deleted = false";
        Query query = em.createNativeQuery(sql);
        query.setParameter("userType", userType);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public long countUsersByKycStatus(String kycStatus) {
        String sql = "SELECT COUNT(*) FROM users WHERE kyc_status = :kycStatus AND deleted = false";
        Query query = em.createNativeQuery(sql);
        query.setParameter("kycStatus", kycStatus);
        return ((Number) query.getSingleResult()).longValue();
    }

    private Map<String, Object> mapUserRow(Object[] row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row[0]);
        map.put("email", row[1]);
        map.put("phone", row[2]);
        map.put("firstName", row[3]);
        map.put("lastName", row[4]);
        map.put("userType", row[5]);
        map.put("kycStatus", row[6]);
        map.put("emailVerified", row[7]);
        map.put("phoneVerified", row[8]);
        map.put("active", row[9]);
        map.put("locked", row[10]);
        map.put("profileImageUrl", row[11]);
        map.put("bio", row[12]);
        map.put("createdAt", row[13]);
        return map;
    }
}
