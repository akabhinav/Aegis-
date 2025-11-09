package io.aegis.apikey.store;

import io.aegis.core.user.UserPrincipal;

import java.util.Optional;

/**
 * Interface for API key storage and validation.
 * <p>
 * Implementations can use databases, in-memory caches, or external services
 * to store and validate API keys.
 *
 * @since 1.0.0
 */
public interface ApiKeyStore {

    /**
     * Validates an API key and returns the associated user principal.
     *
     * @param apiKey the API key to validate
     * @return Optional containing the user principal if the key is valid
     */
    Optional<UserPrincipal> validateApiKey(String apiKey);

    /**
     * Stores or updates an API key for a user.
     *
     * @param apiKey the API key
     * @param principal the user principal
     */
    void storeApiKey(String apiKey, UserPrincipal principal);

    /**
     * Revokes an API key.
     *
     * @param apiKey the API key to revoke
     * @return true if the key was revoked successfully
     */
    boolean revokeApiKey(String apiKey);

    /**
     * Checks if an API key exists.
     *
     * @param apiKey the API key
     * @return true if the key exists
     */
    boolean exists(String apiKey);
}
