package com.keza.user.application.usecase;

import com.keza.common.enums.KycStatus;
import com.keza.common.enums.UserType;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.user.application.dto.ChangePasswordRequest;
import com.keza.user.application.dto.UpdateProfileRequest;
import com.keza.user.application.dto.UserResponse;
import com.keza.user.domain.model.User;
import com.keza.user.domain.model.UserRole;
import com.keza.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileUseCase")
class UserProfileUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileUseCase userProfileUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHash";

    private User testUser;
    private UserRole investorRole;

    @BeforeEach
    void setUp() {
        investorRole = UserRole.builder().name("INVESTOR").description("Investor role").build();
        investorRole.setId(UUID.randomUUID());

        testUser = User.builder()
                .email("jane@example.com")
                .phone("+254712345678")
                .passwordHash(ENCODED_PASSWORD)
                .firstName("Jane")
                .lastName("Doe")
                .roles(Set.of(investorRole))
                .build();
        testUser.setId(USER_ID);
        testUser.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("should return user profile when user exists")
        void shouldReturnProfile() {
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));

            UserResponse response = userProfileUseCase.getProfile(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(USER_ID);
            assertThat(response.getEmail()).isEqualTo("jane@example.com");
            assertThat(response.getFirstName()).isEqualTo("Jane");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getUserType()).isEqualTo(UserType.INVESTOR.name());
            assertThat(response.getKycStatus()).isEqualTo(KycStatus.PENDING.name());
            assertThat(response.getRoles()).contains("INVESTOR");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileUseCase.getProfile(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should update all provided fields")
        void shouldUpdateAllProvidedFields() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Janet")
                    .lastName("Smith")
                    .phone("+254798765432")
                    .bio("Updated bio")
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .nationalId("12345678")
                    .kraPin("A012345678B")
                    .annualIncome(new BigDecimal("500000.00"))
                    .build();

            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse response = userProfileUseCase.updateProfile(USER_ID, request);

            assertThat(testUser.getFirstName()).isEqualTo("Janet");
            assertThat(testUser.getLastName()).isEqualTo("Smith");
            assertThat(testUser.getPhone()).isEqualTo("+254798765432");
            assertThat(testUser.getBio()).isEqualTo("Updated bio");
            assertThat(testUser.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(testUser.getNationalId()).isEqualTo("12345678");
            assertThat(testUser.getKraPin()).isEqualTo("A012345678B");
            assertThat(testUser.getAnnualIncome()).isEqualByComparingTo(new BigDecimal("500000.00"));
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Janet")
                    .build();

            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userProfileUseCase.updateProfile(USER_ID, request);

            assertThat(testUser.getFirstName()).isEqualTo("Janet");
            assertThat(testUser.getLastName()).isEqualTo("Doe"); // unchanged
            assertThat(testUser.getPhone()).isEqualTo("+254712345678"); // unchanged
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            UpdateProfileRequest request = UpdateProfileRequest.builder().firstName("Janet").build();
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileUseCase.updateProfile(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should change password when current password is correct")
        void shouldChangePasswordSuccessfully() {
            ChangePasswordRequest request = new ChangePasswordRequest("OldP@ss1", "NewStr0ng!Pass");
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("OldP@ss1", ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.encode("NewStr0ng!Pass")).thenReturn("$2a$10$newHash");

            userProfileUseCase.changePassword(USER_ID, request);

            assertThat(testUser.getPasswordHash()).isEqualTo("$2a$10$newHash");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should throw BusinessRuleException when current password is incorrect")
        void shouldThrowWhenCurrentPasswordIncorrect() {
            ChangePasswordRequest request = new ChangePasswordRequest("WrongP@ss1", "NewStr0ng!Pass");
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongP@ss1", ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> userProfileUseCase.changePassword(USER_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Current password is incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            ChangePasswordRequest request = new ChangePasswordRequest("OldP@ss1", "NewStr0ng!Pass");
            when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileUseCase.changePassword(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
