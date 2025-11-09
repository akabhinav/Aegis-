package io.aegis.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for SAML 2.0 authentication.
 *
 * @param samlResponse the SAML response (base64 encoded)
 * @param relayState optional relay state parameter
 * @param issuer the SAML issuer (IdP entity ID)
 * @param source the source of the SAML assertion
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record SamlContext(
        String samlResponse,
        String relayState,
        String issuer,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates a SAML context with a SAML response.
     *
     * @param samlResponse the SAML response
     */
    public SamlContext(String samlResponse) {
        this(samlResponse, null, null, "post", new HashMap<>());
    }

    /**
     * Creates a SAML context with a SAML response and relay state.
     *
     * @param samlResponse the SAML response
     * @param relayState the relay state
     */
    public SamlContext(String samlResponse, String relayState) {
        this(samlResponse, relayState, null, "post", new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.SAML;
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
     * Checks if a relay state is present.
     *
     * @return true if relay state is present
     */
    public boolean hasRelayState() {
        return relayState != null && !relayState.isEmpty();
    }
}
