package com.keza.infrastructure.security.oauth2;

import java.util.Collection;
import java.util.UUID;

/**
 * Port interface for generating platform JWT tokens during OAuth2 login.
 * Defined in keza-infrastructure so that the OAuth2SuccessHandler can generate
 * tokens without depending on keza-user's JwtService directly.
 * The implementation lives in keza-user module.
 */
public interface OAuth2TokenGenerationPort {

    /**
     * Generates a platform access token for the given user.
     *
     * @param userId the user's UUID
     * @param email  the user's email
     * @param roles  the user's role names
     * @return the signed JWT access token
     */
    String generateAccessToken(UUID userId, String email, Collection<String> roles);

    /**
     * Generates a platform refresh token for the given user.
     *
     * @param userId the user's UUID
     * @return the signed JWT refresh token
     */
    String generateRefreshToken(UUID userId);

    /**
     * Returns the access token expiration in milliseconds.
     *
     * @return expiration in milliseconds
     */
    long getAccessTokenExpiration();
}
