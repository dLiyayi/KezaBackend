package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.AdminUserResponse;
import com.keza.admin.application.dto.AdminUserSearchCriteria;
import com.keza.admin.domain.port.out.AdminUserRepository;
import com.keza.common.dto.PagedResponse;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.audit.Audited;
import com.keza.infrastructure.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserUseCase {

    private final AdminUserRepository adminUserRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> listUsers(AdminUserSearchCriteria criteria, Pageable pageable) {
        Page<Map<String, Object>> page = adminUserRepository.findUsers(
                criteria.getKycStatus(),
                criteria.getUserType(),
                criteria.getActive(),
                criteria.getSearch(),
                pageable
        );

        List<AdminUserResponse> content = page.getContent().stream()
                .map(this::mapToAdminUserResponse)
                .toList();

        return PagedResponse.<AdminUserResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        Map<String, Object> userData = adminUserRepository.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        AdminUserResponse response = mapToAdminUserResponse(userData);

        // Enrich with roles
        List<Map<String, Object>> roles = adminUserRepository.findUserRoles(userId);
        Set<String> roleNames = roles.stream()
                .map(r -> (String) r.get("name"))
                .collect(Collectors.toSet());
        response.setRoles(roleNames);

        return response;
    }

    @Audited(action = "ACTIVATE_USER", entityType = "User")
    @Transactional
    public AdminUserResponse activateUser(UUID userId) {
        int updated = adminUserRepository.updateUserActive(userId, true);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", userId);
        }
        auditLogger.log("ACTIVATE_USER", "User", userId.toString(), "User activated by admin");
        return getUser(userId);
    }

    @Audited(action = "DEACTIVATE_USER", entityType = "User")
    @Transactional
    public AdminUserResponse deactivateUser(UUID userId) {
        int updated = adminUserRepository.updateUserActive(userId, false);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", userId);
        }
        auditLogger.log("DEACTIVATE_USER", "User", userId.toString(), "User deactivated by admin");
        return getUser(userId);
    }

    @Audited(action = "LOCK_USER", entityType = "User")
    @Transactional
    public AdminUserResponse lockUser(UUID userId) {
        int updated = adminUserRepository.updateUserLocked(userId, true);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", userId);
        }
        auditLogger.log("LOCK_USER", "User", userId.toString(), "User locked by admin");
        return getUser(userId);
    }

    @Audited(action = "UNLOCK_USER", entityType = "User")
    @Transactional
    public AdminUserResponse unlockUser(UUID userId) {
        int updated = adminUserRepository.updateUserLocked(userId, false);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", userId);
        }
        auditLogger.log("UNLOCK_USER", "User", userId.toString(), "User unlocked by admin");
        return getUser(userId);
    }

    @Audited(action = "ASSIGN_ROLE", entityType = "User")
    @Transactional
    public AdminUserResponse assignRole(UUID userId, String roleName) {
        // Verify user exists
        adminUserRepository.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        adminUserRepository.assignRole(userId, roleName.toUpperCase());
        auditLogger.log("ASSIGN_ROLE", "User", userId.toString(),
                null, roleName, "Role assigned by admin");
        return getUser(userId);
    }

    @Audited(action = "REMOVE_ROLE", entityType = "User")
    @Transactional
    public AdminUserResponse removeRole(UUID userId, String roleName) {
        // Verify user exists
        adminUserRepository.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        adminUserRepository.removeRole(userId, roleName.toUpperCase());
        auditLogger.log("REMOVE_ROLE", "User", userId.toString(),
                roleName, null, "Role removed by admin");
        return getUser(userId);
    }

    private AdminUserResponse mapToAdminUserResponse(Map<String, Object> data) {
        return AdminUserResponse.builder()
                .id((UUID) data.get("id"))
                .email((String) data.get("email"))
                .phone((String) data.get("phone"))
                .firstName((String) data.get("firstName"))
                .lastName((String) data.get("lastName"))
                .userType((String) data.get("userType"))
                .kycStatus((String) data.get("kycStatus"))
                .emailVerified(Boolean.TRUE.equals(data.get("emailVerified")))
                .phoneVerified(Boolean.TRUE.equals(data.get("phoneVerified")))
                .active(Boolean.TRUE.equals(data.get("active")))
                .locked(Boolean.TRUE.equals(data.get("locked")))
                .profileImageUrl((String) data.get("profileImageUrl"))
                .bio((String) data.get("bio"))
                .createdAt(data.get("createdAt") instanceof Instant inst ? inst : null)
                .build();
    }
}
