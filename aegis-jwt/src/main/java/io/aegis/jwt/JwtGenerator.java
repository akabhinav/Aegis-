package io.aegis.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.aegis.core.AuthenticationException;
import io.aegis.core.user.UserPrincipal;
import io.aegis.jwt.config.JwtConfiguration;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for generating JWT tokens.
 * <p>
 * Supports both HMAC (symmetric) and RSA (asymmetric) signature algorithms.
 *
 * @since 1.0.0
 */
public class JwtGenerator {

    private final JwtConfiguration configuration;
    private final JWSSigner signer;

    /**
     * Creates a JWT generator with the given configuration.
     *
     * @param configuration the JWT configuration
     * @param privateKey optional private key for RSA signing (null for HMAC)
     */
    public JwtGenerator(JwtConfiguration configuration, PrivateKey privateKey) {
        this.configuration = configuration;
        this.signer = createSigner(configuration, privateKey);
    }

    /**
     * Creates a JWT generator for HMAC signing.
     *
     * @param configuration the JWT configuration
     */
    public JwtGenerator(JwtConfiguration configuration) {
        this(configuration, null);
    }

    /**
     * Generates a JWT token for the given user principal.
     *
     * @param principal the user principal
     * @return the JWT token string
     * @throws AuthenticationException if token generation fails
     */
    public String generateToken(UserPrincipal principal) {
        return generateToken(principal, Map.of());
    }

    /**
     * Generates a JWT token for the given user principal with additional claims.
     *
     * @param principal the user principal
     * @param additionalClaims additional claims to include in the token
     * @return the JWT token string
     * @throws AuthenticationException if token generation fails
     */
    public String generateToken(UserPrincipal principal, Map<String, Object> additionalClaims) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plus(configuration.getTokenExpiration());

            // Build claims
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(principal.getId())
                    .issuer(configuration.getIssuer())
                    .audience(configuration.getAudience())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("username", principal.getUsername())
                    .claim("email", principal.getEmail())
                    .claim("name", principal.getDisplayName())
                    .claim("roles", principal.getRoles());

            // Add additional claims
            additionalClaims.forEach(claimsBuilder::claim);

            JWTClaimsSet claims = claimsBuilder.build();

            // Create JWT
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(configuration.getSignatureAlgorithm()),
                    claims
            );

            // Sign the JWT
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new AuthenticationException("Failed to generate JWT token", e);
        }
    }

    /**
     * Generates a refresh token for the given user principal.
     *
     * @param principal the user principal
     * @return the refresh token string
     * @throws AuthenticationException if token generation fails
     */
    public String generateRefreshToken(UserPrincipal principal) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plus(configuration.getRefreshTokenExpiration());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(principal.getId())
                    .issuer(configuration.getIssuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("type", "refresh")
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(configuration.getSignatureAlgorithm()),
                    claims
            );

            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new AuthenticationException("Failed to generate refresh token", e);
        }
    }

    /**
     * Creates a JWS signer based on the configuration.
     *
     * @param config the JWT configuration
     * @param privateKey optional private key for RSA signing
     * @return the JWS signer
     * @throws AuthenticationException if the signer cannot be created
     */
    private JWSSigner createSigner(JwtConfiguration config, PrivateKey privateKey) {
        try {
            return switch (config.getSignatureAlgorithm().getName()) {
                case "HS256", "HS384", "HS512" -> {
                    if (config.getSecretKey() == null) {
                        throw new AuthenticationException("Secret key is required for HMAC algorithms");
                    }
                    yield new MACSigner(config.getSecretKey());
                }
                case "RS256", "RS384", "RS512" -> {
                    if (privateKey == null) {
                        throw new AuthenticationException("Private key is required for RSA algorithms");
                    }
                    yield new RSASSASigner(privateKey);
                }
                default -> throw new AuthenticationException(
                        "Unsupported signature algorithm: " + config.getSignatureAlgorithm());
            };
        } catch (JOSEException e) {
            throw new AuthenticationException("Failed to create JWT signer", e);
        }
    }
}
