package io.aegis.mfa.email;

import java.time.Duration;

/**
 * Configuration for Email-based OTP authentication.
 *
 * @param codeLength the length of the OTP code (4-8 digits)
 * @param validity the validity duration of the OTP
 * @param maxAttempts the maximum verification attempts allowed
 * @param rateLimitPerMinute the maximum emails per minute per email address
 * @param subject the email subject line
 * @param fromAddress the sender email address
 * @param fromName the sender name (optional)
 * @param templateType the email template type (PLAIN_TEXT or HTML)
 * @since 1.0.0
 */
public record EmailConfiguration(
        int codeLength,
        Duration validity,
        int maxAttempts,
        int rateLimitPerMinute,
        String subject,
        String fromAddress,
        String fromName,
        TemplateType templateType
) {

    /**
     * Default email configuration.
     * - Code length: 6 digits
     * - Validity: 10 minutes
     * - Max attempts: 3
     * - Rate limit: 5 emails per minute
     * - Subject: "Your Verification Code"
     * - Template: HTML
     */
    public static final EmailConfiguration DEFAULT = new EmailConfiguration(
            6,
            Duration.ofMinutes(10),
            3,
            5,
            "Your Verification Code",
            null,
            "Aegis Security",
            TemplateType.HTML
    );

    /**
     * Validates the configuration.
     */
    public EmailConfiguration {
        if (codeLength < 4 || codeLength > 8) {
            throw new IllegalArgumentException("Code length must be between 4 and 8 digits");
        }
        if (validity == null || validity.isZero() || validity.isNegative()) {
            throw new IllegalArgumentException("Validity must be a positive duration");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts must be positive");
        }
        if (rateLimitPerMinute <= 0) {
            throw new IllegalArgumentException("Rate limit must be positive");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject cannot be null or blank");
        }
        if (templateType == null) {
            throw new IllegalArgumentException("Template type cannot be null");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int codeLength = 6;
        private Duration validity = Duration.ofMinutes(10);
        private int maxAttempts = 3;
        private int rateLimitPerMinute = 5;
        private String subject = "Your Verification Code";
        private String fromAddress;
        private String fromName = "Aegis Security";
        private TemplateType templateType = TemplateType.HTML;

        public Builder codeLength(int codeLength) {
            this.codeLength = codeLength;
            return this;
        }

        /**
         * Sets the validity duration for the OTP.
         */
        public Builder validity(Duration validity) {
            this.validity = validity;
            return this;
        }

        /**
         * Sets the maximum verification attempts.
         */
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Sets the rate limit (emails per minute per email address).
         */
        public Builder rateLimitPerMinute(int rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
            return this;
        }

        /**
         * Sets the email subject line.
         */
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Sets the sender email address.
         */
        public Builder fromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
            return this;
        }

        /**
         * Sets the sender name.
         */
        public Builder fromName(String fromName) {
            this.fromName = fromName;
            return this;
        }

        /**
         * Sets the email template type.
         */
        public Builder templateType(TemplateType templateType) {
            this.templateType = templateType;
            return this;
        }

        public EmailConfiguration build() {
            return new EmailConfiguration(
                    codeLength,
                    validity,
                    maxAttempts,
                    rateLimitPerMinute,
                    subject,
                    fromAddress,
                    fromName,
                    templateType
            );
        }
    }

    /**
     * Email template type.
     */
    public enum TemplateType {
        /**
         * Plain text email
         */
        PLAIN_TEXT,

        /**
         * HTML email
         */
        HTML
    }
}
