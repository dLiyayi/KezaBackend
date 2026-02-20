package com.keza.admin.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.keza.admin.application.dto.AdminUserResponse;
import com.keza.admin.application.dto.AdminUserSearchCriteria;
import com.keza.admin.application.usecase.AdminUserUseCase;
import com.keza.common.dto.PagedResponse;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.infrastructure.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserController")
class AdminUserControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AdminUserUseCase adminUserUseCase;

    @InjectMocks
    private AdminUserController adminUserController;

    private UUID userId;
    private AdminUserResponse sampleUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        userId = UUID.randomUUID();
        sampleUser = AdminUserResponse.builder()
                .id(userId)
                .email("john@example.com")
                .phone("+254712345678")
                .firstName("John")
                .lastName("Doe")
                .userType("INVESTOR")
                .kycStatus("VERIFIED")
                .emailVerified(true)
                .phoneVerified(true)
                .active(true)
                .locked(false)
                .roles(Set.of("INVESTOR", "USER"))
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class ListUsers {

        @Test
        @DisplayName("should return 200 with paged users")
        void shouldReturnPagedUsers() throws Exception {
            PagedResponse<AdminUserResponse> pagedResponse = PagedResponse.<AdminUserResponse>builder()
                    .content(List.of(sampleUser))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(adminUserUseCase.listUsers(any(AdminUserSearchCriteria.class), any()))
                    .thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/admin/users")
                            .param("kycStatus", "VERIFIED")
                            .param("userType", "INVESTOR")
                            .param("active", "true")
                            .param("search", "john")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].email").value("john@example.com"))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1));
        }

        @Test
        @DisplayName("should return 200 with empty page when no users match")
        void shouldReturnEmptyPage() throws Exception {
            PagedResponse<AdminUserResponse> emptyResponse = PagedResponse.<AdminUserResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(20)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();

            when(adminUserUseCase.listUsers(any(), any())).thenReturn(emptyResponse);

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/{id}")
    class GetUser {

        @Test
        @DisplayName("should return 200 with user details")
        void shouldReturnUserDetails() throws Exception {
            when(adminUserUseCase.getUser(userId)).thenReturn(sampleUser);

            mockMvc.perform(get("/api/v1/admin/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(userId.toString()))
                    .andExpect(jsonPath("$.data.email").value("john@example.com"))
                    .andExpect(jsonPath("$.data.firstName").value("John"))
                    .andExpect(jsonPath("$.data.lastName").value("Doe"));
        }

        @Test
        @DisplayName("should return error when user not found")
        void shouldReturnErrorWhenNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(adminUserUseCase.getUser(unknownId))
                    .thenThrow(new ResourceNotFoundException("User", unknownId));

            mockMvc.perform(get("/api/v1/admin/users/{id}", unknownId))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{id}/activate")
    class ActivateUser {

        @Test
        @DisplayName("should return 200 with activated user")
        void shouldActivateUser() throws Exception {
            sampleUser.setActive(true);
            when(adminUserUseCase.activateUser(userId)).thenReturn(sampleUser);

            mockMvc.perform(put("/api/v1/admin/users/{id}/activate", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User activated successfully"))
                    .andExpect(jsonPath("$.data.active").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{id}/deactivate")
    class DeactivateUser {

        @Test
        @DisplayName("should return 200 with deactivated user")
        void shouldDeactivateUser() throws Exception {
            sampleUser.setActive(false);
            when(adminUserUseCase.deactivateUser(userId)).thenReturn(sampleUser);

            mockMvc.perform(put("/api/v1/admin/users/{id}/deactivate", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User deactivated successfully"))
                    .andExpect(jsonPath("$.data.active").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{id}/lock")
    class LockUser {

        @Test
        @DisplayName("should return 200 with locked user")
        void shouldLockUser() throws Exception {
            sampleUser.setLocked(true);
            when(adminUserUseCase.lockUser(userId)).thenReturn(sampleUser);

            mockMvc.perform(put("/api/v1/admin/users/{id}/lock", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User locked successfully"))
                    .andExpect(jsonPath("$.data.locked").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{id}/unlock")
    class UnlockUser {

        @Test
        @DisplayName("should return 200 with unlocked user")
        void shouldUnlockUser() throws Exception {
            sampleUser.setLocked(false);
            when(adminUserUseCase.unlockUser(userId)).thenReturn(sampleUser);

            mockMvc.perform(put("/api/v1/admin/users/{id}/unlock", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unlocked successfully"))
                    .andExpect(jsonPath("$.data.locked").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{id}/roles/{roleName}")
    class AssignRole {

        @Test
        @DisplayName("should return 200 with updated user after role assignment")
        void shouldAssignRole() throws Exception {
            sampleUser.setRoles(Set.of("INVESTOR", "USER", "ADMIN"));
            when(adminUserUseCase.assignRole(userId, "ADMIN")).thenReturn(sampleUser);

            mockMvc.perform(post("/api/v1/admin/users/{id}/roles/{roleName}", userId, "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Role assigned successfully"))
                    .andExpect(jsonPath("$.data.roles", hasItem("ADMIN")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/users/{id}/roles/{roleName}")
    class RemoveRole {

        @Test
        @DisplayName("should return 200 after role removal")
        void shouldRemoveRole() throws Exception {
            sampleUser.setRoles(Set.of("USER"));
            when(adminUserUseCase.removeRole(userId, "INVESTOR")).thenReturn(sampleUser);

            mockMvc.perform(delete("/api/v1/admin/users/{id}/roles/{roleName}", userId, "INVESTOR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Role removed successfully"))
                    .andExpect(jsonPath("$.data.roles", not(hasItem("INVESTOR"))));
        }
    }
}
