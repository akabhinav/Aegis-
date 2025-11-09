# WebAuthn/FIDO2 Implementation Guide

## Overview

WebAuthn (Web Authentication) is a modern, passwordless authentication standard that enables:
- Biometric authentication (Touch ID, Face ID, Windows Hello)
- Security keys (YubiKey, Titan Key)
- Passkeys (platform authenticators)
- Phishing-resistant authentication

## Architecture

```
┌─────────────┐
│   Browser   │
│  WebAuthn   │
│     API     │
└──────┬──────┘
       │
       ↓ Registration/Authentication
┌──────────────────────────────┐
│  WebAuthnProvider            │
│  - Registration              │
│  - Authentication            │
│  - Credential Management     │
└──────┬───────────────────────┘
       │
       ↓
┌──────────────────────────────┐
│  CredentialRepository        │
│  - Store credentials         │
│  - Lookup credentials        │
│  - Revoke credentials        │
└──────────────────────────────┘
```

## Module Structure

```
aegis-webauthn/
├── pom.xml
└── src/main/java/io/aegis/webauthn/
    ├── WebAuthnProvider.java
    ├── WebAuthnConfiguration.java
    ├── WebAuthnRegistrationOptions.java
    ├── WebAuthnAuthenticationOptions.java
    ├── model/
    │   ├── PublicKeyCredential.java
    │   ├── AuthenticatorData.java
    │   ├── AttestationObject.java
    │   └── CredentialDescriptor.java
    ├── repository/
    │   ├── CredentialRepository.java
    │   └── InMemoryCredentialRepository.java
    ├── validator/
    │   ├── AttestationValidator.java
    │   └── AssertionValidator.java
    └── util/
        ├── ChallengeGenerator.java
        └── CredentialIdGenerator.java
```

## Dependencies

```xml
<dependencies>
    <!-- Core Aegis -->
    <dependency>
        <groupId>io.aegis</groupId>
        <artifactId>aegis-core</artifactId>
    </dependency>

    <!-- WebAuthn Library -->
    <dependency>
        <groupId>com.webauthn4j</groupId>
        <artifactId>webauthn4j-core</artifactId>
        <version>0.21.4.RELEASE</version>
    </dependency>

    <!-- CBOR (for attestation objects) -->
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-cbor</artifactId>
    </dependency>

    <!-- Bouncy Castle (crypto) -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
    </dependency>
</dependencies>
```

## Implementation

### 1. WebAuthn Context

```java
package io.aegis.webauthn.context;

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
        return AuthenticationContextType.WEBAUTHN;
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
```

### 2. WebAuthn Provider

```java
package io.aegis.webauthn;

import com.webauthn4j.*;
import com.webauthn4j.data.*;
import com.webauthn4j.validator.*;
import io.aegis.core.AuthenticationProvider;
import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.UserPrincipal;
import io.aegis.webauthn.context.WebAuthnContext;
import io.aegis.webauthn.repository.CredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * WebAuthn authentication provider.
 */
public class WebAuthnProvider implements AuthenticationProvider<WebAuthnContext> {

    private static final Logger logger = LoggerFactory.getLogger(WebAuthnProvider.class);

    private final WebAuthnConfiguration configuration;
    private final CredentialRepository credentialRepository;
    private final WebAuthnManager webAuthnManager;

    public WebAuthnProvider(
            WebAuthnConfiguration configuration,
            CredentialRepository credentialRepository
    ) {
        this.configuration = configuration;
        this.credentialRepository = credentialRepository;
        this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    }

    @Override
    public AuthenticationResult authenticate(WebAuthnContext context) {
        try {
            // Retrieve stored credential
            byte[] credentialId = Base64.getUrlDecoder().decode(context.credentialId());
            Optional<StoredCredential> storedCred =
                    credentialRepository.findByCredentialId(credentialId);

            if (storedCred.isEmpty()) {
                return AuthenticationResult.failure("Unknown credential");
            }

            StoredCredential credential = storedCred.get();

            // Build authentication data
            AuthenticationRequest authRequest = new AuthenticationRequest(
                    credentialId,
                    context.authenticatorData(),
                    context.clientDataJSON().getBytes(),
                    context.signature()
            );

            // Validate authentication
            AuthenticationData authData = webAuthnManager.validate(
                    authRequest,
                    new AuthenticationParameters(
                            configuration.getServerProperty(),
                            credential.getPublicKey(),
                            credential.getCounter(),
                            true // user verification required
                    )
            );

            // Update counter
            credentialRepository.updateCounter(
                    credentialId,
                    authData.getAuthenticatorData().getSignCount()
            );

            // Return authenticated user
            UserPrincipal principal = credential.getUserPrincipal();
            logger.info("WebAuthn authentication successful for user: {}",
                    principal.getUsername());

            return AuthenticationResult.success(principal);

        } catch (ValidationException e) {
            logger.error("WebAuthn validation failed", e);
            return AuthenticationResult.failure("Authentication validation failed", e);
        } catch (Exception e) {
            logger.error("WebAuthn authentication error", e);
            return AuthenticationResult.failure("Authentication error: " + e.getMessage(), e);
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
     * Generate registration options for a new credential.
     */
    public PublicKeyCredentialCreationOptions generateRegistrationOptions(
            String username,
            String displayName
    ) {
        byte[] userId = username.getBytes();
        byte[] challenge = ChallengeGenerator.generate();

        return PublicKeyCredentialCreationOptions.builder()
                .rp(new RelyingPartyIdentity(
                        configuration.getRelyingPartyId(),
                        configuration.getRelyingPartyName()
                ))
                .user(new UserIdentity(
                        userId,
                        username,
                        displayName
                ))
                .challenge(challenge)
                .pubKeyCredParams(List.of(
                        // Prefer ES256 (ECDSA)
                        new PublicKeyCredentialParameters(
                                PublicKeyCredentialType.PUBLIC_KEY,
                                COSEAlgorithmIdentifier.ES256
                        ),
                        // Fallback to RS256 (RSA)
                        new PublicKeyCredentialParameters(
                                PublicKeyCredentialType.PUBLIC_KEY,
                                COSEAlgorithmIdentifier.RS256
                        )
                ))
                .timeout(60000L) // 60 seconds
                .authenticatorSelection(new AuthenticatorSelectionCriteria(
                        AuthenticatorAttachment.PLATFORM, // Platform authenticator
                        true, // Require resident key
                        UserVerificationRequirement.REQUIRED
                ))
                .attestation(AttestationConveyancePreference.DIRECT)
                .build();
    }

    /**
     * Generate authentication options.
     */
    public PublicKeyCredentialRequestOptions generateAuthenticationOptions(
            String username
    ) {
        byte[] challenge = ChallengeGenerator.generate();

        // Get user's credentials
        List<PublicKeyCredentialDescriptor> allowCredentials =
                credentialRepository.findByUsername(username).stream()
                        .map(cred -> new PublicKeyCredentialDescriptor(
                                PublicKeyCredentialType.PUBLIC_KEY,
                                cred.getCredentialId()
                        ))
                        .toList();

        return PublicKeyCredentialRequestOptions.builder()
                .challenge(challenge)
                .timeout(60000L)
                .rpId(configuration.getRelyingPartyId())
                .allowCredentials(allowCredentials)
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build();
    }
}
```

### 3. Configuration

```java
package io.aegis.webauthn;

public class WebAuthnConfiguration {

    private String relyingPartyId;
    private String relyingPartyName;
    private List<String> allowedOrigins;
    private AttestationConveyancePreference attestation;
    private boolean requireResidentKey;
    private UserVerificationRequirement userVerification;

    // Getters, setters, builder...

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        // Builder implementation
    }
}
```

### 4. Frontend Integration

```javascript
// registration.js
async function registerWebAuthn(username, displayName) {
    // 1. Get registration options from server
    const optionsResponse = await fetch('/api/webauthn/register/options', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, displayName })
    });
    const options = await optionsResponse.json();

    // 2. Convert base64url to ArrayBuffer
    options.challenge = base64urlDecode(options.challenge);
    options.user.id = base64urlDecode(options.user.id);

    // 3. Call WebAuthn API
    const credential = await navigator.credentials.create({
        publicKey: options
    });

    // 4. Send credential to server
    const response = await fetch('/api/webauthn/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            id: credential.id,
            rawId: base64urlEncode(credential.rawId),
            type: credential.type,
            response: {
                attestationObject: base64urlEncode(
                    credential.response.attestationObject
                ),
                clientDataJSON: base64urlEncode(
                    credential.response.clientDataJSON
                )
            }
        })
    });

    return response.json();
}

// authentication.js
async function authenticateWebAuthn(username) {
    // 1. Get authentication options
    const optionsResponse = await fetch('/api/webauthn/authenticate/options', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
    });
    const options = await optionsResponse.json();

    // 2. Convert base64url
    options.challenge = base64urlDecode(options.challenge);
    options.allowCredentials = options.allowCredentials.map(cred => ({
        ...cred,
        id: base64urlDecode(cred.id)
    }));

    // 3. Call WebAuthn API
    const assertion = await navigator.credentials.get({
        publicKey: options
    });

    // 4. Send assertion to server
    const response = await fetch('/api/webauthn/authenticate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            id: assertion.id,
            rawId: base64urlEncode(assertion.rawId),
            type: assertion.type,
            response: {
                authenticatorData: base64urlEncode(
                    assertion.response.authenticatorData
                ),
                clientDataJSON: base64urlEncode(
                    assertion.response.clientDataJSON
                ),
                signature: base64urlEncode(assertion.response.signature),
                userHandle: assertion.response.userHandle
                    ? base64urlEncode(assertion.response.userHandle)
                    : null
            }
        })
    });

    return response.json();
}

// Utility functions
function base64urlDecode(str) {
    return Uint8Array.from(
        atob(str.replace(/-/g, '+').replace(/_/g, '/')),
        c => c.charCodeAt(0)
    );
}

function base64urlEncode(buffer) {
    return btoa(String.fromCharCode(...new Uint8Array(buffer)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
}
```

## Spring Boot Auto-Configuration

```java
@Configuration
@ConditionalOnProperty(prefix = "aegis.webauthn", name = "enabled", havingValue = "true")
public class WebAuthnAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CredentialRepository credentialRepository() {
        return new InMemoryCredentialRepository();
    }

    @Bean
    public WebAuthnProvider webAuthnProvider(
            AegisProperties properties,
            CredentialRepository repository
    ) {
        WebAuthnConfiguration config = createConfig(properties.getWebauthn());
        return new WebAuthnProvider(config, repository);
    }

    @Bean
    public WebAuthnController webAuthnController(
            WebAuthnProvider provider,
            CredentialRepository repository
    ) {
        return new WebAuthnController(provider, repository);
    }

    private WebAuthnConfiguration createConfig(
            AegisProperties.WebAuthnProperties props
    ) {
        return WebAuthnConfiguration.builder()
                .relyingPartyId(props.getRelyingPartyId())
                .relyingPartyName(props.getRelyingPartyName())
                .allowedOrigins(props.getAllowedOrigins())
                .build();
    }
}
```

## Configuration Properties

```yaml
aegis:
  webauthn:
    enabled: true
    relying-party-id: myapp.com
    relying-party-name: My Application
    allowed-origins:
      - https://myapp.com
      - https://www.myapp.com
    require-resident-key: true
    user-verification: required
    attestation: direct
```

## Testing

```java
@Test
void testWebAuthnAuthentication() {
    // Setup
    WebAuthnConfiguration config = WebAuthnConfiguration.builder()
            .relyingPartyId("localhost")
            .relyingPartyName("Test App")
            .build();

    InMemoryCredentialRepository repository = new InMemoryCredentialRepository();
    WebAuthnProvider provider = new WebAuthnProvider(config, repository);

    // Register credential
    PublicKeyCredentialCreationOptions regOptions =
            provider.generateRegistrationOptions("testuser", "Test User");

    // Simulate credential creation (you'll need test data)
    StoredCredential credential = createTestCredential();
    repository.store(credential);

    // Authenticate
    WebAuthnContext context = new WebAuthnContext(
            base64url(credential.getCredentialId()),
            authenticatorData,
            clientDataJSON,
            signature
    );

    AuthenticationResult result = provider.authenticate(context);

    // Verify
    assertTrue(result.isSuccess());
    assertEquals("testuser", result.getPrincipal().get().getUsername());
}
```

## Security Considerations

1. **Challenge Storage**: Store challenges in Redis with short TTL
2. **Origin Validation**: Always validate origin matches allowed list
3. **HTTPS Only**: WebAuthn only works over HTTPS (except localhost)
4. **Counter Verification**: Check signature counter to detect cloned authenticators
5. **Timeout**: Keep challenge timeout short (30-60 seconds)
6. **User Verification**: Require user verification for sensitive operations

## Browser Compatibility

- ✅ Chrome 67+
- ✅ Firefox 60+
- ✅ Safari 13+
- ✅ Edge 18+
- ✅ iOS Safari 14.5+
- ✅ Android Chrome 70+

## Next Steps

1. Implement credential repository with database backend
2. Add credential management UI (list, revoke)
3. Support conditional UI (autocomplete)
4. Implement attestation verification
5. Add support for security keys
6. Create comprehensive demo
