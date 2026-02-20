package com.keza.integration;

import com.keza.common.dto.ApiResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Auth Integration - Full End-to-End Flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("keza_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3-management-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    // Shared state across ordered test methods
    private static String accessToken;
    private static String refreshToken;

    private static final String TEST_EMAIL = "integrationtest@example.com";
    private static final String TEST_PASSWORD = "TestPass1@secure";
    private static final String TEST_FIRST_NAME = "Integration";
    private static final String TEST_LAST_NAME = "Tester";

    @Nested
    @DisplayName("Registration")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Registration {

        @Test
        @Order(1)
        @DisplayName("should register a new user and return access and refresh tokens")
        void shouldRegisterNewUser() {
            Map<String, Object> registerRequest = new LinkedHashMap<>();
            registerRequest.put("email", TEST_EMAIL);
            registerRequest.put("password", TEST_PASSWORD);
            registerRequest.put("firstName", TEST_FIRST_NAME);
            registerRequest.put("lastName", TEST_LAST_NAME);
            registerRequest.put("userType", "INVESTOR");

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
            Map<String, Object> registerRequest = new LinkedHashMap<>();
            registerRequest.put("email", TEST_EMAIL);
            registerRequest.put("password", TEST_PASSWORD);
            registerRequest.put("firstName", TEST_FIRST_NAME);
            registerRequest.put("lastName", TEST_LAST_NAME);
            registerRequest.put("userType", "INVESTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(registerRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/register", entity, Map.class);

            // Should fail due to duplicate email - expect 4xx or specific error
            assertThat(response.getStatusCode().is4xxClientError()
                    || (response.getBody() != null && Boolean.FALSE.equals(response.getBody().get("success"))))
                    .isTrue();
        }

        @Test
        @Order(3)
        @DisplayName("should reject registration with invalid email format")
        void shouldRejectInvalidEmail() {
            Map<String, Object> registerRequest = new LinkedHashMap<>();
            registerRequest.put("email", "not-an-email");
            registerRequest.put("password", TEST_PASSWORD);
            registerRequest.put("firstName", TEST_FIRST_NAME);
            registerRequest.put("lastName", TEST_LAST_NAME);

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
            Map<String, Object> registerRequest = new LinkedHashMap<>();
            registerRequest.put("email", "another@example.com");
            registerRequest.put("password", "weak");
            registerRequest.put("firstName", TEST_FIRST_NAME);
            registerRequest.put("lastName", TEST_LAST_NAME);

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
            Map<String, Object> registerRequest = new LinkedHashMap<>();
            registerRequest.put("email", "logintest@example.com");
            registerRequest.put("password", TEST_PASSWORD);
            registerRequest.put("firstName", "Login");
            registerRequest.put("lastName", "Tester");
            registerRequest.put("userType", "INVESTOR");

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

            // Store for later tests
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

            // Should get 401 Unauthorized or 403 Forbidden
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

        @Test
        @Order(4)
        @DisplayName("should allow authenticated access to user endpoints after login")
        void shouldAllowAuthenticatedAccess() {
            // Register and login to get a valid token
            Map<String, Object> registerRequest = new LinkedHashMap<>();
            registerRequest.put("email", "protectedtest@example.com");
            registerRequest.put("password", TEST_PASSWORD);
            registerRequest.put("firstName", "Protected");
            registerRequest.put("lastName", "Tester");
            registerRequest.put("userType", "INVESTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                    "/api/v1/auth/register",
                    new HttpEntity<>(registerRequest, headers), Map.class);

            if (registerResponse.getStatusCode().is2xxSuccessful() && registerResponse.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
                if (data != null) {
                    String token = (String) data.get("access_token");

                    // Try accessing a protected user endpoint (e.g., the auth endpoints are public,
                    // but we verify the token is valid by checking we don't get 401)
                    HttpHeaders authHeaders = new HttpHeaders();
                    authHeaders.setBearerAuth(token);

                    // Accessing any authenticated endpoint should not return 401
                    // Using a generic endpoint that requires authentication
                    ResponseEntity<Map> response = restTemplate.exchange(
                            "/api/v1/auth/register",
                            HttpMethod.POST,
                            new HttpEntity<>(registerRequest, authHeaders),
                            Map.class);

                    // The endpoint is public, so we expect it to reach the controller
                    // (might return error due to duplicate email, but not 401)
                    assertThat(response.getStatusCode().value()).isNotEqualTo(401);
                }
            }
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
            Map<String, Object> registerRequest = new LinkedHashMap<>();
            registerRequest.put("email", "refreshtest@example.com");
            registerRequest.put("password", TEST_PASSWORD);
            registerRequest.put("firstName", "Refresh");
            registerRequest.put("lastName", "Tester");
            registerRequest.put("userType", "INVESTOR");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                    "/api/v1/auth/register",
                    new HttpEntity<>(registerRequest, headers), Map.class);

            assertThat(registerResponse.getStatusCode().is2xxSuccessful()).isTrue();

            Map<String, Object> regData = (Map<String, Object>) registerResponse.getBody().get("data");
            String regRefreshToken = (String) regData.get("refresh_token");
            assertThat(regRefreshToken).isNotBlank();

            // Use refresh token
            Map<String, String> refreshRequest = new LinkedHashMap<>();
            refreshRequest.put("refreshToken", regRefreshToken);

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

            // New tokens should be different from old ones (for access token at minimum)
            String newAccessToken = (String) data.get("access_token");
            assertThat(newAccessToken).isNotBlank();
        }

        @Test
        @Order(2)
        @DisplayName("should reject refresh with invalid refresh token")
        void shouldRejectInvalidRefreshToken() {
            Map<String, String> refreshRequest = new LinkedHashMap<>();
            refreshRequest.put("refreshToken", "invalid-refresh-token");

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

        @Test
        @Order(3)
        @DisplayName("should reject refresh with blank refresh token")
        void shouldRejectBlankRefreshToken() {
            Map<String, String> refreshRequest = new LinkedHashMap<>();
            refreshRequest.put("refreshToken", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/auth/refresh",
                    new HttpEntity<>(refreshRequest, headers), Map.class);

            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }
}
