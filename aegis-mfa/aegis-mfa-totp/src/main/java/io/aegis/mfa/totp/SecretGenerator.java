package io.aegis.mfa.totp;

import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;

/**
 * Generates secure random secrets for TOTP.
 * Secrets are Base32-encoded and compatible with Google Authenticator.
 *
 * @since 1.0.0
 */
public class TotpSecretGenerator {

    private final SecretGenerator secretGenerator;
    private final int secretLength;

    /**
     * Creates a new secret generator with default length (20 bytes).
     */
    public TotpSecretGenerator() {
        this(20);
    }

    /**
     * Creates a new secret generator with specified length.
     *
     * @param secretLength the length of the secret in bytes (16-128)
     */
    public TotpSecretGenerator(int secretLength) {
        if (secretLength < 16 || secretLength > 128) {
            throw new IllegalArgumentException("Secret length must be between 16 and 128 bytes");
        }
        this.secretLength = secretLength;
        this.secretGenerator = new DefaultSecretGenerator(secretLength);
    }

    /**
     * Generates a new random Base32-encoded secret.
     *
     * @return Base32-encoded secret string
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Validates a secret string.
     *
     * @param secret the secret to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return false;
        }

        // Base32 alphabet
        String base32Regex = "^[A-Z2-7]+=*$";
        if (!secret.matches(base32Regex)) {
            return false;
        }

        // Check minimum length
        return secret.length() >= 16;
    }

    public int getSecretLength() {
        return secretLength;
    }
}
