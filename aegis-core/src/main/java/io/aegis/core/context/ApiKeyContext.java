package io.aegis.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for API Key based authentication.
 *
 * @param apiKey the API key value
 * @param keyName the name/identifier of the API key
 * @param source the source of the API key (e.g., "header", "query", "cookie")
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record ApiKeyContext(
        String apiKey,
        String keyName,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates an API key context with the given key.
     *
     * @param apiKey the API key
     */
    public ApiKeyContext(String apiKey) {
        this(apiKey, "X-API-Key", "header", new HashMap<>());
    }

    /**
     * Creates an API key context with the given key and name.
     *
     * @param apiKey the API key
     * @param keyName the name of the API key header/parameter
     */
    public ApiKeyContext(String apiKey, String keyName) {
        this(apiKey, keyName, "header", new HashMap<>());
    }

    /**
     * Creates an API key context with the given key, name, and source.
     *
     * @param apiKey the API key
     * @param keyName the name of the API key
     * @param source the source location
     */
    public ApiKeyContext(String apiKey, String keyName, String source) {
        this(apiKey, keyName, source, new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.API_KEY;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getSource() {
        return source;
    }
}
