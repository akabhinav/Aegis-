package io.aegis.mfa.sms;

import io.aegis.mfa.MfaChallenge;
import io.aegis.mfa.MfaMethod;
import io.aegis.mfa.MfaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SMS-based OTP provider for MFA.
 * Supports Twilio, AWS SNS, and custom SMS senders.
 * Includes rate limiting and attempt tracking.
 *
 * @since 1.0.0
 */
public class SmsProvider {

    private static final Logger logger = LoggerFactory.getLogger(SmsProvider.class);

    private final SmsConfiguration configuration;
    private final SmsOtpGenerator otpGenerator;
    private final SmsSender smsSender;

    // Store active challenges: phoneNumber -> ChallengeData
    private final Map<String, ChallengeData> activeChallenges = new ConcurrentHashMap<>();

    // Rate limiting: phoneNumber -> RateLimitData
    private final Map<String, RateLimitData> rateLimits = new ConcurrentHashMap<>();

    /**
     * Creates an SMS provider with default configuration.
     *
     * @param smsSender the SMS sender implementation
     */
    public SmsProvider(SmsSender smsSender) {
        this(SmsConfiguration.DEFAULT, smsSender);
    }

    /**
     * Creates an SMS provider with custom configuration.
     *
     * @param configuration the SMS configuration
     * @param smsSender the SMS sender implementation
     */
    public SmsProvider(SmsConfiguration configuration, SmsSender smsSender) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (smsSender == null) {
            throw new IllegalArgumentException("SMS sender cannot be null");
        }

        this.configuration = configuration;
        this.otpGenerator = new SmsOtpGenerator(configuration.codeLength());
        this.smsSender = smsSender;
    }

    /**
     * Generates and sends an SMS OTP challenge to a phone number.
     *
     * @param userId the user ID
     * @param phoneNumber the phone number (E.164 format recommended)
     * @return MFA challenge with the challenge details
     * @throws SmsSender.SmsException if SMS sending fails
     */
    public MfaChallenge sendChallenge(String userId, String phoneNumber) throws SmsSender.SmsException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }

        // Check rate limit
        if (isRateLimited(phoneNumber)) {
            logger.warn("Rate limit exceeded for phone number: {}", phoneNumber);
            throw new SmsSender.SmsException("Rate limit exceeded. Please try again later.");
        }

        // Generate OTP code
        String code = otpGenerator.generateCode();

        // Format message
        String message = configuration.formatMessage(code);

        // Send SMS
        logger.debug("Sending OTP to phone number: {}", phoneNumber);
        SmsSender.SmsResult result = smsSender.send(phoneNumber, message);

        if (!result.success()) {
            logger.error("Failed to send SMS: {}", result.error());
            throw new SmsSender.SmsException("Failed to send SMS: " + result.error());
        }

        // Store challenge
        Instant expiresAt = Instant.now().plus(configuration.validity());
        ChallengeData challengeData = new ChallengeData(code, expiresAt, new AtomicInteger(0));
        activeChallenges.put(phoneNumber, challengeData);

        // Update rate limit
        updateRateLimit(phoneNumber);

        logger.info("SMS OTP sent successfully to phone number: {}. Message ID: {}", phoneNumber, result.messageId());

        return MfaChallenge.builder()
                .userId(userId)
                .method(MfaMethod.SMS)
                .secret(code) // Note: In production, don't expose the code
                .expiresAt(expiresAt)
                .addMetadata("phoneNumber", phoneNumber)
                .addMetadata("messageId", result.messageId())
                .addMetadata("validity", configuration.validity().toMinutes() + " minutes")
                .build();
    }

    /**
     * Verifies an SMS OTP code for a phone number.
     *
     * @param userId the user ID
     * @param phoneNumber the phone number
     * @param code the OTP code to verify
     * @return MFA result indicating success or failure
     */
    public MfaResult verify(String userId, String phoneNumber, String code) {
        if (userId == null || userId.isBlank()) {
            logger.warn("Verification failed: User ID is null or blank");
            return MfaResult.failure("User ID cannot be null or blank");
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            logger.warn("Verification failed: Phone number is null or blank");
            return MfaResult.failure("Phone number cannot be null or blank");
        }

        if (code == null || code.isBlank()) {
            logger.warn("Verification failed for phone {}: Code is null or blank", phoneNumber);
            return MfaResult.failure("Code cannot be null or blank");
        }

        // Validate code format
        if (!otpGenerator.isValidFormat(code)) {
            logger.warn("Verification failed for phone {}: Invalid code format", phoneNumber);
            return MfaResult.failure("Invalid code format");
        }

        // Get challenge data
        ChallengeData challengeData = activeChallenges.get(phoneNumber);
        if (challengeData == null) {
            logger.warn("Verification failed for phone {}: No active challenge", phoneNumber);
            return MfaResult.failure("No active challenge found");
        }

        // Check expiration
        if (Instant.now().isAfter(challengeData.expiresAt)) {
            logger.warn("Verification failed for phone {}: Challenge expired", phoneNumber);
            activeChallenges.remove(phoneNumber);
            return MfaResult.failure("Challenge expired");
        }

        // Check max attempts
        int attempts = challengeData.attempts.incrementAndGet();
        if (attempts > configuration.maxAttempts()) {
            logger.warn("Verification failed for phone {}: Max attempts exceeded", phoneNumber);
            activeChallenges.remove(phoneNumber);
            return MfaResult.failure("Maximum verification attempts exceeded");
        }

        // Verify code
        if (!challengeData.code.equals(code)) {
            logger.warn("Verification failed for phone {}: Invalid code (attempt {}/{})",
                    phoneNumber, attempts, configuration.maxAttempts());
            return MfaResult.failure("Invalid code");
        }

        // Success - remove challenge
        activeChallenges.remove(phoneNumber);
        logger.info("SMS OTP verification successful for phone: {}", phoneNumber);

        return new MfaResult.Success(
                userId,
                Instant.now(),
                Map.of(
                        "method", "SMS",
                        "phoneNumber", phoneNumber
                )
        );
    }

    /**
     * Checks if a phone number is rate limited.
     *
     * @param phoneNumber the phone number
     * @return true if rate limited, false otherwise
     */
    private boolean isRateLimited(String phoneNumber) {
        RateLimitData rateLimitData = rateLimits.get(phoneNumber);
        if (rateLimitData == null) {
            return false;
        }

        // Clean up expired rate limit data
        Instant oneMinuteAgo = Instant.now().minus(Duration.ofMinutes(1));
        rateLimitData.timestamps.removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));

        return rateLimitData.timestamps.size() >= configuration.rateLimitPerMinute();
    }

    /**
     * Updates the rate limit for a phone number.
     *
     * @param phoneNumber the phone number
     */
    private void updateRateLimit(String phoneNumber) {
        rateLimits.compute(phoneNumber, (key, data) -> {
            if (data == null) {
                data = new RateLimitData();
            }
            data.timestamps.add(Instant.now());
            return data;
        });
    }

    /**
     * Clears all active challenges and rate limits.
     * Useful for testing and cleanup.
     */
    public void clearAll() {
        activeChallenges.clear();
        rateLimits.clear();
        logger.debug("Cleared all active challenges and rate limits");
    }

    /**
     * Removes the challenge for a specific phone number.
     *
     * @param phoneNumber the phone number
     */
    public void removeChallenge(String phoneNumber) {
        activeChallenges.remove(phoneNumber);
        logger.debug("Removed challenge for phone: {}", phoneNumber);
    }

    public SmsConfiguration getConfiguration() {
        return configuration;
    }

    public SmsOtpGenerator getOtpGenerator() {
        return otpGenerator;
    }

    /**
     * Internal class to store challenge data.
     */
    private static class ChallengeData {
        final String code;
        final Instant expiresAt;
        final AtomicInteger attempts;

        ChallengeData(String code, Instant expiresAt, AtomicInteger attempts) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.attempts = attempts;
        }
    }

    /**
     * Internal class to store rate limit data.
     */
    private static class RateLimitData {
        final java.util.List<Instant> timestamps = new java.util.concurrent.CopyOnWriteArrayList<>();
    }
}
