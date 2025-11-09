package io.aegis.core.context;

import java.util.Map;
import java.util.HashMap;

/**
 * Authentication context for JWT (JSON Web Token) based authentication.
 *
 * @param token the JWT token string
 * @param source the source of the token (e.g., "header", "cookie")
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record JwtContext(
        String token,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates a JWT context with the given token.
     *
     * @param token the JWT token
     */
    public JwtContext(String token) {
        this(token, "header", new HashMap<>());
    }

    /**
     * Creates a JWT context with the given token and source.
     *
     * @param token the JWT token
     * @param source the source of the token
     */
    public JwtContext(String token, String source) {
        this(token, source, new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.JWT;
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
