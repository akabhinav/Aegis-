package io.aegis.core.user;

import java.util.Map;
import java.util.Set;

/**
 * Represents an authenticated user principal.
 * Contains user identity and authorization information.
 *
 * @since 1.0.0
 */
public interface UserPrincipal {

    /**
     * Gets the unique identifier of the user.
     *
     * @return the user ID
     */
    String getId();

    /**
     * Gets the username or email of the user.
     *
     * @return the username
     */
    String getUsername();

    /**
     * Gets the display name of the user.
     *
     * @return the display name, or username if not available
     */
    default String getDisplayName() {
        return getUsername();
    }

    /**
     * Gets the email address of the user, if available.
     *
     * @return the email or null
     */
    String getEmail();

    /**
     * Gets the roles assigned to this user.
     *
     * @return a set of role names
     */
    Set<String> getRoles();

    /**
     * Gets the permissions granted to this user.
     *
     * @return a set of permission strings
     */
    Set<String> getPermissions();

    /**
     * Checks if the user has a specific role.
     *
     * @param role the role to check
     * @return true if the user has the role
     */
    default boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * Checks if the user has any of the specified roles.
     *
     * @param roles the roles to check
     * @return true if the user has at least one of the roles
     */
    default boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the user has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    default boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }

    /**
     * Gets additional attributes associated with this principal.
     *
     * @return a map of additional attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Gets an attribute value.
     *
     * @param key the attribute key
     * @param <T> the expected type
     * @return the attribute value or null
     */
    @SuppressWarnings("unchecked")
    default <T> T getAttribute(String key) {
        return (T) getAttributes().get(key);
    }

    /**
     * Checks if the user is enabled/active.
     *
     * @return true if the user is enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Checks if the user account is locked.
     *
     * @return true if the account is locked
     */
    default boolean isLocked() {
        return false;
    }

    /**
     * Gets the authentication mechanism used to authenticate this user.
     *
     * @return the authentication mechanism (e.g., "JWT", "OAuth2", "API_KEY")
     */
    String getAuthenticationMechanism();
}
