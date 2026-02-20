package com.keza.admin.application.usecase;

import com.keza.admin.application.dto.AdminUserResponse;
import com.keza.admin.application.dto.AdminUserSearchCriteria;
import com.keza.admin.domain.port.out.AdminUserRepository;
import com.keza.common.dto.PagedResponse;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.audit.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserUseCase")
class AdminUserUseCaseTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdminUserUseCase adminUserUseCase;

    private UUID userId;
    private Map<String, Object> sampleUserData;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUserData = new LinkedHashMap<>();
        sampleUserData.put("id", userId);
        sampleUserData.put("email", "john@example.com");
        sampleUserData.put("phone", "+254712345678");
        sampleUserData.put("firstName", "John");
        sampleUserData.put("lastName", "Doe");
        sampleUserData.put("userType", "INVESTOR");
        sampleUserData.put("kycStatus", "VERIFIED");
        sampleUserData.put("emailVerified", true);
        sampleUserData.put("phoneVerified", true);
        sampleUserData.put("active", true);
        sampleUserData.put("locked", false);
        sampleUserData.put("profileImageUrl", "https://example.com/photo.jpg");
        sampleUserData.put("bio", "An investor");
        sampleUserData.put("createdAt", Instant.now());
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        @Test
        @DisplayName("should return paged users matching search criteria")
        void shouldReturnPagedUsersMatchingCriteria() {
            AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder()
                    .kycStatus("VERIFIED")
                    .userType("INVESTOR")
                    .active(true)
                    .search("john")
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            Page<Map<String, Object>> page = new PageImpl<>(
                    List.of(sampleUserData), pageable, 1);
            when(adminUserRepository.findUsers("VERIFIED", "INVESTOR", true, "john", pageable))
                    .thenReturn(page);

            PagedResponse<AdminUserResponse> result = adminUserUseCase.listUsers(criteria, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("john@example.com");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("should return empty page when no users match")
        void shouldReturnEmptyPageWhenNoMatch() {
            AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder().build();
            Pageable pageable = PageRequest.of(0, 20);
            Page<Map<String, Object>> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(adminUserRepository.findUsers(null, null, null, null, pageable))
                    .thenReturn(emptyPage);

            PagedResponse<AdminUserResponse> result = adminUserUseCase.listUsers(criteria, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should map all user fields correctly in response")
        void shouldMapAllFieldsCorrectly() {
            AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder().build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Map<String, Object>> page = new PageImpl<>(List.of(sampleUserData), pageable, 1);
            when(adminUserRepository.findUsers(any(), any(), any(), any(), any()))
                    .thenReturn(page);

            PagedResponse<AdminUserResponse> result = adminUserUseCase.listUsers(criteria, pageable);
            AdminUserResponse user = result.getContent().get(0);

            assertThat(user.getId()).isEqualTo(userId);
            assertThat(user.getEmail()).isEqualTo("john@example.com");
            assertThat(user.getPhone()).isEqualTo("+254712345678");
            assertThat(user.getFirstName()).isEqualTo("John");
            assertThat(user.getLastName()).isEqualTo("Doe");
            assertThat(user.getUserType()).isEqualTo("INVESTOR");
            assertThat(user.getKycStatus()).isEqualTo("VERIFIED");
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.isPhoneVerified()).isTrue();
            assertThat(user.isActive()).isTrue();
            assertThat(user.isLocked()).isFalse();
            assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/photo.jpg");
            assertThat(user.getBio()).isEqualTo("An investor");
            assertThat(user.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should handle pagination with multiple pages")
        void shouldHandlePaginationWithMultiplePages() {
            AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder().build();
            Pageable pageable = PageRequest.of(1, 10);
            Page<Map<String, Object>> page = new PageImpl<>(
                    List.of(sampleUserData), pageable, 25);
            when(adminUserRepository.findUsers(any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(page);

            PagedResponse<AdminUserResponse> result = adminUserUseCase.listUsers(criteria, pageable);

            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.isLast()).isFalse();
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("should return user with roles when user exists")
        void shouldReturnUserWithRoles() {
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            List<Map<String, Object>> roles = List.of(
                    Map.of("name", "INVESTOR"),
                    Map.of("name", "USER"));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(roles);

            AdminUserResponse result = adminUserUseCase.getUser(userId);

            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getRoles()).containsExactlyInAnyOrder("INVESTOR", "USER");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(adminUserRepository.findUserById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserUseCase.getUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("activateUser")
    class ActivateUser {

        @Test
        @DisplayName("should activate user and log audit event")
        void shouldActivateUserAndAudit() {
            when(adminUserRepository.updateUserActive(userId, true)).thenReturn(1);
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(List.of());

            AdminUserResponse result = adminUserUseCase.activateUser(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            verify(adminUserRepository).updateUserActive(userId, true);
            verify(auditLogger).log(eq("ACTIVATE_USER"), eq("User"), eq(userId.toString()), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFoundOnActivate() {
            UUID unknownId = UUID.randomUUID();
            when(adminUserRepository.updateUserActive(unknownId, true)).thenReturn(0);

            assertThatThrownBy(() -> adminUserUseCase.activateUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUser {

        @Test
        @DisplayName("should deactivate user and log audit event")
        void shouldDeactivateUserAndAudit() {
            when(adminUserRepository.updateUserActive(userId, false)).thenReturn(1);
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(List.of());

            AdminUserResponse result = adminUserUseCase.deactivateUser(userId);

            assertThat(result).isNotNull();
            verify(adminUserRepository).updateUserActive(userId, false);
            verify(auditLogger).log(eq("DEACTIVATE_USER"), eq("User"), eq(userId.toString()), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFoundOnDeactivate() {
            UUID unknownId = UUID.randomUUID();
            when(adminUserRepository.updateUserActive(unknownId, false)).thenReturn(0);

            assertThatThrownBy(() -> adminUserUseCase.deactivateUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("lockUser")
    class LockUser {

        @Test
        @DisplayName("should lock user and log audit event")
        void shouldLockUserAndAudit() {
            when(adminUserRepository.updateUserLocked(userId, true)).thenReturn(1);
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(List.of());

            AdminUserResponse result = adminUserUseCase.lockUser(userId);

            assertThat(result).isNotNull();
            verify(adminUserRepository).updateUserLocked(userId, true);
            verify(auditLogger).log(eq("LOCK_USER"), eq("User"), eq(userId.toString()), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFoundOnLock() {
            UUID unknownId = UUID.randomUUID();
            when(adminUserRepository.updateUserLocked(unknownId, true)).thenReturn(0);

            assertThatThrownBy(() -> adminUserUseCase.lockUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("unlockUser")
    class UnlockUser {

        @Test
        @DisplayName("should unlock user and log audit event")
        void shouldUnlockUserAndAudit() {
            when(adminUserRepository.updateUserLocked(userId, false)).thenReturn(1);
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(List.of());

            AdminUserResponse result = adminUserUseCase.unlockUser(userId);

            assertThat(result).isNotNull();
            verify(adminUserRepository).updateUserLocked(userId, false);
            verify(auditLogger).log(eq("UNLOCK_USER"), eq("User"), eq(userId.toString()), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFoundOnUnlock() {
            UUID unknownId = UUID.randomUUID();
            when(adminUserRepository.updateUserLocked(unknownId, false)).thenReturn(0);

            assertThatThrownBy(() -> adminUserUseCase.unlockUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("assignRole")
    class AssignRole {

        @Test
        @DisplayName("should assign role and log audit event")
        void shouldAssignRoleAndAudit() {
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(
                    List.of(Map.of("name", "ADMIN")));

            AdminUserResponse result = adminUserUseCase.assignRole(userId, "admin");

            assertThat(result.getRoles()).contains("ADMIN");
            verify(adminUserRepository).assignRole(userId, "ADMIN");
            verify(auditLogger).log(eq("ASSIGN_ROLE"), eq("User"), eq(userId.toString()),
                    isNull(), eq("admin"), anyString());
        }

        @Test
        @DisplayName("should convert role name to uppercase")
        void shouldConvertRoleNameToUpperCase() {
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(List.of());

            adminUserUseCase.assignRole(userId, "super_admin");

            verify(adminUserRepository).assignRole(userId, "SUPER_ADMIN");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(adminUserRepository.findUserById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserUseCase.assignRole(unknownId, "ADMIN"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(adminUserRepository, never()).assignRole(any(), any());
        }
    }

    @Nested
    @DisplayName("removeRole")
    class RemoveRole {

        @Test
        @DisplayName("should remove role and log audit event")
        void shouldRemoveRoleAndAudit() {
            when(adminUserRepository.findUserById(userId)).thenReturn(Optional.of(sampleUserData));
            when(adminUserRepository.findUserRoles(userId)).thenReturn(List.of());

            adminUserUseCase.removeRole(userId, "admin");

            verify(adminUserRepository).removeRole(userId, "ADMIN");
            verify(auditLogger).log(eq("REMOVE_ROLE"), eq("User"), eq(userId.toString()),
                    eq("admin"), isNull(), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(adminUserRepository.findUserById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserUseCase.removeRole(unknownId, "ADMIN"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(adminUserRepository, never()).removeRole(any(), any());
        }
    }
}
