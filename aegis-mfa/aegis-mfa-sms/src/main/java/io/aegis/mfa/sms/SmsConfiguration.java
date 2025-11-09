package io.aegis.mfa.sms;

import java.time.Duration;

/**
 * Configuration for SMS-based OTP authentication.
 *
 * @param codeLength the length of the OTP code (4-8 digits)
 * @param validity the validity duration of the OTP
 * @param maxAttempts the maximum verification attempts allowed
 * @param rateLimitPerMinute the maximum SMS messages per minute per phone number
 * @param messageTemplate the SMS message template (must contain {code} placeholder)
 * @param fromPhoneNumber the sender phone number
 * @param senderType the SMS sender type (TWILIO, AWS_SNS, CUSTOM)
 * @since 1.0.0
 */
public record SmsConfiguration(
        int codeLength,
        Duration validity,
        int maxAttempts,
        int rateLimitPerMinute,
        String messageTemplate,
        String fromPhoneNumber,
        SenderType senderType
) {

    /**
     * Default SMS configuration.
     * - Code length: 6 digits
     * - Validity: 5 minutes
     * - Max attempts: 3
     * - Rate limit: 3 SMS per minute
     * - Template: "Your verification code is: {code}. Valid for 5 minutes."
     */
    public static final SmsConfiguration DEFAULT = new SmsConfiguration(
            6,
            Duration.ofMinutes(5),
            3,
            3,
            "Your verification code is: {code}. Valid for 5 minutes.",
            null,
            SenderType.TWILIO
    );

    /**
     * Validates the configuration.
     */
    public SmsConfiguration {
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
        if (messageTemplate == null || messageTemplate.isBlank()) {
            throw new IllegalArgumentException("Message template cannot be null or blank");
        }
        if (!messageTemplate.contains("{code}")) {
            throw new IllegalArgumentException("Message template must contain {code} placeholder");
        }
        if (senderType == null) {
            throw new IllegalArgumentException("Sender type cannot be null");
        }
    }

    /**
     * Creates a message with the OTP code.
     *
     * @param code the OTP code
     * @return the formatted SMS message
     */
    public String formatMessage(String code) {
        return messageTemplate.replace("{code}", code);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int codeLength = 6;
        private Duration validity = Duration.ofMinutes(5);
        private int maxAttempts = 3;
        private int rateLimitPerMinute = 3;
        private String messageTemplate = "Your verification code is: {code}. Valid for 5 minutes.";
        private String fromPhoneNumber;
        private SenderType senderType = SenderType.TWILIO;

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
         * Sets the rate limit (SMS messages per minute per phone number).
         */
        public Builder rateLimitPerMinute(int rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
            return this;
        }

        /**
         * Sets the SMS message template. Must contain {code} placeholder.
         */
        public Builder messageTemplate(String messageTemplate) {
            this.messageTemplate = messageTemplate;
            return this;
        }

        /**
         * Sets the sender phone number.
         */
        public Builder fromPhoneNumber(String fromPhoneNumber) {
            this.fromPhoneNumber = fromPhoneNumber;
            return this;
        }

        /**
         * Sets the SMS sender type.
         */
        public Builder senderType(SenderType senderType) {
            this.senderType = senderType;
            return this;
        }

        public SmsConfiguration build() {
            return new SmsConfiguration(
                    codeLength,
                    validity,
                    maxAttempts,
                    rateLimitPerMinute,
                    messageTemplate,
                    fromPhoneNumber,
                    senderType
            );
        }
    }

    /**
     * SMS sender type.
     */
    public enum SenderType {
        /**
         * Twilio SMS service
         */
        TWILIO,

        /**
         * AWS SNS SMS service
         */
        AWS_SNS,

        /**
         * Custom SMS sender implementation
         */
        CUSTOM
    }
}
