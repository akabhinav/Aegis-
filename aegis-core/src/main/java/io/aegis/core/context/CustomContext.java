package io.aegis.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication context for proprietary or non-standard authentication mechanisms.
 * This allows developers to extend Aegis with their own authentication schemes.
 *
 * @param mechanism the name of the custom authentication mechanism
 * @param credentials the credentials or authentication data
 * @param source the source of the authentication data
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record CustomContext(
        String mechanism,
        Object credentials,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates a custom context with a mechanism name and credentials.
     *
     * @param mechanism the mechanism name
     * @param credentials the credentials
     */
    public CustomContext(String mechanism, Object credentials) {
        this(mechanism, credentials, "custom", new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.CUSTOM;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getSource() {
        return source;
    }

    /**
     * Gets the credentials cast to the expected type.
     *
     * @param <T> the expected type
     * @return the credentials
     */
    @SuppressWarnings("unchecked")
    public <T> T getCredentials() {
        return (T) credentials;
    }
}
