package com.keza.infrastructure.security.oauth2;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Generates JWT client secrets for Apple Sign-In.
 * <p>
 * Apple requires a short-lived JWT (signed with ES256 using the team's .p8 private key)
 * instead of a static client secret.
 */
@Slf4j
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    private final String teamId;
    private final String clientId;
    private final String keyId;
    private final PrivateKey privateKey;

    public AppleClientSecretGenerator(String teamId, String clientId, String keyId, String privateKeyFile) {
        this.teamId = teamId;
        this.clientId = clientId;
        this.keyId = keyId;
        this.privateKey = loadPrivateKey(privateKeyFile);
    }

    /**
     * Generates a JWT client secret valid for 5 minutes.
     */
    public String generateSecret() {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .audience().add(APPLE_AUDIENCE).and()
                .subject(clientId)
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }

    private static PrivateKey loadPrivateKey(String filePath) {
        try {
            String keyContent = Files.readString(Path.of(filePath));
            String pem = keyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(keySpec);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Apple private key file: " + filePath, e);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid Apple private key", e);
        }
    }
}
