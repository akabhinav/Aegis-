package io.aegis.apikey.store;

import io.aegis.core.user.UserPrincipal;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of API key store.
 * <p>
 * Suitable for development and testing. For production use, consider
 * a persistent storage implementation.
 *
 * @since 1.0.0
 */
public class InMemoryApiKeyStore implements ApiKeyStore {

    private final ConcurrentHashMap<String, UserPrincipal> apiKeys = new ConcurrentHashMap<>();

    @Override
    public Optional<UserPrincipal> validateApiKey(String apiKey) {
        return Optional.ofNullable(apiKeys.get(apiKey));
    }

    @Override
    public void storeApiKey(String apiKey, UserPrincipal principal) {
        apiKeys.put(apiKey, principal);
    }

    @Override
    public boolean revokeApiKey(String apiKey) {
        return apiKeys.remove(apiKey) != null;
    }

    @Override
    public boolean exists(String apiKey) {
        return apiKeys.containsKey(apiKey);
    }

    /**
     * Gets the number of stored API keys.
     *
     * @return the count of API keys
     */
    public int size() {
        return apiKeys.size();
    }

    /**
     * Clears all API keys.
     */
    public void clear() {
        apiKeys.clear();
    }
}
