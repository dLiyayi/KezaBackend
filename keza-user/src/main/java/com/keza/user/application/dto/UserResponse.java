package com.keza.user.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String userType;
    private String kycStatus;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String profileImageUrl;
    private String bio;
    private String authProvider;
    private Set<String> roles;
    private Instant createdAt;
}
