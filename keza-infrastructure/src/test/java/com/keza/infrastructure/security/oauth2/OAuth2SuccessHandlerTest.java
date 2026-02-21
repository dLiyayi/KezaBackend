package com.keza.infrastructure.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OAuth2SuccessHandler")
@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    private static final String FRONTEND_CALLBACK_URL = "http://localhost:3000/auth/callback";

    @Mock
    private OAuth2UserProvisioningPort userProvisioningPort;

    @Mock
    private OAuth2TokenGenerationPort tokenGenerationPort;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(userProvisioningPort, tokenGenerationPort, FRONTEND_CALLBACK_URL);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
    }

    @Nested
    @DisplayName("Successful OAuth2 login")
    class SuccessfulLogin {

        @Test
        @DisplayName("should redirect with tokens when user exists")
        void shouldRedirectWithTokensForExistingUser() throws Exception {
            UUID userId = UUID.randomUUID();
            Map<String, Object> attributes = Map.of(
                    "email", "jane@keza.com",
                    "given_name", "Jane",
                    "family_name", "Doe"
            );
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            OAuth2UserInfo userInfo = new OAuth2UserInfo(userId, "jane@keza.com", Set.of("INVESTOR"));
            when(userProvisioningPort.provisionOrGetUser("jane@keza.com", "Jane", "Doe"))
                    .thenReturn(userInfo);
            when(tokenGenerationPort.generateAccessToken(userId, "jane@keza.com", Set.of("INVESTOR")))
                    .thenReturn("access-token-123");
            when(tokenGenerationPort.generateRefreshToken(userId))
                    .thenReturn("refresh-token-456");
            when(tokenGenerationPort.getAccessTokenExpiration())
                    .thenReturn(900000L);

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).sendRedirect(urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).startsWith(FRONTEND_CALLBACK_URL);
            assertThat(redirectUrl).contains("access_token=access-token-123");
            assertThat(redirectUrl).contains("refresh_token=refresh-token-456");
            assertThat(redirectUrl).contains("token_type=Bearer");
            assertThat(redirectUrl).contains("expires_in=900");
        }

        @Test
        @DisplayName("should auto-provision new user and redirect with tokens")
        void shouldProvisionNewUserAndRedirect() throws Exception {
            UUID userId = UUID.randomUUID();
            Map<String, Object> attributes = Map.of(
                    "email", "new@keza.com",
                    "given_name", "New",
                    "family_name", "User"
            );
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            OAuth2UserInfo userInfo = new OAuth2UserInfo(userId, "new@keza.com", Set.of("INVESTOR"));
            when(userProvisioningPort.provisionOrGetUser("new@keza.com", "New", "User"))
                    .thenReturn(userInfo);
            when(tokenGenerationPort.generateAccessToken(eq(userId), eq("new@keza.com"), anyCollection()))
                    .thenReturn("new-access-token");
            when(tokenGenerationPort.generateRefreshToken(userId))
                    .thenReturn("new-refresh-token");
            when(tokenGenerationPort.getAccessTokenExpiration())
                    .thenReturn(900000L);

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(userProvisioningPort).provisionOrGetUser("new@keza.com", "New", "User");
            verify(response).sendRedirect(contains("access_token=new-access-token"));
        }

        @Test
        @DisplayName("should handle alternative attribute names (first_name, last_name)")
        void shouldHandleAlternativeAttributeNames() throws Exception {
            UUID userId = UUID.randomUUID();
            Map<String, Object> attributes = Map.of(
                    "email", "alt@keza.com",
                    "first_name", "Alt",
                    "last_name", "User"
            );
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            OAuth2UserInfo userInfo = new OAuth2UserInfo(userId, "alt@keza.com", Set.of("INVESTOR"));
            when(userProvisioningPort.provisionOrGetUser("alt@keza.com", "Alt", "User"))
                    .thenReturn(userInfo);
            when(tokenGenerationPort.generateAccessToken(eq(userId), eq("alt@keza.com"), anyCollection()))
                    .thenReturn("token");
            when(tokenGenerationPort.generateRefreshToken(userId)).thenReturn("refresh");
            when(tokenGenerationPort.getAccessTokenExpiration()).thenReturn(900000L);

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(userProvisioningPort).provisionOrGetUser("alt@keza.com", "Alt", "User");
        }
    }

    @Nested
    @DisplayName("Missing attributes")
    class MissingAttributes {

        @Test
        @DisplayName("should redirect with error when email is missing")
        void shouldRedirectWithErrorWhenEmailMissing() throws Exception {
            Map<String, Object> attributes = Map.of(
                    "given_name", "Jane",
                    "family_name", "Doe"
            );
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).sendRedirect(urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).startsWith(FRONTEND_CALLBACK_URL);
            assertThat(redirectUrl).contains("error=");
            assertThat(redirectUrl).doesNotContain("access_token");
            verifyNoInteractions(userProvisioningPort);
        }

        @Test
        @DisplayName("should use default values when name attributes are missing")
        void shouldUseDefaultsWhenNameMissing() throws Exception {
            UUID userId = UUID.randomUUID();
            Map<String, Object> attributes = Map.of("email", "noname@keza.com");
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            OAuth2UserInfo userInfo = new OAuth2UserInfo(userId, "noname@keza.com", Set.of("INVESTOR"));
            when(userProvisioningPort.provisionOrGetUser("noname@keza.com", "User", ""))
                    .thenReturn(userInfo);
            when(tokenGenerationPort.generateAccessToken(eq(userId), eq("noname@keza.com"), anyCollection()))
                    .thenReturn("token");
            when(tokenGenerationPort.generateRefreshToken(userId)).thenReturn("refresh");
            when(tokenGenerationPort.getAccessTokenExpiration()).thenReturn(900000L);

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(userProvisioningPort).provisionOrGetUser("noname@keza.com", "User", "");
        }

        @Test
        @DisplayName("should redirect with error when email is blank")
        void shouldRedirectWithErrorWhenEmailBlank() throws Exception {
            Map<String, Object> attributes = Map.of("email", "  ");
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("error=");
            verifyNoInteractions(userProvisioningPort);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should redirect with error when provisioning fails")
        void shouldRedirectWithErrorWhenProvisioningFails() throws Exception {
            Map<String, Object> attributes = Map.of(
                    "email", "fail@keza.com",
                    "given_name", "Fail",
                    "family_name", "User"
            );
            when(oAuth2User.getAttributes()).thenReturn(attributes);
            when(userProvisioningPort.provisionOrGetUser(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).sendRedirect(urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).startsWith(FRONTEND_CALLBACK_URL);
            assertThat(redirectUrl).contains("error=");
            assertThat(redirectUrl).doesNotContain("access_token");
        }

        @Test
        @DisplayName("should redirect with error when token generation fails")
        void shouldRedirectWithErrorWhenTokenGenerationFails() throws Exception {
            UUID userId = UUID.randomUUID();
            Map<String, Object> attributes = Map.of(
                    "email", "tokenfail@keza.com",
                    "given_name", "Token",
                    "family_name", "Fail"
            );
            when(oAuth2User.getAttributes()).thenReturn(attributes);

            OAuth2UserInfo userInfo = new OAuth2UserInfo(userId, "tokenfail@keza.com", Set.of("INVESTOR"));
            when(userProvisioningPort.provisionOrGetUser(anyString(), anyString(), anyString()))
                    .thenReturn(userInfo);
            when(tokenGenerationPort.generateAccessToken(any(), anyString(), anyCollection()))
                    .thenThrow(new RuntimeException("Key error"));

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("error=");
        }
    }
}
