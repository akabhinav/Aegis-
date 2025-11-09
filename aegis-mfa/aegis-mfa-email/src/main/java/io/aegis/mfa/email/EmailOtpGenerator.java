package io.aegis.mfa.email;

import java.security.SecureRandom;

/**
 * Generates secure random OTP codes for email authentication.
 * Uses SecureRandom for cryptographically strong random number generation.
 *
 * @since 1.0.0
 */
public class EmailOtpGenerator {

    private final SecureRandom random;
    private final int codeLength;

    /**
     * Creates an OTP generator with default length (6 digits).
     */
    public EmailOtpGenerator() {
        this(6);
    }

    /**
     * Creates an OTP generator with specified length.
     *
     * @param codeLength the length of the OTP code (4-8 digits)
     */
    public EmailOtpGenerator(int codeLength) {
        if (codeLength < 4 || codeLength > 8) {
            throw new IllegalArgumentException("Code length must be between 4 and 8 digits");
        }
        this.codeLength = codeLength;
        this.random = new SecureRandom();
    }

    /**
     * Generates a random OTP code.
     *
     * @return OTP code as a string (zero-padded to the specified length)
     */
    public String generateCode() {
        int max = (int) Math.pow(10, codeLength) - 1;
        int min = (int) Math.pow(10, codeLength - 1);

        // Generate a random number between min and max (inclusive)
        int code = random.nextInt(max - min + 1) + min;

        // Format with leading zeros if needed
        return String.format("%0" + codeLength + "d", code);
    }

    /**
     * Validates an OTP code format.
     *
     * @param code the code to validate
     * @return true if valid format, false otherwise
     */
    public boolean isValidFormat(String code) {
        if (code == null || code.length() != codeLength) {
            return false;
        }
        return code.matches("\\d{" + codeLength + "}");
    }

    public int getCodeLength() {
        return codeLength;
    }
}
