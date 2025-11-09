package io.aegis.basic;

import io.aegis.basic.credentials.CredentialsStore;
import io.aegis.core.AuthenticationProvider;
import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.context.BasicAuthContext;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authentication provider for HTTP Basic Authentication.
 * <p>
 * Validates username and password credentials against a configured
 * credentials store.
 *
 * @since 1.0.0
 */
public class BasicAuthenticationProvider implements AuthenticationProvider<BasicAuthContext> {

    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationProvider.class);

    private final CredentialsStore credentialsStore;

    /**
     * Creates a basic authentication provider.
     *
     * @param credentialsStore the credentials store
     */
    public BasicAuthenticationProvider(CredentialsStore credentialsStore) {
        this.credentialsStore = credentialsStore;
    }

    @Override
    public AuthenticationResult authenticate(BasicAuthContext context) {
        try {
            String username = context.username();
            String password = context.password();

            if (username == null || username.isEmpty()) {
                return AuthenticationResult.failure("Username is required");
            }

            if (password == null || password.isEmpty()) {
                return AuthenticationResult.failure("Password is required");
            }

            // Validate credentials
            Optional<UserPrincipal> principal = credentialsStore.validateCredentials(username, password);

            if (principal.isEmpty()) {
                logger.warn("Invalid credentials for username: {}", username);
                return AuthenticationResult.failure("Invalid username or password");
            }

            UserPrincipal user = principal.get();

            // Check if user is enabled
            if (!user.isEnabled()) {
                logger.warn("Account disabled for username: {}", username);
                return AuthenticationResult.failure("Account is disabled");
            }

            // Check if account is locked
            if (user.isLocked()) {
                logger.warn("Account locked for username: {}", username);
                return AuthenticationResult.failure("Account is locked");
            }

            logger.info("Basic authentication successful for user: {}", username);
            return AuthenticationResult.success(user);

        } catch (Exception e) {
            logger.error("Basic authentication error", e);
            return AuthenticationResult.failure("Authentication error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(AuthenticationContext context) {
        return context instanceof BasicAuthContext;
    }

    @Override
    public String getName() {
        return "BasicAuthenticationProvider";
    }

    @Override
    public int getPriority() {
        return 70;
    }
}
