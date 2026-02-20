package com.keza.user.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.keza.common.enums.KycStatus;
import com.keza.common.enums.UserType;
import com.keza.infrastructure.handler.GlobalExceptionHandler;
import com.keza.user.application.dto.*;
import com.keza.user.application.usecase.AuthUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private AuthUseCase authUseCase;

    @InjectMocks
    private AuthController authController;

    private static final UUID USER_ID = UUID.randomUUID();

    private AuthResponse sampleAuthResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(USER_ID)
                .email("jane@example.com")
                .phone("+254712345678")
                .firstName("Jane")
                .lastName("Doe")
                .userType(UserType.INVESTOR.name())
                .kycStatus(KycStatus.PENDING.name())
                .emailVerified(false)
                .phoneVerified(false)
                .roles(Set.of("INVESTOR"))
                .createdAt(Instant.now())
                .build();

        sampleAuthResponse = AuthResponse.builder()
                .accessToken("access.jwt.token")
                .refreshToken("refresh.jwt.token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userResponse)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("should return 201 CREATED with auth response on successful registration")
        void shouldReturn201OnSuccess() throws Exception {
            RegisterUserRequest request = RegisterUserRequest.builder()
                    .email("jane@example.com")
                    .phone("+254712345678")
                    .password("Str0ng!Pass")
                    .firstName("Jane")
                    .lastName("Doe")
                    .userType("INVESTOR")
                    .build();

            when(authUseCase.register(any(RegisterUserRequest.class))).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access.jwt.token"))
                    .andExpect(jsonPath("$.data.user.email").value("jane@example.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("should return 200 OK with auth response on successful login")
        void shouldReturn200OnSuccess() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("jane@example.com")
                    .password("Str0ng!Pass")
                    .build();

            when(authUseCase.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access.jwt.token"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("should return 200 OK with new tokens on successful refresh")
        void shouldReturn200OnSuccess() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("old.refresh.token");
            when(authUseCase.refreshToken(any(RefreshTokenRequest.class))).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access.jwt.token"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordEndpoint {

        @Test
        @DisplayName("should return 200 OK regardless of whether email exists")
        void shouldReturn200Always() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest("jane@example.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authUseCase).forgotPassword(any(ForgotPasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordEndpoint {

        @Test
        @DisplayName("should return 200 OK on successful password reset")
        void shouldReturn200OnSuccess() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "NewStr0ng!Pass");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authUseCase).resetPassword(any(ResetPasswordRequest.class));
        }
    }
}
