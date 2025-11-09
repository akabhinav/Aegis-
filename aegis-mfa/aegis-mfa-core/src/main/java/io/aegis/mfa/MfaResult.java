package io.aegis.mfa;

import java.time.Instant;
import java.util.Map;

/**
 * Result of MFA verification.
 *
 * @since 1.0.0
 */
public sealed interface MfaResult permits MfaResult.Success, MfaResult.Failure {

    boolean isSuccess();

    record Success(String userId, Instant timestamp, Map<String, Object> metadata) implements MfaResult {
        public Success(String userId) {
            this(userId, Instant.now(), Map.of());
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure(String reason, Instant timestamp) implements MfaResult {
        public Failure(String reason) {
            this(reason, Instant.now());
        }

        @Override
        public boolean isSuccess() {
            return false;
        }
    }

    static Success success(String userId) {
        return new Success(userId);
    }

    static Failure failure(String reason) {
        return new Failure(reason);
    }
}
