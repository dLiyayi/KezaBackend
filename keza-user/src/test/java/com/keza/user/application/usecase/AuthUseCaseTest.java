package com.keza.user.application.usecase;

import com.keza.common.enums.KycStatus;
import com.keza.common.enums.UserType;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.DuplicateResourceException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.common.exception.UnauthorizedException;
import com.keza.user.application.dto.*;
import com.keza.user.domain.event.UserRegisteredEvent;
import com.keza.user.domain.model.User;
import com.keza.user.domain.model.UserRole;
import com.keza.user.domain.port.out.RoleRepository;
import com.keza.user.domain.port.out.UserRepository;
import com.keza.user.domain.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUseCase")
class AuthUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthUseCase authUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";
    private static final String PHONE = "+254712345678";
    private static final String PASSWORD = "Str0ng!Pass";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHash";
    private static final String ACCESS_TOKEN = "access.jwt.token";
    private static final String REFRESH_TOKEN = "refresh.jwt.token";

    private UserRole investorRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        investorRole = UserRole.builder().name("INVESTOR").description("Investor role").build();
        investorRole.setId(UUID.randomUUID());

        testUser = User.builder()
                .email(EMAIL)
                .phone(PHONE)
                .passwordHash(ENCODED_PASSWORD)
                .firstName("Jane")
                .lastName("Doe")
                .roles(Set.of(investorRole))
                .build();
        testUser.setId(USER_ID);
        testUser.setCreatedAt(Instant.now());
    }

    private void stubTokenGeneration() {
        when(jwtService.generateAccessToken(any(UUID.class), anyString(), anySet())).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(any(UUID.class))).thenReturn(REFRESH_TOKEN);
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);
    }

    @Nested
    @DisplayName("register")
    class Register {

        private RegisterUserRequest validRequest() {
            return RegisterUserRequest.builder()
                    .email(EMAIL)
                    .phone(PHONE)
                    .password(PASSWORD)
                    .firstName("Jane")
                    .lastName("Doe")
                    .userType("INVESTOR")
                    .build();
        }

        @Test
        @DisplayName("should register a new user successfully")
        void shouldRegisterNewUser() {
            RegisterUserRequest request = validRequest();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByPhone(PHONE)).thenReturn(false);
            when(roleRepository.findByName("INVESTOR")).thenReturn(Optional.of(investorRole));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            stubTokenGeneration();

            AuthResponse response = authUseCase.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
            assertThat(response.getUser().getRoles()).contains("INVESTOR");

            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
        }

        @Test
        @DisplayName("should publish UserRegisteredEvent with correct data")
        void shouldPublishUserRegisteredEvent() {
            RegisterUserRequest request = validRequest();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByPhone(PHONE)).thenReturn(false);
            when(roleRepository.findByName("INVESTOR")).thenReturn(Optional.of(investorRole));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            stubTokenGeneration();

            authUseCase.register(request);

            ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            UserRegisteredEvent event = captor.getValue();
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.email()).isEqualTo(EMAIL);
            assertThat(event.firstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("should default to INVESTOR role when userType is null")
        void shouldDefaultToInvestorRole() {
            RegisterUserRequest request = validRequest();
            request.setUserType(null);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName("INVESTOR")).thenReturn(Optional.of(investorRole));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            stubTokenGeneration();

            authUseCase.register(request);

            verify(roleRepository).findByName("INVESTOR");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void shouldThrowOnDuplicateEmail() {
            RegisterUserRequest request = validRequest();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authUseCase.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when phone already exists")
        void shouldThrowOnDuplicatePhone() {
            RegisterUserRequest request = validRequest();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByPhone(PHONE)).thenReturn(true);

            assertThatThrownBy(() -> authUseCase.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("phone");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip phone uniqueness check when phone is null")
        void shouldSkipPhoneCheckWhenNull() {
            RegisterUserRequest request = validRequest();
            request.setPhone(null);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName("INVESTOR")).thenReturn(Optional.of(investorRole));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            stubTokenGeneration();

            authUseCase.register(request);

            verify(userRepository, never()).existsByPhone(anyString());
        }

        @Test
        @DisplayName("should throw BusinessRuleException when role is not found")
        void shouldThrowWhenRoleNotFound() {
            RegisterUserRequest request = validRequest();
            request.setUserType("UNKNOWN_ROLE");
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByPhone(PHONE)).thenReturn(false);
            when(roleRepository.findByName("UNKNOWN_ROLE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUseCase.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private LoginRequest loginRequest() {
            return LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
        }

        @Test
        @DisplayName("should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            stubTokenGeneration();

            AuthResponse response = authUseCase.login(loginRequest());

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("should reset failed login attempts on successful login")
        void shouldResetFailedAttemptsOnSuccess() {
            testUser.setFailedLoginAttempts(3);
            when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            stubTokenGeneration();

            authUseCase.login(loginRequest());

            assertThat(testUser.getFailedLoginAttempts()).isZero();
            assertThat(testUser.isLocked()).isFalse();
            assertThat(testUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw UnauthorizedException when email not found")
        void shouldThrowWhenEmailNotFound() {
            when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUseCase.login(loginRequest()))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when password is wrong")
        void shouldThrowWhenPasswordIsWrong() {
            when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authUseCase.login(loginRequest()))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");

            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should increment failed login attempts on wrong password")
        void shouldIncrementFailedAttemptsOnWrongPassword() {
            int beforeAttempts = testUser.getFailedLoginAttempts();
            when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authUseCase.login(loginRequest()))
                    .isInstanceOf(UnauthorizedException.class);

            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(beforeAttempts + 1);
        }

        @Test
        @DisplayName("should throw BusinessRuleException when account is locked")
        void shouldThrowWhenAccountIsLocked() {
            testUser.setLocked(true);
            testUser.setLockedUntil(Instant.now().plusSeconds(1800));
            when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authUseCase.login(loginRequest()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Account is locked");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
            when(jwtService.getUserIdFromToken(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(jwtService.isRefreshTokenValid(REFRESH_TOKEN, USER_ID)).thenReturn(true);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            stubTokenGeneration();

            AuthResponse response = authUseCase.refreshToken(request);

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
            verify(jwtService).blacklistToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("should throw UnauthorizedException when refresh token is invalid")
        void shouldThrowWhenRefreshTokenInvalid() {
            RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
            when(jwtService.getUserIdFromToken(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(jwtService.isRefreshTokenValid(REFRESH_TOKEN, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> authUseCase.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found during refresh")
        void shouldThrowWhenUserNotFoundDuringRefresh() {
            RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
            when(jwtService.getUserIdFromToken(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(jwtService.isRefreshTokenValid(REFRESH_TOKEN, USER_ID)).thenReturn(true);
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUseCase.refreshToken(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("should blacklist access token and revoke refresh token")
        void shouldLogoutSuccessfully() {
            authUseCase.logout(ACCESS_TOKEN, USER_ID);

            verify(jwtService).blacklistToken(ACCESS_TOKEN);
            verify(jwtService).revokeRefreshToken(USER_ID);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("should generate reset token and store in redis when user exists")
        void shouldGenerateResetTokenWhenUserExists() {
            ForgotPasswordRequest request = new ForgotPasswordRequest(EMAIL);
            when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(testUser));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            authUseCase.forgotPassword(request);

            verify(valueOperations).set(
                    argThat(key -> key.startsWith("password_reset:")),
                    eq(USER_ID.toString()),
                    eq(1L),
                    eq(TimeUnit.HOURS)
            );
        }

        @Test
        @DisplayName("should do nothing when user email does not exist")
        void shouldDoNothingWhenEmailNotFound() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");
            when(userRepository.findByEmailAndDeletedFalse("unknown@example.com")).thenReturn(Optional.empty());

            authUseCase.forgotPassword(request);

            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private static final String RESET_TOKEN = "reset-token-uuid";

        @Test
        @DisplayName("should reset password successfully with valid token")
        void shouldResetPasswordSuccessfully() {
            ResetPasswordRequest request = new ResetPasswordRequest(RESET_TOKEN, "NewStr0ng!Pass");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password_reset:" + RESET_TOKEN)).thenReturn(USER_ID.toString());
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("NewStr0ng!Pass")).thenReturn("$2a$10$newHash");

            authUseCase.resetPassword(request);

            assertThat(testUser.getPasswordHash()).isEqualTo("$2a$10$newHash");
            verify(userRepository).save(testUser);
            verify(redisTemplate).delete("password_reset:" + RESET_TOKEN);
            verify(jwtService).revokeRefreshToken(USER_ID);
        }

        @Test
        @DisplayName("should throw BusinessRuleException when reset token is invalid or expired")
        void shouldThrowWhenResetTokenInvalid() {
            ResetPasswordRequest request = new ResetPasswordRequest(RESET_TOKEN, "NewStr0ng!Pass");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password_reset:" + RESET_TOKEN)).thenReturn(null);

            assertThatThrownBy(() -> authUseCase.resetPassword(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid or expired password reset token");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found during password reset")
        void shouldThrowWhenUserNotFoundDuringReset() {
            ResetPasswordRequest request = new ResetPasswordRequest(RESET_TOKEN, "NewStr0ng!Pass");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password_reset:" + RESET_TOKEN)).thenReturn(USER_ID.toString());
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUseCase.resetPassword(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
