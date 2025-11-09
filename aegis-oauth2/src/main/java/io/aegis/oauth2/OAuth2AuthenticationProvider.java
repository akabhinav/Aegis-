package io.aegis.oauth2;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import io.aegis.core.AuthenticationProvider;
import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.context.OAuth2Context;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import io.aegis.oauth2.config.OAuth2Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authentication provider for OAuth 2.0 and OpenID Connect.
 * <p>
 * Validates OAuth 2.0 access tokens and optionally fetches user information
 * from the UserInfo endpoint.
 *
 * @since 1.0.0
 */
public class OAuth2AuthenticationProvider implements AuthenticationProvider<OAuth2Context> {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationProvider.class);

    private final OAuth2Configuration configuration;

    public OAuth2AuthenticationProvider(OAuth2Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public AuthenticationResult authenticate(OAuth2Context context) {
        try {
            String accessTokenValue = context.accessToken();
            if (accessTokenValue == null || accessTokenValue.isEmpty()) {
                return AuthenticationResult.failure("Access token is required");
            }

            AccessToken accessToken = new BearerAccessToken(accessTokenValue);

            // Validate token with introspection endpoint if configured
            if (configuration.getIntrospectionEndpoint() != null) {
                boolean valid = validateTokenViaIntrospection(accessToken);
                if (!valid) {
                    logger.warn("Token validation failed via introspection");
                    return AuthenticationResult.failure("Invalid access token");
                }
            }

            // Fetch user information
            UserPrincipal principal = fetchUserInfo(accessToken, context);

            logger.info("OAuth2 authentication successful for user: {}", principal.getUsername());
            return AuthenticationResult.success(principal);

        } catch (Exception e) {
            logger.error("OAuth2 authentication error", e);
            return AuthenticationResult.failure("OAuth2 authentication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(AuthenticationContext context) {
        return context instanceof OAuth2Context;
    }

    @Override
    public String getName() {
        return "OAuth2AuthenticationProvider";
    }

    @Override
    public int getPriority() {
        return 90;
    }

    /**
     * Fetches user information from the UserInfo endpoint.
     *
     * @param accessToken the access token
     * @param context the OAuth2 context
     * @return the user principal
     * @throws Exception if user info fetch fails
     */
    private UserPrincipal fetchUserInfo(AccessToken accessToken, OAuth2Context context) throws Exception {
        if (configuration.getUserInfoEndpoint() == null) {
            // No UserInfo endpoint, create minimal principal from token
            return DefaultUserPrincipal.builder()
                    .id("unknown")
                    .username("oauth2-user")
                    .authenticationMechanism("OAuth2")
                    .build();
        }

        UserInfoRequest userInfoRequest = new UserInfoRequest(
                configuration.getUserInfoEndpoint(),
                accessToken
        );

        HTTPRequest httpRequest = userInfoRequest.toHTTPRequest();
        HTTPResponse httpResponse = httpRequest.send();

        UserInfoResponse userInfoResponse = UserInfoResponse.parse(httpResponse);

        if (!userInfoResponse.indicatesSuccess()) {
            throw new Exception("Failed to fetch user info: " + userInfoResponse.toErrorResponse().getErrorObject());
        }

        UserInfo userInfo = userInfoResponse.toSuccessResponse().getUserInfo();

        // Extract user information
        String userId = userInfo.getSubject().getValue();
        String username = userInfo.getPreferredUsername();
        String email = userInfo.getEmailAddress();
        String name = userInfo.getName();

        Set<String> roles = extractRoles(context);

        return DefaultUserPrincipal.builder()
                .id(userId)
                .username(username != null ? username : email != null ? email : userId)
                .email(email)
                .displayName(name)
                .roles(roles)
                .authenticationMechanism("OAuth2")
                .addAttribute("userInfo", userInfo.toJSONObject())
                .build();
    }

    /**
     * Validates the token via the introspection endpoint.
     *
     * @param accessToken the access token
     * @return true if the token is valid
     */
    private boolean validateTokenViaIntrospection(AccessToken accessToken) {
        try {
            TokenIntrospectionRequest request = new TokenIntrospectionRequest(
                    configuration.getIntrospectionEndpoint(),
                    new BearerAccessToken(configuration.getClientId()),
                    accessToken
            );

            HTTPResponse response = request.toHTTPRequest().send();
            TokenIntrospectionResponse introspectionResponse = TokenIntrospectionResponse.parse(response);

            if (!introspectionResponse.indicatesSuccess()) {
                return false;
            }

            return introspectionResponse.toSuccessResponse().isActive();

        } catch (Exception e) {
            logger.error("Token introspection failed", e);
            return false;
        }
    }

    /**
     * Extracts roles from the OAuth2 context.
     *
     * @param context the OAuth2 context
     * @return the set of roles
     */
    private Set<String> extractRoles(OAuth2Context context) {
        Set<String> roles = new HashSet<>();
        Object rolesAttr = context.getAttribute("roles").orElse(null);

        if (rolesAttr instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> rolesList = (List<String>) rolesAttr;
            roles.addAll(rolesList);
        }

        return roles;
    }
}
