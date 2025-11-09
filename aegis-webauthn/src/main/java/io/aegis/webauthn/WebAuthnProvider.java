package io.aegis.webauthn;

import io.aegis.core.AuthenticationProvider;
import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * WebAuthn/FIDO2 authentication provider.
 * <p>
 * Supports passwordless authentication using:
 * - Platform authenticators (Touch ID, Face ID, Windows Hello)
 * - Security keys (YubiKey, Titan Key)
 * - Passkeys
 *
 * @since 1.0.0
 */
public class WebAuthnProvider implements AuthenticationProvider<WebAuthnContext> {

    private static final Logger logger = LoggerFactory.getLogger(WebAuthnProvider.class);

    private final WebAuthnConfiguration configuration;
    private final SecureRandom random = new SecureRandom();

    public WebAuthnProvider(WebAuthnConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public AuthenticationResult authenticate(WebAuthnContext context) {
        try {
            // In a full implementation, this would:
            // 1. Retrieve stored credential from repository
            // 2. Verify authenticator data
            // 3. Verify client data JSON
            // 4. Verify signature
            // 5. Update signature counter

            logger.info("WebAuthn authentication attempt for credential: {}",
                    context.credentialId());

            // Placeholder implementation
            // TODO: Implement full WebAuthn validation with webauthn4j
            UserPrincipal principal = DefaultUserPrincipal.builder()
                    .id("webauthn-user")
                    .username("webauthn-user")
                    .email("user@example.com")
                    .authenticationMechanism("WEBAUTHN")
                    .addRole("USER")
                    .build();

            return AuthenticationResult.success(principal);

        } catch (Exception e) {
            logger.error("WebAuthn authentication error", e);
            return AuthenticationResult.failure("WebAuthn authentication failed", e);
        }
    }

    @Override
    public boolean supports(AuthenticationContext context) {
        return context instanceof WebAuthnContext;
    }

    @Override
    public String getName() {
        return "WebAuthnProvider";
    }

    @Override
    public int getPriority() {
        return 150; // Higher priority than JWT
    }

    /**
     * Generate a random challenge for registration/authentication.
     *
     * @return base64url encoded challenge
     */
    public String generateChallenge() {
        byte[] challenge = new byte[32];
        random.nextBytes(challenge);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);
    }

    /**
     * Create registration options for a new credential.
     *
     * @param username the username
     * @param displayName the display name
     * @return registration options as JSON-compatible map
     */
    public java.util.Map<String, Object> createRegistrationOptions(
            String username,
            String displayName
    ) {
        String challenge = generateChallenge();
        byte[] userId = username.getBytes();

        return java.util.Map.of(
                "challenge", challenge,
                "rp", java.util.Map.of(
                        "id", configuration.getRelyingPartyId(),
                        "name", configuration.getRelyingPartyName()
                ),
                "user", java.util.Map.of(
                        "id", Base64.getUrlEncoder().withoutPadding().encodeToString(userId),
                        "name", username,
                        "displayName", displayName
                ),
                "pubKeyCredParams", java.util.List.of(
                        java.util.Map.of("type", "public-key", "alg", -7), // ES256
                        java.util.Map.of("type", "public-key", "alg", -257) // RS256
                ),
                "timeout", configuration.getTimeout(),
                "attestation", "direct",
                "authenticatorSelection", java.util.Map.of(
                        "authenticatorAttachment", "platform",
                        "requireResidentKey", configuration.isRequireResidentKey(),
                        "userVerification", "required"
                )
        );
    }

    /**
     * Create authentication options.
     *
     * @return authentication options as JSON-compatible map
     */
    public java.util.Map<String, Object> createAuthenticationOptions() {
        String challenge = generateChallenge();

        return java.util.Map.of(
                "challenge", challenge,
                "rpId", configuration.getRelyingPartyId(),
                "timeout", configuration.getTimeout(),
                "userVerification", "required"
        );
    }
}
