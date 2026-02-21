package com.keza.user.application.usecase;

import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.DuplicateResourceException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.common.exception.UnauthorizedException;
import com.keza.user.application.dto.*;
import com.keza.user.domain.event.EmailVerificationRequestedEvent;
import com.keza.user.domain.event.PasswordResetRequestedEvent;
import com.keza.user.domain.event.UserRegisteredEvent;
import com.keza.user.domain.model.User;
import com.keza.user.domain.model.UserRole;
import com.keza.user.domain.port.out.RoleRepository;
import com.keza.user.domain.port.out.UserRepository;
import com.keza.user.domain.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PASSWORD_RESET_PREFIX = "password_reset:";
    private static final String EMAIL_VERIFICATION_PREFIX = "email_verification:";

    @Transactional
    public AuthResponse register(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        String roleName = request.getUserType() != null ? request.getUserType() : "INVESTOR";
        UserRole role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessRuleException("Role not found: " + roleName));

        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(role))
                .build();

        user = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getUserType().name()));

        log.info("User registered: {} ({})", user.getEmail(), user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new BusinessRuleException("ACCOUNT_LOCKED",
                    "Account is locked. Try again after " + user.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new UnauthorizedException("Invalid email or password");
        }

        user.resetFailedLoginAttempts();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        UUID userId = jwtService.getUserIdFromToken(token);

        if (!jwtService.isRefreshTokenValid(token, userId)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Rotate refresh token
        jwtService.blacklistToken(token);
        return buildAuthResponse(user);
    }

    public void logout(String accessToken, UUID userId) {
        jwtService.blacklistToken(accessToken);
        jwtService.revokeRefreshToken(userId);
        log.info("User logged out: {}", userId);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndDeletedFalse(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                    PASSWORD_RESET_PREFIX + token, user.getId().toString(),
                    1, TimeUnit.HOURS);
            eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                    user.getId(), user.getEmail(), user.getFirstName(), token));
            log.info("Password reset token generated for user: {}", user.getEmail());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String userId = (String) redisTemplate.opsForValue().get(PASSWORD_RESET_PREFIX + request.getToken());
        if (userId == null) {
            throw new BusinessRuleException("INVALID_TOKEN", "Invalid or expired password reset token");
        }

        User user = userRepository.findByIdAndDeletedFalse(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(PASSWORD_RESET_PREFIX + request.getToken());
        jwtService.revokeRefreshToken(user.getId());

        log.info("Password reset for user: {}", user.getEmail());
    }

    @Transactional
    public void sendEmailVerification(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.isEmailVerified()) {
            throw new BusinessRuleException("ALREADY_VERIFIED", "Email is already verified");
        }

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                EMAIL_VERIFICATION_PREFIX + token, userId.toString(),
                24, TimeUnit.HOURS);

        // Publish event for notification module to send the email
        eventPublisher.publishEvent(new EmailVerificationRequestedEvent(
                userId, user.getEmail(), user.getFirstName(), token));

        log.info("Email verification token generated for user: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        String userId = (String) redisTemplate.opsForValue().get(EMAIL_VERIFICATION_PREFIX + token);
        if (userId == null) {
            throw new BusinessRuleException("INVALID_TOKEN", "Invalid or expired verification token");
        }

        User user = userRepository.findByIdAndDeletedFalse(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEmailVerified(true);
        userRepository.save(user);
        redisTemplate.delete(EMAIL_VERIFICATION_PREFIX + token);

        log.info("Email verified for user: {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(mapToUserResponse(user, roleNames))
                .build();
    }

    private UserResponse mapToUserResponse(User user, Set<String> roles) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType().name())
                .kycStatus(user.getKycStatus().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
