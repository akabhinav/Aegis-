package io.aegis.mfa.push;

import java.time.Duration;

/**
 * Configuration for push notification-based MFA.
 * Uses builder pattern for flexible configuration.
 *
 * @param approvalTimeout how long to wait for user to approve/deny (default: 2 minutes)
 * @param maxPendingChallenges maximum pending challenges per user (default: 3)
 * @param rateLimitPerMinute maximum push notifications per minute per device (default: 5)
 * @param title notification title template
 * @param body notification body template
 * @param requireBiometric whether to require biometric authentication on device (default: false)
 * @param allowAutoApprove whether to allow auto-approval based on trusted devices (default: false)
 *
 * @since 1.0.0
 */
public record PushConfiguration(
        Duration approvalTimeout,
        int maxPendingChallenges,
        int rateLimitPerMinute,
        String title,
        String body,
        boolean requireBiometric,
        boolean allowAutoApprove
) {

    /**
     * Default configuration with sensible defaults.
     */
    public static final PushConfiguration DEFAULT = new PushConfiguration(
            Duration.ofMinutes(2),
            3,
            5,
            "Login Request",
            "Approve login attempt from {location}?",
            false,
            false
    );

    /**
     * Creates a configuration with validation.
     */
    public PushConfiguration {
        if (approvalTimeout == null || approvalTimeout.isNegative() || approvalTimeout.isZero()) {
            throw new IllegalArgumentException("Approval timeout must be positive");
        }
        if (maxPendingChallenges <= 0) {
            throw new IllegalArgumentException("Max pending challenges must be positive");
        }
        if (rateLimitPerMinute <= 0) {
            throw new IllegalArgumentException("Rate limit must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Body cannot be null or blank");
        }
    }

    /**
     * Creates a builder for this configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Formats the notification body with context information.
     *
     * @param location location information (e.g., "San Francisco, CA")
     * @param ipAddress IP address of the login attempt
     * @param deviceInfo device information
     * @return formatted body message
     */
    public String formatBody(String location, String ipAddress, String deviceInfo) {
        return body
                .replace("{location}", location != null ? location : "Unknown location")
                .replace("{ip}", ipAddress != null ? ipAddress : "Unknown IP")
                .replace("{device}", deviceInfo != null ? deviceInfo : "Unknown device");
    }

    /**
     * Builder for PushConfiguration.
     */
    public static class Builder {
        private Duration approvalTimeout = DEFAULT.approvalTimeout;
        private int maxPendingChallenges = DEFAULT.maxPendingChallenges;
        private int rateLimitPerMinute = DEFAULT.rateLimitPerMinute;
        private String title = DEFAULT.title;
        private String body = DEFAULT.body;
        private boolean requireBiometric = DEFAULT.requireBiometric;
        private boolean allowAutoApprove = DEFAULT.allowAutoApprove;

        /**
         * Sets the approval timeout.
         *
         * @param approvalTimeout how long to wait for approval
         * @return this builder
         */
        public Builder approvalTimeout(Duration approvalTimeout) {
            this.approvalTimeout = approvalTimeout;
            return this;
        }

        /**
         * Sets the maximum pending challenges per user.
         *
         * @param maxPendingChallenges max pending challenges
         * @return this builder
         */
        public Builder maxPendingChallenges(int maxPendingChallenges) {
            this.maxPendingChallenges = maxPendingChallenges;
            return this;
        }

        /**
         * Sets the rate limit per minute.
         *
         * @param rateLimitPerMinute rate limit
         * @return this builder
         */
        public Builder rateLimitPerMinute(int rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
            return this;
        }

        /**
         * Sets the notification title.
         *
         * @param title notification title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the notification body template.
         *
         * @param body notification body template
         * @return this builder
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets whether to require biometric authentication.
         *
         * @param requireBiometric whether to require biometric
         * @return this builder
         */
        public Builder requireBiometric(boolean requireBiometric) {
            this.requireBiometric = requireBiometric;
            return this;
        }

        /**
         * Sets whether to allow auto-approval.
         *
         * @param allowAutoApprove whether to allow auto-approval
         * @return this builder
         */
        public Builder allowAutoApprove(boolean allowAutoApprove) {
            this.allowAutoApprove = allowAutoApprove;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the configuration
         */
        public PushConfiguration build() {
            return new PushConfiguration(
                    approvalTimeout,
                    maxPendingChallenges,
                    rateLimitPerMinute,
                    title,
                    body,
                    requireBiometric,
                    allowAutoApprove
            );
        }
    }
}
