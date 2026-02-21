package com.keza.user.adapter.out.oauth2;

import com.keza.infrastructure.security.oauth2.OAuth2TokenGenerationPort;
import com.keza.user.domain.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

/**
 * Implementation of {@link OAuth2TokenGenerationPort} that delegates to the
 * existing {@link JwtService} for generating platform JWT tokens during
 * OAuth2 login flows.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keza.oauth2.enabled", havingValue = "true")
public class OAuth2TokenGenerationAdapter implements OAuth2TokenGenerationPort {

    private final JwtService jwtService;

    @Override
    public String generateAccessToken(UUID userId, String email, Collection<String> roles) {
        return jwtService.generateAccessToken(userId, email, roles);
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        return jwtService.generateRefreshToken(userId);
    }

    @Override
    public long getAccessTokenExpiration() {
        return jwtService.getAccessTokenExpiration();
    }
}
