package com.keza.infrastructure.security.oauth2;

import java.util.Set;
import java.util.UUID;

/**
 * DTO representing user information returned after OAuth2 provisioning.
 * Used as the return type for {@link OAuth2UserProvisioningPort} to decouple
 * keza-infrastructure from keza-user domain entities.
 */
public record OAuth2UserInfo(
        UUID userId,
        String email,
        Set<String> roles
) {
}
