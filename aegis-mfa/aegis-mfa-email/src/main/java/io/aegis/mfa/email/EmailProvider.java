package io.aegis.mfa.email;

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
 * Email-based OTP provider for MFA.
 * Supports SMTP and other email delivery mechanisms.
 * Includes rate limiting and attempt tracking.
 *
 * @since 1.0.0
 */
public class EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(EmailProvider.class);

    private final EmailConfiguration configuration;
    private final EmailOtpGenerator otpGenerator;
    private final EmailSender emailSender;
    private final EmailTemplate emailTemplate;

    // Store active challenges: emailAddress -> ChallengeData
    private final Map<String, ChallengeData> activeChallenges = new ConcurrentHashMap<>();

    // Rate limiting: emailAddress -> RateLimitData
    private final Map<String, RateLimitData> rateLimits = new ConcurrentHashMap<>();

    /**
     * Creates an email provider with default configuration.
     *
     * @param emailSender the email sender implementation
     */
    public EmailProvider(EmailSender emailSender) {
        this(EmailConfiguration.DEFAULT, emailSender);
    }

    /**
     * Creates an email provider with custom configuration.
     *
     * @param configuration the email configuration
     * @param emailSender the email sender implementation
     */
    public EmailProvider(EmailConfiguration configuration, EmailSender emailSender) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (emailSender == null) {
            throw new IllegalArgumentException("Email sender cannot be null");
        }

        this.configuration = configuration;
        this.otpGenerator = new EmailOtpGenerator(configuration.codeLength());
        this.emailSender = emailSender;
        this.emailTemplate = new EmailTemplate(configuration);
    }

    /**
     * Generates and sends an email OTP challenge to an email address.
     *
     * @param userId the user ID
     * @param emailAddress the email address
     * @return MFA challenge with the challenge details
     * @throws EmailSender.EmailException if email sending fails
     */
    public MfaChallenge sendChallenge(String userId, String emailAddress) throws EmailSender.EmailException {
        return sendChallenge(userId, emailAddress, null);
    }

    /**
     * Generates and sends an email OTP challenge to an email address with recipient name.
     *
     * @param userId the user ID
     * @param emailAddress the email address
     * @param recipientName optional recipient name for personalization
     * @return MFA challenge with the challenge details
     * @throws EmailSender.EmailException if email sending fails
     */
    public MfaChallenge sendChallenge(String userId, String emailAddress, String recipientName)
            throws EmailSender.EmailException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (emailAddress == null || emailAddress.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or blank");
        }

        // Check rate limit
        if (isRateLimited(emailAddress)) {
            logger.warn("Rate limit exceeded for email address: {}", emailAddress);
            throw new EmailSender.EmailException("Rate limit exceeded. Please try again later.");
        }

        // Generate OTP code
        String code = otpGenerator.generateCode();

        // Generate email body
        String body = emailTemplate.generateBody(code, recipientName);

        // Send email
        logger.debug("Sending OTP to email address: {}", emailAddress);
        boolean isHtml = configuration.templateType() == EmailConfiguration.TemplateType.HTML;
        EmailSender.EmailResult result = emailSender.send(
                emailAddress,
                configuration.subject(),
                body,
                isHtml
        );

        if (!result.success()) {
            logger.error("Failed to send email: {}", result.error());
            throw new EmailSender.EmailException("Failed to send email: " + result.error());
        }

        // Store challenge
        Instant expiresAt = Instant.now().plus(configuration.validity());
        ChallengeData challengeData = new ChallengeData(code, expiresAt, new AtomicInteger(0));
        activeChallenges.put(emailAddress, challengeData);

        // Update rate limit
        updateRateLimit(emailAddress);

        logger.info("Email OTP sent successfully to: {}. Message ID: {}", emailAddress, result.messageId());

        return MfaChallenge.builder()
                .userId(userId)
                .method(MfaMethod.EMAIL)
                .secret(code) // Note: In production, don't expose the code
                .expiresAt(expiresAt)
                .addMetadata("emailAddress", emailAddress)
                .addMetadata("messageId", result.messageId())
                .addMetadata("validity", configuration.validity().toMinutes() + " minutes")
                .build();
    }

    /**
     * Verifies an email OTP code for an email address.
     *
     * @param userId the user ID
     * @param emailAddress the email address
     * @param code the OTP code to verify
     * @return MFA result indicating success or failure
     */
    public MfaResult verify(String userId, String emailAddress, String code) {
        if (userId == null || userId.isBlank()) {
            logger.warn("Verification failed: User ID is null or blank");
            return MfaResult.failure("User ID cannot be null or blank");
        }

        if (emailAddress == null || emailAddress.isBlank()) {
            logger.warn("Verification failed: Email address is null or blank");
            return MfaResult.failure("Email address cannot be null or blank");
        }

        if (code == null || code.isBlank()) {
            logger.warn("Verification failed for email {}: Code is null or blank", emailAddress);
            return MfaResult.failure("Code cannot be null or blank");
        }

        // Validate code format
        if (!otpGenerator.isValidFormat(code)) {
            logger.warn("Verification failed for email {}: Invalid code format", emailAddress);
            return MfaResult.failure("Invalid code format");
        }

        // Get challenge data
        ChallengeData challengeData = activeChallenges.get(emailAddress);
        if (challengeData == null) {
            logger.warn("Verification failed for email {}: No active challenge", emailAddress);
            return MfaResult.failure("No active challenge found");
        }

        // Check expiration
        if (Instant.now().isAfter(challengeData.expiresAt)) {
            logger.warn("Verification failed for email {}: Challenge expired", emailAddress);
            activeChallenges.remove(emailAddress);
            return MfaResult.failure("Challenge expired");
        }

        // Check max attempts
        int attempts = challengeData.attempts.incrementAndGet();
        if (attempts > configuration.maxAttempts()) {
            logger.warn("Verification failed for email {}: Max attempts exceeded", emailAddress);
            activeChallenges.remove(emailAddress);
            return MfaResult.failure("Maximum verification attempts exceeded");
        }

        // Verify code
        if (!challengeData.code.equals(code)) {
            logger.warn("Verification failed for email {}: Invalid code (attempt {}/{})",
                    emailAddress, attempts, configuration.maxAttempts());
            return MfaResult.failure("Invalid code");
        }

        // Success - remove challenge
        activeChallenges.remove(emailAddress);
        logger.info("Email OTP verification successful for: {}", emailAddress);

        return new MfaResult.Success(
                userId,
                Instant.now(),
                Map.of(
                        "method", "EMAIL",
                        "emailAddress", emailAddress
                )
        );
    }

    /**
     * Checks if an email address is rate limited.
     *
     * @param emailAddress the email address
     * @return true if rate limited, false otherwise
     */
    private boolean isRateLimited(String emailAddress) {
        RateLimitData rateLimitData = rateLimits.get(emailAddress);
        if (rateLimitData == null) {
            return false;
        }

        // Clean up expired rate limit data
        Instant oneMinuteAgo = Instant.now().minus(Duration.ofMinutes(1));
        rateLimitData.timestamps.removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));

        return rateLimitData.timestamps.size() >= configuration.rateLimitPerMinute();
    }

    /**
     * Updates the rate limit for an email address.
     *
     * @param emailAddress the email address
     */
    private void updateRateLimit(String emailAddress) {
        rateLimits.compute(emailAddress, (key, data) -> {
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
     * Removes the challenge for a specific email address.
     *
     * @param emailAddress the email address
     */
    public void removeChallenge(String emailAddress) {
        activeChallenges.remove(emailAddress);
        logger.debug("Removed challenge for email: {}", emailAddress);
    }

    public EmailConfiguration getConfiguration() {
        return configuration;
    }

    public EmailOtpGenerator getOtpGenerator() {
        return otpGenerator;
    }

    public EmailTemplate getEmailTemplate() {
        return emailTemplate;
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
