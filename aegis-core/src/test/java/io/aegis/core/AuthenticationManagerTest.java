package io.aegis.core;

import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.context.JwtContext;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthenticationManager.
 */
class AuthenticationManagerTest {

    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        authenticationManager = new AuthenticationManager();
    }

    @Test
    void shouldRegisterProvider() {
        TestAuthenticationProvider provider = new TestAuthenticationProvider();
        authenticationManager.registerProvider(provider);

        assertEquals(1, authenticationManager.getProviderCount());
    }

    @Test
    void shouldUnregisterProvider() {
        TestAuthenticationProvider provider = new TestAuthenticationProvider();
        authenticationManager.registerProvider(provider);

        boolean removed = authenticationManager.unregisterProvider(provider);

        assertTrue(removed);
        assertEquals(0, authenticationManager.getProviderCount());
    }

    @Test
    void shouldAuthenticateWithSupportedProvider() {
        TestAuthenticationProvider provider = new TestAuthenticationProvider();
        authenticationManager.registerProvider(provider);

        JwtContext context = new JwtContext("test-token");
        AuthenticationResult result = authenticationManager.authenticate(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getPrincipal().isPresent());
        assertEquals("test-user", result.getPrincipal().get().getUsername());
    }

    @Test
    void shouldFailWhenNoProviderSupportsContext() {
        JwtContext context = new JwtContext("test-token");
        AuthenticationResult result = authenticationManager.authenticate(context);

        assertFalse(result.isSuccess());
    }

    @Test
    void shouldFailWithNullContext() {
        AuthenticationResult result = authenticationManager.authenticate(null);

        assertFalse(result.isSuccess());
    }

    @Test
    void shouldSortProvidersByPriority() {
        TestAuthenticationProvider lowPriority = new TestAuthenticationProvider(10);
        TestAuthenticationProvider highPriority = new TestAuthenticationProvider(100);

        authenticationManager.registerProvider(lowPriority);
        authenticationManager.registerProvider(highPriority);

        List<AuthenticationProvider<?>> providers = authenticationManager.getProviders();

        assertEquals(100, providers.get(0).getPriority());
        assertEquals(10, providers.get(1).getPriority());
    }

    @Test
    void shouldCheckIfProviderExistsForContext() {
        TestAuthenticationProvider provider = new TestAuthenticationProvider();
        authenticationManager.registerProvider(provider);

        JwtContext context = new JwtContext("test-token");

        assertTrue(authenticationManager.hasProviderFor(context));
    }

    /**
     * Test authentication provider for testing purposes.
     */
    static class TestAuthenticationProvider implements AuthenticationProvider<JwtContext> {

        private final int priority;

        TestAuthenticationProvider() {
            this(0);
        }

        TestAuthenticationProvider(int priority) {
            this.priority = priority;
        }

        @Override
        public AuthenticationResult authenticate(JwtContext context) {
            UserPrincipal principal = DefaultUserPrincipal.builder()
                    .id("1")
                    .username("test-user")
                    .email("test@example.com")
                    .addRole("USER")
                    .build();

            return AuthenticationResult.success(principal);
        }

        @Override
        public boolean supports(AuthenticationContext context) {
            return context instanceof JwtContext;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}
