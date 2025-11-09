package io.aegis.jwt.config;

import com.nimbusds.jose.JWSAlgorithm;

import java.security.PublicKey;
import java.time.Duration;

/**
 * Configuration for JWT authentication.
 * <p>
 * Supports both symmetric (HMAC) and asymmetric (RSA) signature algorithms.
 *
 * @since 1.0.0
 */
public class JwtConfiguration {

    private JWSAlgorithm signatureAlgorithm = JWSAlgorithm.RS256;
    private String secretKey;
    private PublicKey publicKey;
    private String issuer;
    private String audience;
    private Duration tokenExpiration = Duration.ofHours(1);
    private Duration refreshTokenExpiration = Duration.ofDays(7);
    private boolean validateExpiration = true;
    private boolean validateIssuer = true;
    private boolean validateAudience = true;
    private Duration clockSkew = Duration.ofMinutes(5);

    /**
     * Creates a default JWT configuration.
     */
    public JwtConfiguration() {
    }

    public JWSAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(JWSAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getTokenExpiration() {
        return tokenExpiration;
    }

    public void setTokenExpiration(Duration tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }

    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(Duration refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public boolean isValidateExpiration() {
        return validateExpiration;
    }

    public void setValidateExpiration(boolean validateExpiration) {
        this.validateExpiration = validateExpiration;
    }

    public boolean isValidateIssuer() {
        return validateIssuer;
    }

    public void setValidateIssuer(boolean validateIssuer) {
        this.validateIssuer = validateIssuer;
    }

    public boolean isValidateAudience() {
        return validateAudience;
    }

    public void setValidateAudience(boolean validateAudience) {
        this.validateAudience = validateAudience;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    /**
     * Creates a builder for JWT configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for JWT configuration.
     */
    public static class Builder {
        private final JwtConfiguration config = new JwtConfiguration();

        private Builder() {
        }

        public Builder signatureAlgorithm(JWSAlgorithm algorithm) {
            config.signatureAlgorithm = algorithm;
            return this;
        }

        public Builder secretKey(String secretKey) {
            config.secretKey = secretKey;
            return this;
        }

        public Builder publicKey(PublicKey publicKey) {
            config.publicKey = publicKey;
            return this;
        }

        public Builder issuer(String issuer) {
            config.issuer = issuer;
            return this;
        }

        public Builder audience(String audience) {
            config.audience = audience;
            return this;
        }

        public Builder tokenExpiration(Duration expiration) {
            config.tokenExpiration = expiration;
            return this;
        }

        public Builder refreshTokenExpiration(Duration expiration) {
            config.refreshTokenExpiration = expiration;
            return this;
        }

        public Builder validateExpiration(boolean validate) {
            config.validateExpiration = validate;
            return this;
        }

        public Builder validateIssuer(boolean validate) {
            config.validateIssuer = validate;
            return this;
        }

        public Builder validateAudience(boolean validate) {
            config.validateAudience = validate;
            return this;
        }

        public Builder clockSkew(Duration clockSkew) {
            config.clockSkew = clockSkew;
            return this;
        }

        public JwtConfiguration build() {
            return config;
        }
    }
}
