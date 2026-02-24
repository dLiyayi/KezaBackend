package com.keza.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the auth flow.
 * Requires Docker Compose services running (PostgreSQL, Redis, RabbitMQ).
 * Start with: docker compose -f docker/docker-compose.yml up -d
 * Set KEZA_INTEGRATION_TESTS=true to enable these tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Auth Integration - Full End-to-End Flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "KEZA_INTEGRATION_TESTS", matches = "true")
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static String accessToken;
    private static String refreshToken;

    private static final String TEST_EMAIL = "integrationtest@example.com";
    private static final String TEST_PASSWORD = "TestPass1@secure";
    private static final String TEST_FIRST_NAME = "Integration";
    private static final String TEST_LAST_NAME = "Tester";

    private Map<String, Object> buildRegisterRequest(String email, String password,
                                                      String firstName, String lastName, String userType) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("email", email);
        request.put("password", password);
        request.put("first_name", firstName);
        request.put("last_name", lastName);
        if (userType != null) {
            request.put("user_type", userType);
        }
        return request;
    }

    @Nested
    @DisplayName("Registration")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Registration {

        @Test
        @Order(1)
        @DisplayName("should register a new user and return access and refresh tokens")
        void shouldRegisterNewUser() {
            Map<String, Object> registerRequest = buildRegisterRequest(
                    TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME, TEST_LAST_NAME, "INVESTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(registerRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", entity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();

            Map<String, Object> body = response.getBody();
            assertThat(body.get("success")).isEqualTo(true);
            assertThat(body.get("message")).isEqualTo("Registration successful");

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data).isNotNull();
            assertThat(data.get("access_token")).isNotNull();
            assertThat(data.get("refresh_token")).isNotNull();
            assertThat(data.get("token_type")).isEqualTo("Bearer");

            accessToken = (String) data.get("access_token");
            refreshToken = (String) data.get("refresh_token");
        }

        @Test
        @Order(2)
        @DisplayName("should reject duplicate registration with same email")
        void shouldRejectDuplicateRegistration() {
            Map<String, Object> registerRequest = buildRegisterRequest(
                    TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME, TEST_LAST_NAME, "INVESTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(registerRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", entity, Map.class);

            assertThat(response.getStatusCode().is4xxClientError()
                    || (response.getBody() != null && Boolean.FALSE.equals(response.getBody().get("success"))))
                    .isTrue();
        }

        @Test
        @Order(3)
        @DisplayName("should reject registration with invalid email format")
        void shouldRejectInvalidEmail() {
            Map<String, Object> registerRequest = buildRegisterRequest(
                    "not-an-email", TEST_PASSWORD, TEST_FIRST_NAME, TEST_LAST_NAME, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(registerRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", entity, Map.class);

            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        @Order(4)
        @DisplayName("should reject registration with weak password")
        void shouldRejectWeakPassword() {
            Map<String, Object> registerRequest = buildRegisterRequest(
                    "another@example.com", "weak", TEST_FIRST_NAME, TEST_LAST_NAME, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(registerRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", entity, Map.class);

            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    @DisplayName("Login")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Login {

        @Test
        @Order(1)
        @DisplayName("should login with valid credentials and return tokens")
        void shouldLoginWithValidCredentials() {
            // First, ensure user exists by registering
            Map<String, Object> registerRequest = buildRegisterRequest(
                    "logintest@example.com", TEST_PASSWORD, "Login", "Tester", "INVESTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity("/api/v1/auth/register",
                    new HttpEntity<>(registerRequest, headers), Map.class);

            // Now login
            Map<String, String> loginRequest = new LinkedHashMap<>();
            loginRequest.put("email", "logintest@example.com");
            loginRequest.put("password", TEST_PASSWORD);

            HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/login", loginEntity, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("success")).isEqualTo(true);

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data.get("access_token")).isNotNull();
            assertThat(data.get("refresh_token")).isNotNull();

            accessToken = (String) data.get("access_token");
            refreshToken = (String) data.get("refresh_token");
        }

        @Test
        @Order(2)
        @DisplayName("should reject login with invalid password")
        void shouldRejectInvalidPassword() {
            Map<String, String> loginRequest = new LinkedHashMap<>();
            loginRequest.put("email", "logintest@example.com");
            loginRequest.put("password", "WrongPassword1@");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/login", entity, Map.class);

            assertThat(response.getStatusCode().is4xxClientError()
                    || (response.getBody() != null && Boolean.FALSE.equals(response.getBody().get("success"))))
                    .isTrue();
        }

        @Test
        @Order(3)
        @DisplayName("should reject login with non-existent email")
        void shouldRejectNonExistentEmail() {
            Map<String, String> loginRequest = new LinkedHashMap<>();
            loginRequest.put("email", "nonexistent@example.com");
            loginRequest.put("password", TEST_PASSWORD);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/login", entity, Map.class);

            assertThat(response.getStatusCode().is4xxClientError()
                    || (response.getBody() != null && Boolean.FALSE.equals(response.getBody().get("success"))))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Protected Endpoint Access")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProtectedEndpoint {

        @Test
        @Order(1)
        @DisplayName("should deny access to protected endpoint without token")
        void shouldDenyAccessWithoutToken() {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/v1/admin/analytics/overview", Map.class);

            assertThat(response.getStatusCode().value()).isIn(401, 403);
        }

        @Test
        @Order(2)
        @DisplayName("should deny access to protected endpoint with invalid token")
        void shouldDenyAccessWithInvalidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalid-jwt-token-here");

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/admin/analytics/overview",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);

            assertThat(response.getStatusCode().value()).isIn(401, 403);
        }

        @Test
        @Order(3)
        @DisplayName("should allow access to public health endpoint without token")
        void shouldAllowAccessToHealthEndpoint() {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/actuator/health", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TokenRefresh {

        @Test
        @Order(1)
        @DisplayName("should refresh token with valid refresh token")
        void shouldRefreshTokenWithValidRefreshToken() {
            // Register to get tokens
            Map<String, Object> registerRequest = buildRegisterRequest(
                    "refreshtest@example.com", TEST_PASSWORD, "Refresh", "Tester", "INVESTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                    "/api/v1/auth/register",
                    new HttpEntity<>(registerRequest, headers), Map.class);

            assertThat(registerResponse.getStatusCode().is2xxSuccessful()).isTrue();

            Map<String, Object> regData = (Map<String, Object>) registerResponse.getBody().get("data");
            String regRefreshToken = (String) regData.get("refresh_token");
            assertThat(regRefreshToken).isNotBlank();

            // Use refresh token (snake_case for Jackson)
            Map<String, String> refreshRequest = new LinkedHashMap<>();
            refreshRequest.put("refresh_token", regRefreshToken);

            ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(
                    "/api/v1/auth/refresh",
                    new HttpEntity<>(refreshRequest, headers), Map.class);

            assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = refreshResponse.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("success")).isEqualTo(true);

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data.get("access_token")).isNotNull();
            assertThat(data.get("refresh_token")).isNotNull();
        }

        @Test
        @Order(2)
        @DisplayName("should reject refresh with invalid refresh token")
        void shouldRejectInvalidRefreshToken() {
            Map<String, String> refreshRequest = new LinkedHashMap<>();
            refreshRequest.put("refresh_token", "invalid-refresh-token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/refresh",
                    new HttpEntity<>(refreshRequest, headers), Map.class);

            assertThat(response.getStatusCode().is4xxClientError()
                    || response.getStatusCode().is5xxServerError()
                    || (response.getBody() != null && Boolean.FALSE.equals(response.getBody().get("success"))))
                    .isTrue();
        }
    }
}
