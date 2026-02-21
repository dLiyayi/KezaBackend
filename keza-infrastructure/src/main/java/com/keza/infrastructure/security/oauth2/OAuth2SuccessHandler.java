package com.keza.infrastructure.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handles successful OAuth2 authentication (e.g. KCB login).
 * <p>
 * On success this handler:
 * <ol>
 *   <li>Extracts email, firstName, lastName from the OAuth2User attributes</li>
 *   <li>Provisions or retrieves the user via {@link OAuth2UserProvisioningPort}</li>
 *   <li>Generates platform JWT tokens (access + refresh) via {@link OAuth2TokenGenerationPort}</li>
 *   <li>Redirects to the frontend callback URL with tokens as query parameters</li>
 * </ol>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "keza.oauth2.enabled", havingValue = "true")
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2UserProvisioningPort userProvisioningPort;
    private final OAuth2TokenGenerationPort tokenGenerationPort;
    private final String frontendCallbackUrl;

    public OAuth2SuccessHandler(
            OAuth2UserProvisioningPort userProvisioningPort,
            OAuth2TokenGenerationPort tokenGenerationPort,
            @Value("${keza.oauth2.frontend-callback-url}") String frontendCallbackUrl) {
        this.userProvisioningPort = userProvisioningPort;
        this.tokenGenerationPort = tokenGenerationPort;
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractAttribute(attributes, "email");
        String firstName = extractAttribute(attributes, "given_name", "first_name", "firstName");
        String lastName = extractAttribute(attributes, "family_name", "last_name", "lastName");

        if (email == null || email.isBlank()) {
            log.error("OAuth2 login failed: email attribute is missing from provider response");
            String errorUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                    .queryParam("error", URLEncoder.encode("Email not provided by OAuth2 provider", StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
            response.sendRedirect(errorUrl);
            return;
        }

        // Default to "User" if name attributes are missing
        if (firstName == null || firstName.isBlank()) {
            firstName = "User";
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = "";
        }

        try {
            OAuth2UserInfo userInfo = userProvisioningPort.provisionOrGetUser(email, firstName, lastName);

            String accessToken = tokenGenerationPort.generateAccessToken(
                    userInfo.userId(), userInfo.email(), userInfo.roles());
            String refreshToken = tokenGenerationPort.generateRefreshToken(userInfo.userId());
            long expiresIn = tokenGenerationPort.getAccessTokenExpiration() / 1000;

            String redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                    .queryParam("access_token", accessToken)
                    .queryParam("refresh_token", refreshToken)
                    .queryParam("token_type", "Bearer")
                    .queryParam("expires_in", expiresIn)
                    .build()
                    .toUriString();

            log.info("OAuth2 login successful for user: {} ({})", email, userInfo.userId());
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 login failed during user provisioning for email: {}", email, e);
            String errorUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                    .queryParam("error", URLEncoder.encode("Authentication failed. Please try again.", StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * Extracts the first non-null, non-blank attribute value from the given keys.
     */
    private String extractAttribute(Map<String, Object> attributes, String... keys) {
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return null;
    }
}
