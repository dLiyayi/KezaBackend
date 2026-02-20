package com.keza.admin.domain.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin-local repository interface for querying users table via native SQL.
 * This avoids a direct dependency on keza-user module.
 */
@Repository
public interface AdminUserRepository {

    Page<Map<String, Object>> findUsers(String kycStatus, String userType, Boolean active, String search, Pageable pageable);

    Optional<Map<String, Object>> findUserById(UUID userId);

    int updateUserActive(UUID userId, boolean active);

    int updateUserLocked(UUID userId, boolean locked);

    List<Map<String, Object>> findUserRoles(UUID userId);

    void assignRole(UUID userId, String roleName);

    void removeRole(UUID userId, String roleName);

    long countUsers();

    long countUsersByType(String userType);

    long countUsersByKycStatus(String kycStatus);
}
