package io.aegis.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for HTTP Basic Authentication.
 *
 * @param username the username
 * @param password the password
 * @param source the source of the credentials
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record BasicAuthContext(
        String username,
        String password,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates a basic auth context with username and password.
     *
     * @param username the username
     * @param password the password
     */
    public BasicAuthContext(String username, String password) {
        this(username, password, "header", new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.BASIC;
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
