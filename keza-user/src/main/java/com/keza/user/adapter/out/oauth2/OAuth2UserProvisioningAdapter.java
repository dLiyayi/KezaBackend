package com.keza.user.adapter.out.oauth2;

import com.keza.common.exception.BusinessRuleException;
import com.keza.infrastructure.security.oauth2.OAuth2UserInfo;
import com.keza.infrastructure.security.oauth2.OAuth2UserProvisioningPort;
import com.keza.user.domain.model.User;
import com.keza.user.domain.model.UserRole;
import com.keza.user.domain.port.out.RoleRepository;
import com.keza.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link OAuth2UserProvisioningPort} that provisions or retrieves
 * users from the database during OAuth2 login flows (e.g. KCB, Google, Facebook, Apple).
 * <p>
 * If the user does not exist, a new account is created with:
 * <ul>
 *   <li>INVESTOR role</li>
 *   <li>Email marked as verified (OAuth2 provider verified it)</li>
 *   <li>Random password hash (user authenticates via OAuth2, not password)</li>
 *   <li>Auth provider and provider ID recorded</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keza.oauth2.enabled", havingValue = "true")
public class OAuth2UserProvisioningAdapter implements OAuth2UserProvisioningPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2UserInfo provisionOrGetUser(String email, String firstName, String lastName,
                                             String provider, String providerId) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseGet(() -> createUser(email, firstName, lastName, provider, providerId));

        Set<String> roleNames = user.getRoles().stream()
                .map(UserRole::getName)
                .collect(Collectors.toSet());

        return new OAuth2UserInfo(user.getId(), user.getEmail(), roleNames);
    }

    private User createUser(String email, String firstName, String lastName,
                            String provider, String providerId) {
        log.info("Auto-provisioning new user from OAuth2 login: {} via {}", email, provider);

        UserRole investorRole = roleRepository.findByName("INVESTOR")
                .orElseThrow(() -> new BusinessRuleException("Default role INVESTOR not found"));

        // Generate a random password hash; OAuth2 users don't use password login
        String randomPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());

        String authProvider = provider != null ? provider.toUpperCase() : "OAUTH2";

        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName.isBlank() ? firstName : lastName)
                .passwordHash(randomPasswordHash)
                .emailVerified(true)
                .authProvider(authProvider)
                .authProviderId(providerId)
                .roles(Set.of(investorRole))
                .build();

        user = userRepository.save(user);
        log.info("OAuth2 user provisioned: {} ({}) via {}", email, user.getId(), authProvider);
        return user;
    }
}
