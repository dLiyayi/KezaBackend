package com.keza.admin.adapter.in.web;

import com.keza.admin.application.dto.AdminUserResponse;
import com.keza.admin.application.dto.AdminUserSearchCriteria;
import com.keza.admin.application.usecase.AdminUserUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserUseCase adminUserUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) String kycStatus,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AdminUserSearchCriteria criteria = AdminUserSearchCriteria.builder()
                .kycStatus(kycStatus)
                .userType(userType)
                .active(active)
                .search(search)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<AdminUserResponse> response = adminUserUseCase.listUsers(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(@PathVariable UUID id) {
        AdminUserResponse response = adminUserUseCase.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> activateUser(@PathVariable UUID id) {
        AdminUserResponse response = adminUserUseCase.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User activated successfully"));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deactivateUser(@PathVariable UUID id) {
        AdminUserResponse response = adminUserUseCase.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User deactivated successfully"));
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<AdminUserResponse>> lockUser(@PathVariable UUID id) {
        AdminUserResponse response = adminUserUseCase.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User locked successfully"));
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unlockUser(@PathVariable UUID id) {
        AdminUserResponse response = adminUserUseCase.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User unlocked successfully"));
    }

    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> assignRole(
            @PathVariable UUID id,
            @PathVariable String roleName) {
        AdminUserResponse response = adminUserUseCase.assignRole(id, roleName);
        return ResponseEntity.ok(ApiResponse.success(response, "Role assigned successfully"));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> removeRole(
            @PathVariable UUID id,
            @PathVariable String roleName) {
        AdminUserResponse response = adminUserUseCase.removeRole(id, roleName);
        return ResponseEntity.ok(ApiResponse.success(response, "Role removed successfully"));
    }
}
