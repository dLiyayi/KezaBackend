package com.keza.infrastructure.security.oauth2;

/**
 * Port interface for OAuth2 user provisioning.
 * Defined in keza-infrastructure so that the OAuth2SuccessHandler can use it
 * without depending on keza-user. The implementation lives in keza-user module.
 */
public interface OAuth2UserProvisioningPort {

    /**
     * Finds an existing user by email, or auto-provisions a new user with the
     * INVESTOR role, a verified email flag, and a random password hash.
     *
     * @param email     the user's email from the OAuth2 provider
     * @param firstName the user's first name from the OAuth2 provider
     * @param lastName  the user's last name from the OAuth2 provider
     * @return OAuth2UserInfo containing the user's id, email, and role names
     */
    OAuth2UserInfo provisionOrGetUser(String email, String firstName, String lastName);
}
