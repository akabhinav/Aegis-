package io.aegis.basic.credentials;

import io.aegis.core.user.UserPrincipal;

import java.util.Optional;

/**
 * Interface for storing and validating user credentials.
 *
 * @since 1.0.0
 */
public interface CredentialsStore {

    /**
     * Validates username and password credentials.
     *
     * @param username the username
     * @param password the password
     * @return Optional containing the user principal if credentials are valid
     */
    Optional<UserPrincipal> validateCredentials(String username, String password);

    /**
     * Stores or updates credentials for a user.
     *
     * @param username the username
     * @param password the password (should be hashed)
     * @param principal the user principal
     */
    void storeCredentials(String username, String password, UserPrincipal principal);

    /**
     * Removes credentials for a user.
     *
     * @param username the username
     * @return true if credentials were removed
     */
    boolean removeCredentials(String username);
}
