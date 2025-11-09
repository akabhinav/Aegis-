package io.aegis.core.context;

import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for authentication context classes.
 */
class AuthenticationContextTest {

    @Test
    void shouldCreateJwtContext() {
        JwtContext context = new JwtContext("test-token");

        assertEquals("test-token", context.token());
        assertEquals("header", context.source());
        assertEquals(AuthenticationContextType.JWT, context.getType());
        assertNotNull(context.getAttributes());
    }

    @Test
    void shouldCreateJwtContextWithSource() {
        JwtContext context = new JwtContext("test-token", "cookie");

        assertEquals("test-token", context.token());
        assertEquals("cookie", context.source());
    }

    @Test
    void shouldCreateJwtContextWithAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key1", "value1");

        JwtContext context = new JwtContext("test-token", "header", attrs);

        assertEquals("value1", context.getAttribute("key1").orElse(null));
    }

    @Test
    void shouldCreateOAuth2Context() {
        OAuth2Context context = new OAuth2Context("access-token");

        assertEquals("access-token", context.accessToken());
        assertEquals("Bearer", context.tokenType());
        assertNull(context.scope());
        assertNull(context.idToken());
        assertEquals("header", context.source());
        assertEquals(AuthenticationContextType.OAUTH2, context.getType());
    }

    @Test
    void shouldCreateOAuth2ContextWithTokenType() {
        OAuth2Context context = new OAuth2Context("access-token", "Custom");

        assertEquals("access-token", context.accessToken());
        assertEquals("Custom", context.tokenType());
    }

    @Test
    void shouldCheckIdTokenPresence() {
        OAuth2Context withoutIdToken = new OAuth2Context("access-token");
        assertFalse(withoutIdToken.hasIdToken());

        OAuth2Context withIdToken = new OAuth2Context(
                "access-token", "Bearer", "openid", "id-token", "header", new HashMap<>()
        );
        assertTrue(withIdToken.hasIdToken());
    }

    @Test
    void shouldCreateApiKeyContext() {
        ApiKeyContext context = new ApiKeyContext("my-api-key");

        assertEquals("my-api-key", context.apiKey());
        assertEquals("X-API-Key", context.keyName());
        assertEquals("header", context.source());
        assertEquals(AuthenticationContextType.API_KEY, context.getType());
    }

    @Test
    void shouldCreateApiKeyContextWithCustomName() {
        ApiKeyContext context = new ApiKeyContext("my-api-key", "Custom-Key");

        assertEquals("my-api-key", context.apiKey());
        assertEquals("Custom-Key", context.keyName());
    }

    @Test
    void shouldCreateApiKeyContextWithSource() {
        ApiKeyContext context = new ApiKeyContext("my-api-key", "X-API-Key", "query");

        assertEquals("my-api-key", context.apiKey());
        assertEquals("query", context.source());
    }

    @Test
    void shouldCreateBasicAuthContext() {
        BasicAuthContext context = new BasicAuthContext("user", "pass");

        assertEquals("user", context.username());
        assertEquals("pass", context.password());
        assertEquals("header", context.source());
        assertEquals(AuthenticationContextType.BASIC, context.getType());
    }

    @Test
    void shouldCreateMtlsContext() {
        MtlsContext context = new MtlsContext((X509Certificate) null);

        assertNull(context.certificate());
        assertEquals("tls", context.source());
        assertEquals(AuthenticationContextType.MTLS, context.getType());
    }

    @Test
    void shouldCreateSamlContext() {
        SamlContext context = new SamlContext("saml-response");

        assertEquals("saml-response", context.samlResponse());
        assertNull(context.relayState());
        assertNull(context.issuer());
        assertEquals("post", context.source());
        assertEquals(AuthenticationContextType.SAML, context.getType());
    }

    @Test
    void shouldCreateSamlContextWithRelayState() {
        SamlContext context = new SamlContext("saml-response", "relay-state");

        assertEquals("saml-response", context.samlResponse());
        assertEquals("relay-state", context.relayState());
        assertTrue(context.hasRelayState());
    }

    @Test
    void shouldCheckRelayStatePresence() {
        SamlContext withoutRelay = new SamlContext("saml-response");
        assertFalse(withoutRelay.hasRelayState());

        SamlContext withRelay = new SamlContext("saml-response", "relay-state");
        assertTrue(withRelay.hasRelayState());
    }

    @Test
    void shouldCreateSessionContext() {
        SessionContext context = new SessionContext("session-123");

        assertEquals("session-123", context.sessionId());
        assertEquals("cookie", context.source());
        assertEquals(AuthenticationContextType.SESSION, context.getType());
        assertNotNull(context.sessionData());
    }

    @Test
    void shouldCreateSessionContextWithData() {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("user-id", "123");

        SessionContext context = new SessionContext("session-123", sessionData);

        assertEquals("123", context.getSessionAttribute("user-id"));
    }

    @Test
    void shouldCreateCustomContext() {
        CustomContext context = new CustomContext("my-auth", "credentials");

        assertEquals("my-auth", context.mechanism());
        assertEquals("credentials", context.credentials());
        assertEquals("custom", context.source());
        assertEquals(AuthenticationContextType.CUSTOM, context.getType());
    }

    @Test
    void shouldGetCredentialsFromCustomContext() {
        Map<String, String> creds = Map.of("username", "user", "password", "pass");
        CustomContext context = new CustomContext("my-auth", creds);

        Map<String, String> retrieved = context.getCredentials();
        assertEquals(creds, retrieved);
    }

    @Test
    void shouldGetAttributeFromContext() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("key1", "value1");

        JwtContext context = new JwtContext("token", "header", attrs);

        assertTrue(context.getAttribute("key1").isPresent());
        assertEquals("value1", context.getAttribute("key1").get());
        assertFalse(context.getAttribute("nonexistent").isPresent());
    }
}
