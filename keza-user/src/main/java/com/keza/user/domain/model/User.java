package com.keza.user.domain.model;

import com.keza.common.domain.model.SoftDeletableEntity;
import com.keza.common.enums.KycStatus;
import com.keza.common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends SoftDeletableEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    @Builder.Default
    private UserType userType = UserType.INVESTOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "kra_pin", length = 20)
    private String kraPin;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(length = 20)
    private String gender;

    @Column(name = "country_of_residence", length = 100)
    private String countryOfResidence;

    @Column(length = 100)
    private String citizenship;

    @Column(name = "net_worth", precision = 15, scale = 2)
    private BigDecimal netWorth;

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private String authProvider = "LOCAL";

    @Column(name = "auth_provider_id")
    private String authProviderId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.locked = true;
            this.lockedUntil = Instant.now().plusSeconds(1800); // 30 minutes
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.locked = false;
        this.lockedUntil = null;
    }

    public boolean isAccountLocked() {
        if (!locked) return false;
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            resetFailedLoginAttempts();
            return false;
        }
        return true;
    }
}
