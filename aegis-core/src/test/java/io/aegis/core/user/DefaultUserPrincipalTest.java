package io.aegis.core.user;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultUserPrincipal.
 */
class DefaultUserPrincipalTest {

    @Test
    void shouldCreateMinimalPrincipal() {
        UserPrincipal principal = new DefaultUserPrincipal("123", "john");

        assertEquals("123", principal.getId());
        assertEquals("john", principal.getUsername());
        assertEquals("john", principal.getDisplayName());
        assertNull(principal.getEmail());
        assertTrue(principal.getRoles().isEmpty());
        assertTrue(principal.getPermissions().isEmpty());
        assertTrue(principal.isEnabled());
        assertFalse(principal.isLocked());
    }

    @Test
    void shouldCreatePrincipalWithBuilder() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .email("john@example.com")
                .displayName("John Doe")
                .addRole("USER")
                .addRole("ADMIN")
                .addPermission("read:users")
                .addPermission("write:users")
                .addAttribute("department", "Engineering")
                .enabled(true)
                .locked(false)
                .authenticationMechanism("JWT")
                .build();

        assertEquals("123", principal.getId());
        assertEquals("john", principal.getUsername());
        assertEquals("john@example.com", principal.getEmail());
        assertEquals("John Doe", principal.getDisplayName());
        assertEquals(Set.of("USER", "ADMIN"), principal.getRoles());
        assertEquals(Set.of("read:users", "write:users"), principal.getPermissions());
        assertEquals("Engineering", principal.getAttribute("department"));
        assertTrue(principal.isEnabled());
        assertFalse(principal.isLocked());
        assertEquals("JWT", principal.getAuthenticationMechanism());
    }

    @Test
    void shouldCheckRoles() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .addRole("USER")
                .addRole("ADMIN")
                .build();

        assertTrue(principal.hasRole("USER"));
        assertTrue(principal.hasRole("ADMIN"));
        assertFalse(principal.hasRole("SUPER_ADMIN"));
    }

    @Test
    void shouldCheckAnyRole() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .addRole("USER")
                .build();

        assertTrue(principal.hasAnyRole("USER", "ADMIN"));
        assertTrue(principal.hasAnyRole("ADMIN", "USER"));
        assertFalse(principal.hasAnyRole("ADMIN", "SUPER_ADMIN"));
    }

    @Test
    void shouldCheckPermissions() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .addPermission("read:users")
                .addPermission("write:users")
                .build();

        assertTrue(principal.hasPermission("read:users"));
        assertTrue(principal.hasPermission("write:users"));
        assertFalse(principal.hasPermission("delete:users"));
    }

    @Test
    void shouldThrowExceptionWhenMissingRequiredFields() {
        assertThrows(IllegalStateException.class, () ->
                DefaultUserPrincipal.builder().build()
        );

        assertThrows(IllegalStateException.class, () ->
                DefaultUserPrincipal.builder().id("123").build()
        );

        assertThrows(IllegalStateException.class, () ->
                DefaultUserPrincipal.builder().username("john").build()
        );
    }

    @Test
    void shouldGetAttributes() {
        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .addAttribute("key1", "value1")
                .addAttribute("key2", 42)
                .build();

        assertEquals("value1", principal.getAttribute("key1"));
        assertEquals(42, principal.<Integer>getAttribute("key2"));
        assertNull(principal.getAttribute("nonexistent"));
    }

    @Test
    void shouldSetAttributesMap() {
        Map<String, Object> attrs = Map.of(
                "key1", "value1",
                "key2", "value2"
        );

        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .attributes(attrs)
                .build();

        assertEquals("value1", principal.getAttribute("key1"));
        assertEquals("value2", principal.getAttribute("key2"));
    }

    @Test
    void shouldSetRolesSet() {
        Set<String> roles = Set.of("USER", "ADMIN");

        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .roles(roles)
                .build();

        assertEquals(roles, principal.getRoles());
    }

    @Test
    void shouldSetPermissionsSet() {
        Set<String> permissions = Set.of("read:users", "write:users");

        UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("123")
                .username("john")
                .permissions(permissions)
                .build();

        assertEquals(permissions, principal.getPermissions());
    }
}
