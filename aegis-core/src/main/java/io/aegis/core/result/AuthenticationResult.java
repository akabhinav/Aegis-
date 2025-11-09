package io.aegis.core.result;

import io.aegis.core.user.UserPrincipal;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the result of an authentication attempt.
 * This is a sealed interface ensuring type-safe exhaustive pattern matching.
 *
 * @since 1.0.0
 */
public sealed interface AuthenticationResult permits
        AuthenticationResult.Success,
        AuthenticationResult.Failure {

    /**
     * Checks if the authentication was successful.
     *
     * @return true if authentication succeeded
     */
    boolean isSuccess();

    /**
     * Gets the authenticated user principal if authentication was successful.
     *
     * @return Optional containing the user principal if successful
     */
    Optional<UserPrincipal> getPrincipal();

    /**
     * Gets the timestamp when this result was created.
     *
     * @return the result timestamp
     */
    Instant getTimestamp();

    /**
     * Represents a successful authentication result.
     *
     * @param principal the authenticated user principal
     * @param timestamp the authentication timestamp
     * @param metadata additional metadata about the authentication
     */
    record Success(
            UserPrincipal principal,
            Instant timestamp,
            Map<String, Object> metadata
    ) implements AuthenticationResult {

        public Success(UserPrincipal principal) {
            this(principal, Instant.now(), Map.of());
        }

        public Success(UserPrincipal principal, Map<String, Object> metadata) {
            this(principal, Instant.now(), metadata);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<UserPrincipal> getPrincipal() {
            return Optional.of(principal);
        }

        @Override
        public Instant getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Represents a failed authentication result.
     *
     * @param reason the reason for failure
     * @param timestamp the failure timestamp
     * @param error optional exception that caused the failure
     * @param metadata additional metadata about the failure
     */
    record Failure(
            String reason,
            Instant timestamp,
            Throwable error,
            Map<String, Object> metadata
    ) implements AuthenticationResult {

        public Failure(String reason) {
            this(reason, Instant.now(), null, Map.of());
        }

        public Failure(String reason, Throwable error) {
            this(reason, Instant.now(), error, Map.of());
        }

        public Failure(String reason, Map<String, Object> metadata) {
            this(reason, Instant.now(), null, metadata);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<UserPrincipal> getPrincipal() {
            return Optional.empty();
        }

        @Override
        public Instant getTimestamp() {
            return timestamp;
        }

        /**
         * Gets the error that caused the failure, if any.
         *
         * @return Optional containing the error
         */
        public Optional<Throwable> getError() {
            return Optional.ofNullable(error);
        }
    }

    /**
     * Creates a successful authentication result.
     *
     * @param principal the authenticated user principal
     * @return a success result
     */
    static Success success(UserPrincipal principal) {
        return new Success(principal);
    }

    /**
     * Creates a successful authentication result with metadata.
     *
     * @param principal the authenticated user principal
     * @param metadata additional metadata
     * @return a success result
     */
    static Success success(UserPrincipal principal, Map<String, Object> metadata) {
        return new Success(principal, metadata);
    }

    /**
     * Creates a failed authentication result.
     *
     * @param reason the failure reason
     * @return a failure result
     */
    static Failure failure(String reason) {
        return new Failure(reason);
    }

    /**
     * Creates a failed authentication result with an error.
     *
     * @param reason the failure reason
     * @param error the error that caused the failure
     * @return a failure result
     */
    static Failure failure(String reason, Throwable error) {
        return new Failure(reason, error);
    }

    /**
     * Creates a failed authentication result with metadata.
     *
     * @param reason the failure reason
     * @param metadata additional metadata
     * @return a failure result
     */
    static Failure failure(String reason, Map<String, Object> metadata) {
        return new Failure(reason, metadata);
    }
}
