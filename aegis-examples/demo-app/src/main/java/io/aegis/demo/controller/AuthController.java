package io.aegis.demo.controller;

import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import io.aegis.jwt.JwtGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Authentication controller for JWT login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtGenerator jwtGenerator;

    /**
     * Login endpoint - generates JWT token for valid credentials.
     * This is a simplified demo. In production, validate against a database.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        // Simulate credential validation
        // In production, check against database with hashed passwords
        UserPrincipal principal = validateAndGetUser(request.username, request.password);

        if (principal == null) {
            Map<String, Object> error = Map.of(
                    "error", "Invalid credentials",
                    "message", "Username or password is incorrect"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Generate JWT token
        String token = jwtGenerator.generateToken(principal);
        String refreshToken = jwtGenerator.generateRefreshToken(principal);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600); // 1 hour
        response.put("user", Map.of(
                "id", principal.getId(),
                "username", principal.getUsername(),
                "email", principal.getEmail(),
                "displayName", principal.getDisplayName(),
                "roles", principal.getRoles()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Get test credentials for demo purposes.
     */
    @GetMapping("/test-credentials")
    public ResponseEntity<Map<String, Object>> getTestCredentials() {
        Map<String, Object> response = new HashMap<>();

        response.put("users", java.util.List.of(
                Map.of(
                        "username", "demo",
                        "password", "password",
                        "roles", Set.of("USER"),
                        "description", "Regular user account"
                ),
                Map.of(
                        "username", "alice",
                        "password", "alice123",
                        "roles", Set.of("USER", "MANAGER"),
                        "description", "Manager account with elevated permissions"
                ),
                Map.of(
                        "username", "bob",
                        "password", "bob123",
                        "roles", Set.of("USER"),
                        "description", "Regular user account"
                ),
                Map.of(
                        "username", "admin",
                        "password", "admin123",
                        "roles", Set.of("USER", "ADMIN"),
                        "description", "Administrator account with full access"
                )
        ));

        response.put("apiKeys", java.util.List.of(
                Map.of(
                        "key", "demo-key-alice-12345",
                        "username", "alice",
                        "roles", Set.of("USER"),
                        "description", "Alice's API key"
                ),
                Map.of(
                        "key", "demo-key-admin-67890",
                        "username", "admin",
                        "roles", Set.of("USER", "ADMIN"),
                        "description", "Admin API key with full access"
                ),
                Map.of(
                        "key", "demo-key-service-abc123",
                        "username", "service-bot",
                        "roles", Set.of("SERVICE"),
                        "description", "Service account API key"
                )
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Simulated user validation.
     * In production, use a proper user service with database and password hashing.
     */
    private UserPrincipal validateAndGetUser(String username, String password) {
        // Demo credentials - DO NOT use in production
        if ("demo".equals(username) && "password".equals(password)) {
            return createDemoUser();
        } else if ("alice".equals(username) && "alice123".equals(password)) {
            return createAliceUser();
        } else if ("bob".equals(username) && "bob123".equals(password)) {
            return createBobUser();
        } else if ("admin".equals(username) && "admin123".equals(password)) {
            return createAdminUser();
        }

        return null;
    }

    private UserPrincipal createDemoUser() {
        return DefaultUserPrincipal.builder()
                .id("user-1")
                .username("demo")
                .email("demo@example.com")
                .displayName("Demo User")
                .addRole("USER")
                .addPermission("read:data")
                .authenticationMechanism("JWT")
                .build();
    }

    private UserPrincipal createAliceUser() {
        return DefaultUserPrincipal.builder()
                .id("user-2")
                .username("alice")
                .email("alice@example.com")
                .displayName("Alice Johnson")
                .addRole("USER")
                .addRole("MANAGER")
                .addPermission("read:data")
                .addPermission("write:data")
                .authenticationMechanism("JWT")
                .build();
    }

    private UserPrincipal createBobUser() {
        return DefaultUserPrincipal.builder()
                .id("user-3")
                .username("bob")
                .email("bob@example.com")
                .displayName("Bob Smith")
                .addRole("USER")
                .addPermission("read:data")
                .authenticationMechanism("JWT")
                .build();
    }

    private UserPrincipal createAdminUser() {
        return DefaultUserPrincipal.builder()
                .id("user-4")
                .username("admin")
                .email("admin@example.com")
                .displayName("Administrator")
                .addRole("USER")
                .addRole("ADMIN")
                .addPermission("read:data")
                .addPermission("write:data")
                .addPermission("delete:data")
                .authenticationMechanism("JWT")
                .build();
    }

    /**
     * Login request DTO.
     */
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        public String username;

        @NotBlank(message = "Password is required")
        public String password;
    }
}
