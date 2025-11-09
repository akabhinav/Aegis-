package io.aegis.core.context;

import java.util.Map;
import java.util.Optional;

/**
 * Represents the context of an authentication request.
 * Contains all information needed to authenticate a request.
 * <p>
 * This is a sealed interface to ensure type safety and exhaustive pattern matching.
 *
 * @since 1.0.0
 */
public sealed interface AuthenticationContext permits
        JwtContext,
        OAuth2Context,
        ApiKeyContext,
        BasicAuthContext,
        MtlsContext,
        SamlContext,
        SessionContext,
        CustomContext {

    /**
     * Returns the type of authentication context.
     *
     * @return the context type
     */
    AuthenticationContextType getType();

    /**
     * Returns additional attributes associated with this context.
     *
     * @return a map of additional attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Gets an attribute value by key.
     *
     * @param key the attribute key
     * @param <T> the expected type of the attribute
     * @return an Optional containing the attribute value if present
     */
    @SuppressWarnings("unchecked")
    default <T> Optional<T> getAttribute(String key) {
        return Optional.ofNullable((T) getAttributes().get(key));
    }

    /**
     * Returns the source of this authentication context (e.g., "header", "cookie", "query").
     *
     * @return the source identifier
     */
    default String getSource() {
        return "unknown";
    }
}
