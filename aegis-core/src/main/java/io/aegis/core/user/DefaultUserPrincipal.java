package io.aegis.core.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link UserPrincipal}.
 * Immutable record-based implementation with builder support.
 *
 * @param id the user ID
 * @param username the username
 * @param email the email address
 * @param displayName the display name
 * @param roles the user roles
 * @param permissions the user permissions
 * @param attributes additional attributes
 * @param enabled whether the user is enabled
 * @param locked whether the account is locked
 * @param authenticationMechanism the authentication mechanism used
 * @since 1.0.0
 */
public record DefaultUserPrincipal(
        String id,
        String username,
        String email,
        String displayName,
        Set<String> roles,
        Set<String> permissions,
        Map<String, Object> attributes,
        boolean enabled,
        boolean locked,
        String authenticationMechanism
) implements UserPrincipal {

    /**
     * Creates a minimal user principal with just an ID and username.
     *
     * @param id the user ID
     * @param username the username
     */
    public DefaultUserPrincipal(String id, String username) {
        this(id, username, null, username, new HashSet<>(), new HashSet<>(),
                new HashMap<>(), true, false, "UNKNOWN");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getDisplayName() {
        return displayName != null ? displayName : username;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public String getAuthenticationMechanism() {
        return authenticationMechanism;
    }

    /**
     * Creates a new builder for constructing UserPrincipal instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating DefaultUserPrincipal instances.
     */
    public static class Builder {
        private String id;
        private String username;
        private String email;
        private String displayName;
        private Set<String> roles = new HashSet<>();
        private Set<String> permissions = new HashSet<>();
        private Map<String, Object> attributes = new HashMap<>();
        private boolean enabled = true;
        private boolean locked = false;
        private String authenticationMechanism = "UNKNOWN";

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder roles(Set<String> roles) {
            this.roles = new HashSet<>(roles);
            return this;
        }

        public Builder addRole(String role) {
            this.roles.add(role);
            return this;
        }

        public Builder permissions(Set<String> permissions) {
            this.permissions = new HashSet<>(permissions);
            return this;
        }

        public Builder addPermission(String permission) {
            this.permissions.add(permission);
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = new HashMap<>(attributes);
            return this;
        }

        public Builder addAttribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public Builder authenticationMechanism(String mechanism) {
            this.authenticationMechanism = mechanism;
            return this;
        }

        public DefaultUserPrincipal build() {
            if (id == null || username == null) {
                throw new IllegalStateException("ID and username are required");
            }
            return new DefaultUserPrincipal(
                    id, username, email, displayName,
                    Set.copyOf(roles), Set.copyOf(permissions),
                    Map.copyOf(attributes), enabled, locked, authenticationMechanism
            );
        }
    }
}
