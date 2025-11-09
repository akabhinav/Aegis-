package io.aegis.demo.controller;

import io.aegis.core.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * API controller with protected endpoints to test authentication.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    /**
     * Get current authenticated user information.
     * Accessible by all authenticated users.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("displayName", user.getDisplayName());
        response.put("roles", user.getRoles());
        response.put("permissions", user.getPermissions());
        response.put("authenticationMechanism", user.getAuthenticationMechanism());
        response.put("enabled", user.isEnabled());
        response.put("locked", user.isLocked());

        return ResponseEntity.ok(response);
    }

    /**
     * Public endpoint - no authentication required.
     */
    @GetMapping("/public/hello")
    public ResponseEntity<Map<String, String>> publicHello() {
        return ResponseEntity.ok(Map.of(
                "message", "Hello from public endpoint!",
                "timestamp", Instant.now().toString(),
                "authentication", "Not required"
        ));
    }

    /**
     * Protected endpoint - requires authentication.
     */
    @GetMapping("/protected/data")
    public ResponseEntity<Map<String, Object>> getProtectedData(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, " + user.getDisplayName() + "!");
        response.put("timestamp", Instant.now().toString());
        response.put("data", Map.of(
                "item1", "Confidential Data 1",
                "item2", "Confidential Data 2",
                "item3", "Confidential Data 3"
        ));
        response.put("accessedBy", user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * User-only endpoint - requires USER role.
     */
    @GetMapping("/user/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> userDashboard(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "message", "Welcome to user dashboard",
                "user", user.getUsername(),
                "stats", Map.of(
                        "logins", 42,
                        "lastLogin", Instant.now().minusSeconds(3600).toString(),
                        "activity", "High"
                )
        ));
    }

    /**
     * Admin-only endpoint - requires ADMIN role.
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminUsers(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "message", "Admin access granted",
                "admin", user.getUsername(),
                "users", java.util.List.of(
                        Map.of("id", 1, "username", "demo", "status", "active"),
                        Map.of("id", 2, "username", "alice", "status", "active"),
                        Map.of("id", 3, "username", "bob", "status", "active"),
                        Map.of("id", 4, "username", "admin", "status", "active")
                ),
                "totalUsers", 4
        ));
    }

    /**
     * Manager-only endpoint - requires MANAGER role.
     */
    @GetMapping("/manager/reports")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> managerReports(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "message", "Manager reports",
                "manager", user.getUsername(),
                "reports", java.util.List.of(
                        Map.of("id", 1, "title", "Q1 Report", "status", "completed"),
                        Map.of("id", 2, "title", "Q2 Report", "status", "in-progress")
                ),
                "totalReports", 2
        ));
    }

    /**
     * Test endpoint to verify different authentication methods.
     */
    @GetMapping("/test/auth-method")
    public ResponseEntity<Map<String, Object>> testAuthMethod(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", user.getUsername(),
                "authenticationMechanism", user.getAuthenticationMechanism(),
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Endpoint to test write permissions.
     */
    @PostMapping("/data")
    @PreAuthorize("hasPermission(#request, 'write:data')")
    public ResponseEntity<Map<String, Object>> createData(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "message", "Data created successfully",
                "createdBy", user.getUsername(),
                "data", request,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Endpoint to test delete permissions.
     */
    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasPermission(#id, 'delete:data')")
    public ResponseEntity<Map<String, Object>> deleteData(
            @PathVariable String id,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "message", "Data deleted successfully",
                "deletedBy", user.getUsername(),
                "id", id,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Health check endpoint (public).
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "application", "Aegis Demo App"
        ));
    }
}
