package com.keza.infrastructure.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-provider OAuth2 client configuration.
 * Registers all available providers (KCB, Google, Facebook, Apple) based on which
 * ones have credentials configured via environment variables.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "keza.oauth2.enabled", havingValue = "true")
public class SocialOAuth2Config {

    // KCB properties
    @Value("${keza.oauth2.kcb.client-id:}")
    private String kcbClientId;
    @Value("${keza.oauth2.kcb.client-secret:}")
    private String kcbClientSecret;
    @Value("${keza.oauth2.kcb.authorization-uri:}")
    private String kcbAuthorizationUri;
    @Value("${keza.oauth2.kcb.token-uri:}")
    private String kcbTokenUri;
    @Value("${keza.oauth2.kcb.user-info-uri:}")
    private String kcbUserInfoUri;
    @Value("${keza.oauth2.kcb.redirect-uri:}")
    private String kcbRedirectUri;

    // Google properties
    @Value("${keza.oauth2.google.client-id:}")
    private String googleClientId;
    @Value("${keza.oauth2.google.client-secret:}")
    private String googleClientSecret;

    // Facebook properties
    @Value("${keza.oauth2.facebook.client-id:}")
    private String facebookClientId;
    @Value("${keza.oauth2.facebook.client-secret:}")
    private String facebookClientSecret;

    // Apple properties
    @Value("${keza.oauth2.apple.client-id:}")
    private String appleClientId;
    @Value("${keza.oauth2.apple.team-id:}")
    private String appleTeamId;
    @Value("${keza.oauth2.apple.key-id:}")
    private String appleKeyId;
    @Value("${keza.oauth2.apple.private-key-file:}")
    private String applePrivateKeyFile;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (!kcbClientId.isBlank()) {
            registrations.add(kcbRegistration());
            log.info("OAuth2 provider registered: KCB");
        }
        if (!googleClientId.isBlank()) {
            registrations.add(googleRegistration());
            log.info("OAuth2 provider registered: Google");
        }
        if (!facebookClientId.isBlank()) {
            registrations.add(facebookRegistration());
            log.info("OAuth2 provider registered: Facebook");
        }
        if (!appleClientId.isBlank()) {
            registrations.add(appleRegistration());
            log.info("OAuth2 provider registered: Apple");
        }

        if (registrations.isEmpty()) {
            log.warn("OAuth2 is enabled but no providers have credentials configured");
            // Add a placeholder to avoid empty repository error; it will never match
            registrations.add(placeholderRegistration());
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration kcbRegistration() {
        return ClientRegistration.withRegistrationId("kcb")
                .clientId(kcbClientId)
                .clientSecret(kcbClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(kcbRedirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri(kcbAuthorizationUri)
                .tokenUri(kcbTokenUri)
                .userInfoUri(kcbUserInfoUri)
                .userNameAttributeName("email")
                .clientName("Kenya Commercial Bank")
                .build();
    }

    private ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();
    }

    private ClientRegistration facebookRegistration() {
        return ClientRegistration.withRegistrationId("facebook")
                .clientId(facebookClientId)
                .clientSecret(facebookClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v19.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v19.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email,first_name,last_name")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }

    private ClientRegistration appleRegistration() {
        AppleClientSecretGenerator secretGenerator =
                new AppleClientSecretGenerator(appleTeamId, appleClientId, appleKeyId, applePrivateKeyFile);

        return ClientRegistration.withRegistrationId("apple")
                .clientId(appleClientId)
                .clientSecret(secretGenerator.generateSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "name")
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Apple")
                .build();
    }

    private ClientRegistration placeholderRegistration() {
        return ClientRegistration.withRegistrationId("none")
                .clientId("placeholder")
                .clientSecret("placeholder")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://localhost/placeholder/authorize")
                .tokenUri("https://localhost/placeholder/token")
                .clientName("Placeholder")
                .build();
    }
}
