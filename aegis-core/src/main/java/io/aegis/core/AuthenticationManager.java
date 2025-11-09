package io.aegis.core;

import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.result.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central authentication manager that orchestrates multiple authentication providers.
 * <p>
 * The manager maintains a list of authentication providers and delegates authentication
 * requests to the appropriate provider based on the context type and provider support.
 * <p>
 * Thread-safe implementation using CopyOnWriteArrayList for provider management.
 *
 * @since 1.0.0
 */
public class AuthenticationManager {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);

    private final List<AuthenticationProvider<?>> providers = new CopyOnWriteArrayList<>();

    /**
     * Creates an authentication manager with the given providers.
     *
     * @param providers the authentication providers
     */
    public AuthenticationManager(List<AuthenticationProvider<?>> providers) {
        if (providers != null && !providers.isEmpty()) {
            // Sort providers by priority (highest first)
            List<AuthenticationProvider<?>> sorted = new ArrayList<>(providers);
            sorted.sort(Comparator.comparingInt(AuthenticationProvider::getPriority).reversed());
            this.providers.addAll(sorted);
        }
    }

    /**
     * Creates an empty authentication manager.
     */
    public AuthenticationManager() {
        this(List.of());
    }

    /**
     * Registers a new authentication provider.
     *
     * @param provider the provider to register
     */
    public void registerProvider(AuthenticationProvider<?> provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        logger.info("Registering authentication provider: {}", provider.getName());
        this.providers.add(provider);
        // Re-sort after adding
        this.providers.sort(Comparator.comparingInt(AuthenticationProvider::getPriority).reversed());
    }

    /**
     * Unregisters an authentication provider.
     *
     * @param provider the provider to unregister
     * @return true if the provider was removed
     */
    public boolean unregisterProvider(AuthenticationProvider<?> provider) {
        boolean removed = this.providers.remove(provider);
        if (removed) {
            logger.info("Unregistered authentication provider: {}", provider.getName());
        }
        return removed;
    }

    /**
     * Authenticates using the given authentication context.
     * <p>
     * Iterates through registered providers in priority order and uses the first
     * provider that supports the given context.
     *
     * @param context the authentication context
     * @return the authentication result
     * @throws AuthenticationException if no suitable provider is found or authentication fails
     */
    @SuppressWarnings("unchecked")
    public AuthenticationResult authenticate(AuthenticationContext context) {
        if (context == null) {
            logger.warn("Attempted authentication with null context");
            return AuthenticationResult.failure("Authentication context cannot be null");
        }

        logger.debug("Attempting authentication with context type: {}", context.getType());

        for (AuthenticationProvider<?> provider : providers) {
            if (provider.supports(context)) {
                logger.debug("Using provider: {} for context type: {}",
                        provider.getName(), context.getType());

                try {
                    // Cast is safe because we checked supports()
                    @SuppressWarnings("rawtypes")
                    AuthenticationProvider rawProvider = provider;
                    AuthenticationResult result = rawProvider.authenticate(context);

                    if (result.isSuccess()) {
                        logger.info("Authentication successful using provider: {}",
                                provider.getName());
                    } else {
                        logger.warn("Authentication failed using provider: {}",
                                provider.getName());
                    }

                    return result;
                } catch (Exception e) {
                    logger.error("Error during authentication with provider: {}",
                            provider.getName(), e);
                    return AuthenticationResult.failure(
                            "Authentication error: " + e.getMessage(), e);
                }
            }
        }

        logger.warn("No authentication provider found for context type: {}", context.getType());
        return AuthenticationResult.failure(
                "No authentication provider found for context type: " + context.getType());
    }

    /**
     * Gets the list of registered providers.
     *
     * @return a copy of the provider list
     */
    public List<AuthenticationProvider<?>> getProviders() {
        return new ArrayList<>(providers);
    }

    /**
     * Gets the number of registered providers.
     *
     * @return the provider count
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Checks if any provider supports the given context.
     *
     * @param context the authentication context
     * @return true if at least one provider supports the context
     */
    public boolean hasProviderFor(AuthenticationContext context) {
        return providers.stream()
                .anyMatch(provider -> provider.supports(context));
    }
}
