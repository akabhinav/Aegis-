package io.aegis.mfa.totp;

import dev.samstevens.totp.code.HashingAlgorithm;

/**
 * Configuration for TOTP authentication.
 *
 * @param issuer the issuer name (appears in authenticator app)
 * @param algorithm the hashing algorithm (SHA1, SHA256, SHA512)
 * @param digits the number of digits in the OTP code (6 or 8)
 * @param period the time period in seconds (default 30)
 * @param discrepancy the number of time windows to check (for clock skew)
 * @param secretLength the length of the secret in bytes (default 20)
 * @since 1.0.0
 */
public record TotpConfiguration(
        String issuer,
        HashingAlgorithm algorithm,
        int digits,
        int period,
        int discrepancy,
        int secretLength
) {

    /**
     * Default TOTP configuration (Google Authenticator compatible).
     * - Issuer: "Aegis"
     * - Algorithm: SHA1
     * - Digits: 6
     * - Period: 30 seconds
     * - Discrepancy: 1 (checks current + previous/next window)
     * - Secret length: 20 bytes
     */
    public static final TotpConfiguration DEFAULT = new TotpConfiguration(
            "Aegis",
            HashingAlgorithm.SHA1,
            6,
            30,
            1,
            20
    );

    /**
     * Validates the configuration.
     */
    public TotpConfiguration {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Issuer cannot be null or blank");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        if (digits != 6 && digits != 8) {
            throw new IllegalArgumentException("Digits must be 6 or 8");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        if (discrepancy < 0) {
            throw new IllegalArgumentException("Discrepancy cannot be negative");
        }
        if (secretLength < 16 || secretLength > 128) {
            throw new IllegalArgumentException("Secret length must be between 16 and 128 bytes");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String issuer = "Aegis";
        private HashingAlgorithm algorithm = HashingAlgorithm.SHA1;
        private int digits = 6;
        private int period = 30;
        private int discrepancy = 1;
        private int secretLength = 20;

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder algorithm(HashingAlgorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Sets the number of digits (6 or 8).
         */
        public Builder digits(int digits) {
            this.digits = digits;
            return this;
        }

        /**
         * Sets the time period in seconds (typically 30).
         */
        public Builder period(int period) {
            this.period = period;
            return this;
        }

        /**
         * Sets the discrepancy (number of time windows to check for clock skew).
         * A value of 1 checks current + previous/next windows.
         */
        public Builder discrepancy(int discrepancy) {
            this.discrepancy = discrepancy;
            return this;
        }

        /**
         * Sets the secret length in bytes (default 20).
         */
        public Builder secretLength(int secretLength) {
            this.secretLength = secretLength;
            return this;
        }

        public TotpConfiguration build() {
            return new TotpConfiguration(issuer, algorithm, digits, period, discrepancy, secretLength);
        }
    }
}
