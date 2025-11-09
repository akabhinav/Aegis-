package io.aegis.core.result;

import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthenticationResult.
 */
class AuthenticationResultTest {

    @Test
    void shouldCreateSuccessResult() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .build();

        AuthenticationResult result = AuthenticationResult.success(principal);

        assertTrue(result.isSuccess());
        assertTrue(result.getPrincipal().isPresent());
        assertEquals("john", result.getPrincipal().get().getUsername());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void shouldCreateSuccessResultWithMetadata() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .build();

        Map<String, Object> metadata = Map.of("login-method", "password");

        AuthenticationResult result = AuthenticationResult.success(principal, metadata);

        assertTrue(result.isSuccess());
        assertTrue(result instanceof AuthenticationResult.Success);

        AuthenticationResult.Success success = (AuthenticationResult.Success) result;
        assertEquals("password", success.metadata().get("login-method"));
    }

    @Test
    void shouldCreateFailureResult() {
        AuthenticationResult result = AuthenticationResult.failure("Invalid credentials");

        assertFalse(result.isSuccess());
        assertTrue(result.getPrincipal().isEmpty());
        assertTrue(result instanceof AuthenticationResult.Failure);

        AuthenticationResult.Failure failure = (AuthenticationResult.Failure) result;
        assertEquals("Invalid credentials", failure.reason());
        assertNotNull(failure.getTimestamp());
    }

    @Test
    void shouldCreateFailureResultWithError() {
        Exception error = new RuntimeException("Database error");
        AuthenticationResult result = AuthenticationResult.failure("Authentication failed", error);

        assertFalse(result.isSuccess());
        assertTrue(result instanceof AuthenticationResult.Failure);

        AuthenticationResult.Failure failure = (AuthenticationResult.Failure) result;
        assertEquals("Authentication failed", failure.reason());
        assertTrue(failure.getError().isPresent());
        assertEquals(error, failure.getError().get());
    }

    @Test
    void shouldCreateFailureResultWithMetadata() {
        Map<String, Object> metadata = Map.of("attempts", 3);
        AuthenticationResult result = AuthenticationResult.failure("Too many attempts", metadata);

        assertFalse(result.isSuccess());
        assertTrue(result instanceof AuthenticationResult.Failure);

        AuthenticationResult.Failure failure = (AuthenticationResult.Failure) result;
        assertEquals("Too many attempts", failure.reason());
        assertEquals(3, failure.metadata().get("attempts"));
    }

    @Test
    void shouldSupportPatternMatching() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .build();

        AuthenticationResult successResult = AuthenticationResult.success(principal);
        AuthenticationResult failureResult = AuthenticationResult.failure("Failed");

        // Pattern matching with switch (Java 21)
        String successMessage = switch (successResult) {
            case AuthenticationResult.Success(var p, var ts, var meta) ->
                    "Welcome, " + p.getUsername();
            case AuthenticationResult.Failure(var reason, var ts, var err, var meta) ->
                    "Failed: " + reason;
        };

        String failureMessage = switch (failureResult) {
            case AuthenticationResult.Success(var p, var ts, var meta) ->
                    "Welcome, " + p.getUsername();
            case AuthenticationResult.Failure(var reason, var ts, var err, var meta) ->
                    "Failed: " + reason;
        };

        assertEquals("Welcome, john", successMessage);
        assertEquals("Failed: Failed", failureMessage);
    }

    @Test
    void shouldAccessSuccessDetails() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .addRole("USER")
                .build();

        Map<String, Object> metadata = Map.of("method", "JWT");

        AuthenticationResult.Success success = new AuthenticationResult.Success(
                principal,
                metadata
        );

        assertEquals(principal, success.principal());
        assertEquals("JWT", success.metadata().get("method"));
        assertNotNull(success.timestamp());
    }

    @Test
    void shouldAccessFailureDetails() {
        Exception error = new IllegalArgumentException("Invalid token");
        Map<String, Object> metadata = Map.of("token-type", "JWT");

        AuthenticationResult.Failure failure = new AuthenticationResult.Failure(
                "Token validation failed",
                error,
                metadata
        );

        assertEquals("Token validation failed", failure.reason());
        assertTrue(failure.getError().isPresent());
        assertEquals(error, failure.getError().get());
        assertEquals("JWT", failure.metadata().get("token-type"));
        assertNotNull(failure.timestamp());
    }

    @Test
    void shouldHandleNullErrorInFailure() {
        AuthenticationResult.Failure failure = new AuthenticationResult.Failure("Failed");

        assertTrue(failure.getError().isEmpty());
    }
}
