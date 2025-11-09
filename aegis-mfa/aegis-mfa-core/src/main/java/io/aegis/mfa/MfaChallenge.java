package io.aegis.mfa;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an MFA challenge issued to a user.
 *
 * @param userId the user ID
 * @param method the MFA method
 * @param secret the secret/challenge data
 * @param expiresAt when the challenge expires
 * @param metadata additional challenge metadata
 * @since 1.0.0
 */
public record MfaChallenge(
        String userId,
        MfaMethod method,
        String secret,
        Instant expiresAt,
        Map<String, Object> metadata
) {

    public MfaChallenge(String userId, MfaMethod method, String secret) {
        this(userId, method, secret, Instant.now().plusSeconds(300), new HashMap<>());
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String getSecret() {
        return secret;
    }

    public String getUserId() {
        return userId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private MfaMethod method;
        private String secret;
        private Instant expiresAt = Instant.now().plusSeconds(300);
        private Map<String, Object> metadata = new HashMap<>();

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder method(MfaMethod method) {
            this.method = method;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public MfaChallenge build() {
            return new MfaChallenge(userId, method, secret, expiresAt, metadata);
        }
    }
}
