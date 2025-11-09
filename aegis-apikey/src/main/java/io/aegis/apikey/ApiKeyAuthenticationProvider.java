package io.aegis.apikey;

import io.aegis.apikey.config.ApiKeyConfiguration;
import io.aegis.apikey.store.ApiKeyStore;
import io.aegis.core.AuthenticationProvider;
import io.aegis.core.context.ApiKeyContext;
import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authentication provider for API Key based authentication.
 * <p>
 * Validates API keys from headers, query parameters, or cookies against
 * a configured API key store.
 *
 * @since 1.0.0
 */
public class ApiKeyAuthenticationProvider implements AuthenticationProvider<ApiKeyContext> {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationProvider.class);

    private final ApiKeyConfiguration configuration;
    private final ApiKeyStore apiKeyStore;

    /**
     * Creates an API key authentication provider.
     *
     * @param configuration the API key configuration
     * @param apiKeyStore the API key store
     */
    public ApiKeyAuthenticationProvider(ApiKeyConfiguration configuration, ApiKeyStore apiKeyStore) {
        this.configuration = configuration;
        this.apiKeyStore = apiKeyStore;
    }

    @Override
    public AuthenticationResult authenticate(ApiKeyContext context) {
        try {
            String apiKey = context.apiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                return AuthenticationResult.failure("API key is required");
            }

            // Validate API key
            Optional<UserPrincipal> principal = apiKeyStore.validateApiKey(apiKey);

            if (principal.isEmpty()) {
                logger.warn("Invalid API key provided");
                return AuthenticationResult.failure("Invalid API key");
            }

            // Check if API key is enabled
            UserPrincipal user = principal.get();
            if (!user.isEnabled()) {
                logger.warn("API key is disabled for user: {}", user.getUsername());
                return AuthenticationResult.failure("API key is disabled");
            }

            logger.info("API key authentication successful for user: {}", user.getUsername());
            return AuthenticationResult.success(user);

        } catch (Exception e) {
            logger.error("API key authentication error", e);
            return AuthenticationResult.failure("Authentication error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(AuthenticationContext context) {
        return context instanceof ApiKeyContext;
    }

    @Override
    public String getName() {
        return "ApiKeyAuthenticationProvider";
    }

    @Override
    public int getPriority() {
        return 80;
    }
}
