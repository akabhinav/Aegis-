package io.aegis.webauthn;

import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.context.AuthenticationContextType;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for WebAuthn.
 *
 * @param credentialId the credential ID (base64url encoded)
 * @param authenticatorData raw authenticator data
 * @param clientDataJSON client data JSON
 * @param signature the signature
 * @param userHandle optional user handle
 * @param source the source of the credential
 * @param attributes additional attributes
 * @since 1.0.0
 */
public record WebAuthnContext(
        String credentialId,
        byte[] authenticatorData,
        String clientDataJSON,
        byte[] signature,
        String userHandle,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    public WebAuthnContext(
            String credentialId,
            byte[] authenticatorData,
            String clientDataJSON,
            byte[] signature
    ) {
        this(credentialId, authenticatorData, clientDataJSON, signature,
                null, "webauthn", new HashMap<>());
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.CUSTOM; // WebAuthn is a custom type
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
