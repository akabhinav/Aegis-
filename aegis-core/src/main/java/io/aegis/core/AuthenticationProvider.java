package io.aegis.core;

import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.result.AuthenticationResult;

/**
 * Core interface for all authentication providers in Aegis.
 * <p>
 * Implementations should be stateless and thread-safe. Each provider
 * is responsible for authenticating requests using a specific mechanism
 * (JWT, OAuth, API Key, etc.).
 *
 * @param <T> the type of authentication context this provider handles
 * @since 1.0.0
 */
public interface AuthenticationProvider<T extends AuthenticationContext> {

    /**
     * Attempts to authenticate the given context.
     *
     * @param context the authentication context containing credentials or tokens
     * @return the authentication result indicating success or failure
     * @throws AuthenticationException if an error occurs during authentication
     */
    AuthenticationResult authenticate(T context);

    /**
     * Determines if this provider supports the given authentication context.
     *
     * @param context the authentication context to check
     * @return true if this provider can handle the context, false otherwise
     */
    boolean supports(AuthenticationContext context);

    /**
     * Returns the name of this authentication provider.
     * Used for logging and debugging purposes.
     *
     * @return the provider name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the priority of this provider. Higher values indicate higher priority.
     * When multiple providers support the same context, the one with highest priority wins.
     *
     * @return the priority value (default is 0)
     */
    default int getPriority() {
        return 0;
    }
}
