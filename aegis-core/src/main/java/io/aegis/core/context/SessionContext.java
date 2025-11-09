package io.aegis.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for session-based authentication.
 *
 * @param sessionId the session identifier
 * @param sessionData session data/attributes
 * @param source the source of the session (e.g., "cookie", "header")
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record SessionContext(
        String sessionId,
        Map<String, Object> sessionData,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates a session context with a session ID.
     *
     * @param sessionId the session ID
     */
    public SessionContext(String sessionId) {
        this(sessionId, new HashMap<>(), "cookie", new HashMap<>());
    }

    /**
     * Creates a session context with a session ID and data.
     *
     * @param sessionId the session ID
     * @param sessionData the session data
     */
    public SessionContext(String sessionId, Map<String, Object> sessionData) {
        this(sessionId, sessionData, "cookie", new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.SESSION;
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
     * Gets a value from the session data.
     *
     * @param key the key
     * @param <T> the expected type
     * @return the value or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getSessionAttribute(String key) {
        return (T) sessionData.get(key);
    }
}
