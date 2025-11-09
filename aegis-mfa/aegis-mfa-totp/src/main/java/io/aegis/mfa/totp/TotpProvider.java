package io.aegis.mfa.totp;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.aegis.mfa.MfaChallenge;
import io.aegis.mfa.MfaMethod;
import io.aegis.mfa.MfaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TOTP (Time-based One-Time Password) provider for MFA.
 * Compatible with Google Authenticator, Authy, and other TOTP apps.
 *
 * @since 1.0.0
 */
public class TotpProvider {

    private static final Logger logger = LoggerFactory.getLogger(TotpProvider.class);

    private final TotpConfiguration configuration;
    private final TotpSecretGenerator secretGenerator;
    private final CodeGenerator codeGenerator;
    private final CodeVerifier codeVerifier;
    private final TimeProvider timeProvider;

    // Store active challenges: userId -> secret
    private final Map<String, String> activeSecrets = new ConcurrentHashMap<>();

    /**
     * Creates a TOTP provider with default configuration.
     */
    public TotpProvider() {
        this(TotpConfiguration.DEFAULT);
    }

    /**
     * Creates a TOTP provider with custom configuration.
     *
     * @param configuration the TOTP configuration
     */
    public TotpProvider(TotpConfiguration configuration) {
        this.configuration = configuration;
        this.secretGenerator = new TotpSecretGenerator(configuration.secretLength());
        this.timeProvider = new SystemTimeProvider();
        this.codeGenerator = new DefaultCodeGenerator(
                configuration.algorithm(),
                configuration.digits()
        );
        this.codeVerifier = new DefaultCodeVerifier(
                codeGenerator,
                timeProvider
        );
    }

    /**
     * Generates a new TOTP secret for a user and creates an enrollment challenge.
     *
     * @param userId the user ID
     * @return MFA challenge with the secret
     */
    public MfaChallenge generateEnrollmentChallenge(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }

        String secret = secretGenerator.generateSecret();
        activeSecrets.put(userId, secret);

        logger.debug("Generated TOTP enrollment challenge for user: {}", userId);

        return MfaChallenge.builder()
                .userId(userId)
                .method(MfaMethod.TOTP)
                .secret(secret)
                .expiresAt(Instant.now().plusSeconds(300)) // 5 minutes to enroll
                .addMetadata("issuer", configuration.issuer())
                .addMetadata("algorithm", configuration.algorithm().name())
                .addMetadata("digits", configuration.digits())
                .addMetadata("period", configuration.period())
                .build();
    }

    /**
     * Verifies a TOTP code for a user.
     *
     * @param userId the user ID
     * @param code the TOTP code to verify
     * @param secret the user's TOTP secret
     * @return MFA result indicating success or failure
     */
    public MfaResult verify(String userId, String code, String secret) {
        if (userId == null || userId.isBlank()) {
            logger.warn("Verification failed: User ID is null or blank");
            return MfaResult.failure("User ID cannot be null or blank");
        }

        if (code == null || code.isBlank()) {
            logger.warn("Verification failed for user {}: Code is null or blank", userId);
            return MfaResult.failure("Code cannot be null or blank");
        }

        if (secret == null || secret.isBlank()) {
            logger.warn("Verification failed for user {}: Secret is null or blank", userId);
            return MfaResult.failure("Secret cannot be null or blank");
        }

        // Validate code format
        if (!isValidCodeFormat(code)) {
            logger.warn("Verification failed for user {}: Invalid code format", userId);
            return MfaResult.failure("Invalid code format");
        }

        // Verify the code
        boolean isValid = codeVerifier.isValidCode(secret, code);

        if (isValid) {
            logger.info("TOTP verification successful for user: {}", userId);
            return new MfaResult.Success(
                    userId,
                    Instant.now(),
                    Map.of(
                            "method", "TOTP",
                            "issuer", configuration.issuer()
                    )
            );
        } else {
            logger.warn("TOTP verification failed for user: {}", userId);
            return MfaResult.failure("Invalid TOTP code");
        }
    }

    /**
     * Verifies a TOTP code using the stored secret for a user.
     *
     * @param userId the user ID
     * @param code the TOTP code to verify
     * @return MFA result indicating success or failure
     */
    public MfaResult verifyWithStoredSecret(String userId, String code) {
        String secret = activeSecrets.get(userId);
        if (secret == null) {
            logger.warn("No stored secret found for user: {}", userId);
            return MfaResult.failure("No TOTP secret found for user");
        }
        return verify(userId, code, secret);
    }

    /**
     * Stores a TOTP secret for a user (used after enrollment).
     *
     * @param userId the user ID
     * @param secret the TOTP secret
     */
    public void storeSecret(String userId, String secret) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (!secretGenerator.isValidSecret(secret)) {
            throw new IllegalArgumentException("Invalid TOTP secret format");
        }
        activeSecrets.put(userId, secret);
        logger.debug("Stored TOTP secret for user: {}", userId);
    }

    /**
     * Removes the stored secret for a user.
     *
     * @param userId the user ID
     */
    public void removeSecret(String userId) {
        activeSecrets.remove(userId);
        logger.debug("Removed TOTP secret for user: {}", userId);
    }

    /**
     * Generates the current TOTP code for a secret (for testing/debugging).
     *
     * @param secret the TOTP secret
     * @return the current TOTP code
     * @throws Exception if code generation fails
     */
    public String generateCurrentCode(String secret) throws Exception {
        long currentBucket = Math.floorDiv(timeProvider.getTime(), configuration.period());
        return codeGenerator.generate(secret, currentBucket);
    }

    /**
     * Generates a provisioning URI for QR code generation.
     * Format: otpauth://totp/{issuer}:{userId}?secret={secret}&issuer={issuer}&algorithm={algorithm}&digits={digits}&period={period}
     *
     * @param userId the user ID
     * @param secret the TOTP secret
     * @return provisioning URI
     */
    public String getProvisioningUri(String userId, String secret) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                configuration.issuer(),
                userId,
                secret,
                configuration.issuer(),
                configuration.algorithm().name(),
                configuration.digits(),
                configuration.period()
        );
    }

    /**
     * Validates the format of a TOTP code.
     *
     * @param code the code to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidCodeFormat(String code) {
        if (code.length() != configuration.digits()) {
            return false;
        }
        return code.matches("\\d+");
    }

    public TotpConfiguration getConfiguration() {
        return configuration;
    }

    public TotpSecretGenerator getSecretGenerator() {
        return secretGenerator;
    }
}
