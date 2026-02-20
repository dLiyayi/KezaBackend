package com.keza.user.domain.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    private JwtService jwtService;

    private static final String SECRET = "my-super-secret-key-for-testing-jwt-tokens-minimum-256-bits";
    private static final long ACCESS_EXPIRATION = 3600000L;  // 1 hour
    private static final long REFRESH_EXPIRATION = 604800000L; // 7 days
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";
    private static final Set<String> ROLES = Set.of("INVESTOR");

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION, redisTemplate);
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("should generate a non-empty access token")
        void shouldGenerateToken() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("should embed userId as subject in the token")
        void shouldEmbedUserIdAsSubject() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

            Claims claims = jwtService.parseToken(token);
            assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        }

        @Test
        @DisplayName("should embed email and roles as claims")
        void shouldEmbedEmailAndRoles() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

            Claims claims = jwtService.parseToken(token);
            assertThat(claims.get("email", String.class)).isEqualTo(EMAIL);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            assertThat(roles).contains("INVESTOR");
        }

        @Test
        @DisplayName("should set expiration in the future")
        void shouldSetExpiration() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

            Claims claims = jwtService.parseToken(token);
            assertThat(claims.getExpiration()).isInTheFuture();
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("should generate a non-empty refresh token and store token ID in Redis")
        void shouldGenerateRefreshTokenAndStoreInRedis() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String token = jwtService.generateRefreshToken(USER_ID);

            assertThat(token).isNotBlank();
            verify(valueOperations).set(
                    eq("refresh_token:" + USER_ID),
                    anyString(),
                    eq(REFRESH_EXPIRATION),
                    eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("should include a JTI (token ID) in the refresh token")
        void shouldIncludeJti() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String token = jwtService.generateRefreshToken(USER_ID);

            Claims claims = jwtService.parseToken(token);
            assertThat(claims.getId()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("parseToken")
    class ParseToken {

        @Test
        @DisplayName("should parse a valid token and return claims")
        void shouldParseValidToken() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

            Claims claims = jwtService.parseToken(token);

            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        }

        @Test
        @DisplayName("should throw exception for an invalid token")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> jwtService.parseToken("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("should return true for a valid non-blacklisted token")
        void shouldReturnTrueForValidToken() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);
            // Access tokens have no JTI, so blacklist check is skipped

            boolean valid = jwtService.isTokenValid(token);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should return false for a blacklisted refresh token")
        void shouldReturnFalseForBlacklistedToken() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String refreshToken = jwtService.generateRefreshToken(USER_ID);
            // The refresh token has a JTI; simulate it being blacklisted
            Claims claims = jwtService.parseToken(refreshToken);
            when(redisTemplate.hasKey("blacklist:" + claims.getId())).thenReturn(true);

            boolean valid = jwtService.isTokenValid(refreshToken);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should return false for a tampered token")
        void shouldReturnFalseForTamperedToken() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            boolean valid = jwtService.isTokenValid(tampered);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should return false for a completely invalid token string")
        void shouldReturnFalseForGarbageToken() {
            boolean valid = jwtService.isTokenValid("not-a-jwt");

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("isRefreshTokenValid")
    class IsRefreshTokenValid {

        @Test
        @DisplayName("should return true when token JTI matches stored value")
        void shouldReturnTrueWhenJtiMatches() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String refreshToken = jwtService.generateRefreshToken(USER_ID);
            Claims claims = jwtService.parseToken(refreshToken);
            // Return the same JTI that was stored
            when(valueOperations.get("refresh_token:" + USER_ID)).thenReturn(claims.getId());

            boolean valid = jwtService.isRefreshTokenValid(refreshToken, USER_ID);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should return false when stored JTI does not match")
        void shouldReturnFalseWhenJtiMismatch() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String refreshToken = jwtService.generateRefreshToken(USER_ID);
            when(valueOperations.get("refresh_token:" + USER_ID)).thenReturn("different-jti");

            boolean valid = jwtService.isRefreshTokenValid(refreshToken, USER_ID);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should return false when no stored token exists in Redis")
        void shouldReturnFalseWhenNoStoredToken() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String refreshToken = jwtService.generateRefreshToken(USER_ID);
            when(valueOperations.get("refresh_token:" + USER_ID)).thenReturn(null);

            boolean valid = jwtService.isRefreshTokenValid(refreshToken, USER_ID);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken")
    class GetUserIdFromToken {

        @Test
        @DisplayName("should extract userId from a valid token")
        void shouldExtractUserId() {
            String token = jwtService.generateAccessToken(USER_ID, EMAIL, ROLES);

            UUID extractedId = jwtService.getUserIdFromToken(token);

            assertThat(extractedId).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("blacklistToken")
    class BlacklistToken {

        @Test
        @DisplayName("should store token in Redis blacklist with correct TTL")
        void shouldBlacklistToken() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String refreshToken = jwtService.generateRefreshToken(USER_ID);

            jwtService.blacklistToken(refreshToken);

            verify(valueOperations).set(
                    argThat(key -> key.startsWith("blacklist:")),
                    eq("true"),
                    longThat(ttl -> ttl > 0),
                    eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("should not throw for an invalid token")
        void shouldNotThrowForInvalidToken() {
            // blacklistToken catches all exceptions internally
            jwtService.blacklistToken("invalid.token");
            // no exception - passes
        }
    }

    @Nested
    @DisplayName("revokeRefreshToken")
    class RevokeRefreshToken {

        @Test
        @DisplayName("should delete the refresh token key from Redis")
        void shouldDeleteFromRedis() {
            jwtService.revokeRefreshToken(USER_ID);

            verify(redisTemplate).delete("refresh_token:" + USER_ID);
        }
    }

    @Nested
    @DisplayName("getAccessTokenExpiration")
    class GetAccessTokenExpiration {

        @Test
        @DisplayName("should return the configured expiration value")
        void shouldReturnExpiration() {
            assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(ACCESS_EXPIRATION);
        }
    }
}
