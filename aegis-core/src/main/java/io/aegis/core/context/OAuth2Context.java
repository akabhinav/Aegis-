package io.aegis.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for OAuth 2.0 / OpenID Connect authentication.
 *
 * @param accessToken the OAuth 2.0 access token
 * @param tokenType the type of token (e.g., "Bearer")
 * @param scope the granted scopes
 * @param idToken optional OpenID Connect ID token
 * @param source the source of the token
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record OAuth2Context(
        String accessToken,
        String tokenType,
        String scope,
        String idToken,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates an OAuth2 context with just an access token.
     *
     * @param accessToken the access token
     */
    public OAuth2Context(String accessToken) {
        this(accessToken, "Bearer", null, null, "header", new HashMap<>());
    }

    /**
     * Creates an OAuth2 context with access token and token type.
     *
     * @param accessToken the access token
     * @param tokenType the token type
     */
    public OAuth2Context(String accessToken, String tokenType) {
        this(accessToken, tokenType, null, null, "header", new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.OAUTH2;
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
     * Checks if this context includes an ID token (OpenID Connect).
     *
     * @return true if an ID token is present
     */
    public boolean hasIdToken() {
        return idToken != null && !idToken.isEmpty();
    }
}
