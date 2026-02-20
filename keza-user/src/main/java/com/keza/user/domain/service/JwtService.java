package com.keza.user.domain.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public JwtService(
            @Value("${keza.jwt.secret}") String secret,
            @Value("${keza.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${keza.jwt.refresh-token-expiration}") long refreshTokenExpiration,
            RedisTemplate<String, Object> redisTemplate) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                Base64.getEncoder().encodeToString(secret.getBytes())));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(UUID userId, String email, Collection<String> roles) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(userId.toString())
                .id(tokenId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(signingKey)
                .compact();

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                tokenId,
                refreshTokenExpiration, TimeUnit.MILLISECONDS);

        return token;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            String jti = claims.getId();
            if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti))) {
                return false;
            }
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, UUID userId) {
        try {
            Claims claims = parseToken(token);
            String storedTokenId = (String) redisTemplate.opsForValue()
                    .get(REFRESH_TOKEN_PREFIX + userId);
            return claims.getId() != null && claims.getId().equals(storedTokenId);
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = parseToken(token);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                String key = claims.getId() != null ? claims.getId() : token;
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + key, "true", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.debug("Could not blacklist token: {}", e.getMessage());
        }
    }

    public void revokeRefreshToken(UUID userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
