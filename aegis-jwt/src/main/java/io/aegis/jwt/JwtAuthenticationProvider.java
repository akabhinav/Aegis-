package io.aegis.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.aegis.core.AuthenticationException;
import io.aegis.core.AuthenticationProvider;
import io.aegis.core.context.AuthenticationContext;
import io.aegis.core.context.JwtContext;
import io.aegis.core.exception.InvalidTokenException;
import io.aegis.core.exception.TokenExpiredException;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import io.aegis.jwt.config.JwtConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authentication provider for JWT (JSON Web Token) based authentication.
 * <p>
 * Supports both HMAC (symmetric) and RSA (asymmetric) signature verification.
 * Validates token expiration, issuer, and audience claims.
 *
 * @since 1.0.0
 */
public class JwtAuthenticationProvider implements AuthenticationProvider<JwtContext> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationProvider.class);

    private final JwtConfiguration configuration;
    private final JWSVerifier verifier;

    /**
     * Creates a JWT authentication provider with the given configuration.
     *
     * @param configuration the JWT configuration
     * @throws AuthenticationException if the configuration is invalid
     */
    public JwtAuthenticationProvider(JwtConfiguration configuration) {
        this.configuration = configuration;
        this.verifier = createVerifier(configuration);
    }

    @Override
    public AuthenticationResult authenticate(JwtContext context) {
        try {
            String token = context.token();
            if (token == null || token.isEmpty()) {
                return AuthenticationResult.failure("JWT token is required");
            }

            // Parse the JWT
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify signature
            if (!signedJWT.verify(verifier)) {
                logger.warn("JWT signature verification failed");
                return AuthenticationResult.failure("Invalid JWT signature");
            }

            // Extract claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Validate expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                logger.warn("JWT token has expired");
                return AuthenticationResult.failure("Token has expired");
            }

            // Validate not before
            Date notBefore = claims.getNotBeforeTime();
            if (notBefore != null && notBefore.after(new Date())) {
                logger.warn("JWT token not yet valid");
                return AuthenticationResult.failure("Token not yet valid");
            }

            // Validate issuer
            if (configuration.getIssuer() != null) {
                String issuer = claims.getIssuer();
                if (!configuration.getIssuer().equals(issuer)) {
                    logger.warn("JWT issuer mismatch. Expected: {}, Got: {}",
                            configuration.getIssuer(), issuer);
                    return AuthenticationResult.failure("Invalid token issuer");
                }
            }

            // Validate audience
            if (configuration.getAudience() != null) {
                List<String> audiences = claims.getAudience();
                if (audiences == null || !audiences.contains(configuration.getAudience())) {
                    logger.warn("JWT audience validation failed");
                    return AuthenticationResult.failure("Invalid token audience");
                }
            }

            // Extract user information
            UserPrincipal principal = extractPrincipal(claims);

            logger.debug("JWT authentication successful for user: {}", principal.getUsername());
            return AuthenticationResult.success(principal);

        } catch (ParseException e) {
            logger.error("Failed to parse JWT token", e);
            return AuthenticationResult.failure("Invalid JWT token format", e);
        } catch (JOSEException e) {
            logger.error("JWT verification error", e);
            return AuthenticationResult.failure("JWT verification failed", e);
        } catch (Exception e) {
            logger.error("Unexpected error during JWT authentication", e);
            return AuthenticationResult.failure("Authentication error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(AuthenticationContext context) {
        return context instanceof JwtContext;
    }

    @Override
    public String getName() {
        return "JwtAuthenticationProvider";
    }

    @Override
    public int getPriority() {
        return 100; // High priority for JWT
    }

    /**
     * Extracts user principal from JWT claims.
     *
     * @param claims the JWT claims
     * @return the user principal
     */
    private UserPrincipal extractPrincipal(JWTClaimsSet claims) {
        String userId = claims.getSubject();
        String username = claims.getStringClaim("preferred_username");
        if (username == null) {
            username = claims.getStringClaim("username");
        }
        if (username == null) {
            username = claims.getStringClaim("email");
        }
        if (username == null) {
            username = userId;
        }

        String email = claims.getStringClaim("email");
        String name = claims.getStringClaim("name");

        // Extract roles
        Set<String> roles = extractRoles(claims);

        return DefaultUserPrincipal.builder()
                .id(userId)
                .username(username)
                .email(email)
                .displayName(name)
                .roles(roles)
                .authenticationMechanism("JWT")
                .addAttribute("claims", claims.getClaims())
                .build();
    }

    /**
     * Extracts roles from JWT claims.
     * Checks multiple possible claim names for roles.
     *
     * @param claims the JWT claims
     * @return the set of roles
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(JWTClaimsSet claims) {
        Set<String> roles = new HashSet<>();

        // Try different claim names
        Object rolesObj = claims.getClaim("roles");
        if (rolesObj == null) {
            rolesObj = claims.getClaim("authorities");
        }
        if (rolesObj == null) {
            rolesObj = claims.getClaim("groups");
        }

        if (rolesObj instanceof List) {
            List<String> rolesList = (List<String>) rolesObj;
            roles.addAll(rolesList);
        } else if (rolesObj instanceof String) {
            roles.add((String) rolesObj);
        }

        return roles;
    }

    /**
     * Creates a JWS verifier based on the configuration.
     *
     * @param config the JWT configuration
     * @return the JWS verifier
     * @throws AuthenticationException if the verifier cannot be created
     */
    private JWSVerifier createVerifier(JwtConfiguration config) {
        try {
            return switch (config.getSignatureAlgorithm()) {
                case HS256, HS384, HS512 -> {
                    if (config.getSecretKey() == null) {
                        throw new AuthenticationException("Secret key is required for HMAC algorithms");
                    }
                    yield new MACVerifier(config.getSecretKey());
                }
                case RS256, RS384, RS512 -> {
                    if (config.getPublicKey() == null) {
                        throw new AuthenticationException("Public key is required for RSA algorithms");
                    }
                    yield new RSASSAVerifier((RSAPublicKey) config.getPublicKey());
                }
                default -> throw new AuthenticationException(
                        "Unsupported signature algorithm: " + config.getSignatureAlgorithm());
            };
        } catch (JOSEException e) {
            throw new AuthenticationException("Failed to create JWT verifier", e);
        }
    }
}
