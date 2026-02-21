package com.keza.infrastructure.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * OAuth2 client configuration for Kenya Commercial Bank (KCB).
 * <p>
 * Only activated when {@code keza.oauth2.enabled=true}. All provider-specific
 * URIs are injected from configuration properties so they can be switched
 * between sandbox and production environments via environment variables.
 */
@Configuration
@ConditionalOnProperty(name = "keza.oauth2.enabled", havingValue = "true")
public class KcbOAuth2Config {

    @Value("${keza.oauth2.kcb.client-id}")
    private String clientId;

    @Value("${keza.oauth2.kcb.client-secret}")
    private String clientSecret;

    @Value("${keza.oauth2.kcb.authorization-uri}")
    private String authorizationUri;

    @Value("${keza.oauth2.kcb.token-uri}")
    private String tokenUri;

    @Value("${keza.oauth2.kcb.user-info-uri}")
    private String userInfoUri;

    @Value("${keza.oauth2.kcb.redirect-uri}")
    private String redirectUri;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(kcbClientRegistration());
    }

    private ClientRegistration kcbClientRegistration() {
        return ClientRegistration.withRegistrationId("kcb")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .userInfoUri(userInfoUri)
                .userNameAttributeName("email")
                .clientName("Kenya Commercial Bank")
                .build();
    }
}
