package io.aegis.demo.config;

import io.aegis.basic.credentials.CredentialsStore;
import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory credentials store for demo purposes.
 * In production, use a proper database with hashed passwords.
 */
@Component
public class DemoCredentialsStore implements CredentialsStore {

    private final Map<String, UserCredentials> credentials = new ConcurrentHashMap<>();

    public DemoCredentialsStore() {
        // Initialize with demo users
        // In production, passwords should be properly hashed with BCrypt
        storeCredentials("demo", "password", createDemoUser());
        storeCredentials("alice", "alice123", createAliceUser());
        storeCredentials("bob", "bob123", createBobUser());
        storeCredentials("admin", "admin123", createAdminUser());
    }

    @Override
    public Optional<UserPrincipal> validateCredentials(String username, String password) {
        UserCredentials userCreds = credentials.get(username);

        if (userCreds != null && userCreds.password.equals(password)) {
            return Optional.of(userCreds.principal);
        }

        return Optional.empty();
    }

    @Override
    public void storeCredentials(String username, String password, UserPrincipal principal) {
        credentials.put(username, new UserCredentials(password, principal));
    }

    @Override
    public boolean removeCredentials(String username) {
        return credentials.remove(username) != null;
    }

    private UserPrincipal createDemoUser() {
        return DefaultUserPrincipal.builder()
                .id("user-1")
                .username("demo")
                .email("demo@example.com")
                .displayName("Demo User")
                .addRole("USER")
                .addPermission("read:data")
                .authenticationMechanism("BASIC")
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
                .authenticationMechanism("BASIC")
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
                .authenticationMechanism("BASIC")
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
                .authenticationMechanism("BASIC")
                .build();
    }

    private static class UserCredentials {
        final String password;
        final UserPrincipal principal;

        UserCredentials(String password, UserPrincipal principal) {
            this.password = password;
            this.principal = principal;
        }
    }
}
