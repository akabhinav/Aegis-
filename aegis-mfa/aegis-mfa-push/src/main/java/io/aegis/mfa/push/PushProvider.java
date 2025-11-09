package io.aegis.mfa.push;

import io.aegis.mfa.core.MfaChallenge;
import io.aegis.mfa.core.MfaMethod;
import io.aegis.mfa.core.MfaProvider;
import io.aegis.mfa.core.MfaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Push notification-based MFA provider.
 * Sends push notifications to registered devices for approval-based authentication.
 * Thread-safe implementation with rate limiting and challenge management.
 *
 * @since 1.0.0
 */
public class PushProvider implements MfaProvider {

    private static final Logger logger = LoggerFactory.getLogger(PushProvider.class);

    private final PushConfiguration configuration;
    private final PushNotificationSender notificationSender;

    // Thread-safe storage for pending challenges
    private final Map<String, ChallengeData> pendingChallenges = new ConcurrentHashMap<>();

    // Rate limiting per device token
    private final Map<String, RateLimitData> rateLimits = new ConcurrentHashMap<>();

    /**
     * Creates a push MFA provider with default configuration.
     *
     * @param notificationSender the notification sender implementation
     */
    public PushProvider(PushNotificationSender notificationSender) {
        this(PushConfiguration.DEFAULT, notificationSender);
    }

    /**
     * Creates a push MFA provider with custom configuration.
     *
     * @param configuration the push configuration
     * @param notificationSender the notification sender implementation
     */
    public PushProvider(PushConfiguration configuration, PushNotificationSender notificationSender) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (notificationSender == null) {
            throw new IllegalArgumentException("Notification sender cannot be null");
        }

        this.configuration = configuration;
        this.notificationSender = notificationSender;

        logger.info("PushProvider initialized with timeout: {}, max pending: {}",
                configuration.approvalTimeout(), configuration.maxPendingChallenges());
    }

    @Override
    public MfaMethod getMethod() {
        return MfaMethod.PUSH;
    }

    /**
     * Sends a push notification challenge to the user's device.
     *
     * @param userId user identifier
     * @param deviceToken device registration token (FCM token, APNS token, etc.)
     * @param location location information (optional)
     * @param ipAddress IP address (optional)
     * @param deviceInfo device information (optional)
     * @return the MFA challenge
     * @throws PushNotificationSender.PushException if sending fails
     */
    public MfaChallenge sendChallenge(String userId, String deviceToken, String location,
                                      String ipAddress, String deviceInfo)
            throws PushNotificationSender.PushException {
        if (userId == null || userId.isBlank()) {
            throw new PushNotificationSender.PushException("User ID cannot be null or blank");
        }
        if (deviceToken == null || deviceToken.isBlank()) {
            throw new PushNotificationSender.PushException("Device token cannot be null or blank");
        }

        // Check rate limit
        if (isRateLimited(deviceToken)) {
            throw new PushNotificationSender.PushException(
                    "Rate limit exceeded. Please wait before requesting another push notification.");
        }

        // Check max pending challenges
        long pendingCount = pendingChallenges.values().stream()
                .filter(data -> data.userId.equals(userId))
                .count();

        if (pendingCount >= configuration.maxPendingChallenges()) {
            throw new PushNotificationSender.PushException(
                    "Maximum pending challenges reached. Please approve or deny existing challenges first.");
        }

        // Generate challenge ID
        String challengeId = UUID.randomUUID().toString();

        // Format notification body with context
        String body = configuration.formatBody(location, ipAddress, deviceInfo);

        // Prepare data payload
        Map<String, String> data = new HashMap<>();
        data.put("challenge_id", challengeId);
        data.put("user_id", userId);
        data.put("require_biometric", String.valueOf(configuration.requireBiometric()));
        if (location != null) data.put("location", location);
        if (ipAddress != null) data.put("ip_address", ipAddress);
        if (deviceInfo != null) data.put("device_info", deviceInfo);

        // Send push notification
        PushNotificationSender.PushResult result = notificationSender.send(
                deviceToken,
                configuration.title(),
                body,
                data
        );

        if (!result.success()) {
            throw new PushNotificationSender.PushException("Failed to send push notification: " + result.error());
        }

        // Store challenge data
        Instant expiresAt = Instant.now().plus(configuration.approvalTimeout());
        ChallengeData challengeData = new ChallengeData(
                challengeId,
                userId,
                deviceToken,
                expiresAt,
                ChallengeStatus.PENDING,
                location,
                ipAddress,
                deviceInfo
        );
        pendingChallenges.put(challengeId, challengeData);

        // Update rate limit
        updateRateLimit(deviceToken);

        logger.info("Push challenge sent to user: {} with challenge ID: {}", userId, challengeId);

        // Create MFA challenge
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("challenge_id", challengeId);
        metadata.put("message_id", result.messageId());
        metadata.put("expires_at", expiresAt.toString());
        metadata.put("require_biometric", configuration.requireBiometric());

        return new MfaChallenge(
                MfaMethod.PUSH,
                challengeId,
                expiresAt,
                metadata
        );
    }

    /**
     * Approves a pending challenge.
     *
     * @param challengeId the challenge ID
     * @param biometricVerified whether biometric authentication was verified (if required)
     * @return the MFA result
     */
    public MfaResult approve(String challengeId, boolean biometricVerified) {
        if (challengeId == null || challengeId.isBlank()) {
            return MfaResult.failure("Challenge ID cannot be null or blank");
        }

        ChallengeData challengeData = pendingChallenges.get(challengeId);

        if (challengeData == null) {
            return MfaResult.failure("Challenge not found or already processed");
        }

        // Check if challenge has expired
        if (Instant.now().isAfter(challengeData.expiresAt)) {
            pendingChallenges.remove(challengeId);
            return MfaResult.failure("Challenge has expired");
        }

        // Check if biometric is required but not verified
        if (configuration.requireBiometric() && !biometricVerified) {
            return MfaResult.failure("Biometric authentication required but not verified");
        }

        // Mark as approved and remove from pending
        pendingChallenges.remove(challengeId);

        logger.info("Challenge approved by user: {} for challenge ID: {}", challengeData.userId, challengeId);

        // Create success result
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("challenge_id", challengeId);
        metadata.put("approved_at", Instant.now().toString());
        metadata.put("biometric_verified", biometricVerified);
        if (challengeData.location != null) metadata.put("location", challengeData.location);
        if (challengeData.ipAddress != null) metadata.put("ip_address", challengeData.ipAddress);
        if (challengeData.deviceInfo != null) metadata.put("device_info", challengeData.deviceInfo);

        return new MfaResult.Success(
                challengeData.userId,
                Instant.now(),
                metadata
        );
    }

    /**
     * Denies a pending challenge.
     *
     * @param challengeId the challenge ID
     * @return the MFA result
     */
    public MfaResult deny(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) {
            return MfaResult.failure("Challenge ID cannot be null or blank");
        }

        ChallengeData challengeData = pendingChallenges.get(challengeId);

        if (challengeData == null) {
            return MfaResult.failure("Challenge not found or already processed");
        }

        // Remove from pending
        pendingChallenges.remove(challengeId);

        logger.info("Challenge denied by user: {} for challenge ID: {}", challengeData.userId, challengeId);

        return MfaResult.failure("Authentication denied by user");
    }

    /**
     * Gets the status of a pending challenge.
     *
     * @param challengeId the challenge ID
     * @return the challenge status, or empty if not found
     */
    public Optional<ChallengeStatus> getChallengeStatus(String challengeId) {
        ChallengeData challengeData = pendingChallenges.get(challengeId);
        if (challengeData == null) {
            return Optional.empty();
        }

        // Check if expired
        if (Instant.now().isAfter(challengeData.expiresAt)) {
            pendingChallenges.remove(challengeId);
            return Optional.of(ChallengeStatus.EXPIRED);
        }

        return Optional.of(challengeData.status);
    }

    /**
     * Gets all pending challenges for a user.
     *
     * @param userId the user ID
     * @return list of pending challenge IDs
     */
    public List<String> getPendingChallenges(String userId) {
        List<String> challenges = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<String, ChallengeData> entry : pendingChallenges.entrySet()) {
            ChallengeData data = entry.getValue();

            // Remove expired challenges
            if (now.isAfter(data.expiresAt)) {
                pendingChallenges.remove(entry.getKey());
                continue;
            }

            if (data.userId.equals(userId)) {
                challenges.add(entry.getKey());
            }
        }

        return challenges;
    }

    /**
     * Cancels a pending challenge.
     *
     * @param challengeId the challenge ID
     * @return true if cancelled, false if not found
     */
    public boolean cancelChallenge(String challengeId) {
        ChallengeData removed = pendingChallenges.remove(challengeId);
        if (removed != null) {
            logger.info("Challenge cancelled: {}", challengeId);
            return true;
        }
        return false;
    }

    /**
     * Cleans up expired challenges and old rate limit data.
     */
    public void cleanup() {
        Instant now = Instant.now();

        // Remove expired challenges
        pendingChallenges.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().expiresAt)) {
                logger.debug("Removing expired challenge: {}", entry.getKey());
                return true;
            }
            return false;
        });

        // Clean up old rate limit data
        Instant oneMinuteAgo = now.minusSeconds(60);
        rateLimits.entrySet().removeIf(entry -> {
            entry.getValue().timestamps.removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));
            return entry.getValue().timestamps.isEmpty();
        });

        logger.debug("Cleanup completed. Pending challenges: {}, Rate limit entries: {}",
                pendingChallenges.size(), rateLimits.size());
    }

    private boolean isRateLimited(String deviceToken) {
        RateLimitData rateLimitData = rateLimits.get(deviceToken);
        if (rateLimitData == null) {
            return false;
        }

        // Remove timestamps older than 1 minute
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        rateLimitData.timestamps.removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));

        return rateLimitData.timestamps.size() >= configuration.rateLimitPerMinute();
    }

    private void updateRateLimit(String deviceToken) {
        rateLimits.computeIfAbsent(deviceToken, k -> new RateLimitData())
                .timestamps.add(Instant.now());
    }

    public PushConfiguration getConfiguration() {
        return configuration;
    }

    public int getPendingChallengeCount() {
        return pendingChallenges.size();
    }

    /**
     * Challenge status.
     */
    public enum ChallengeStatus {
        PENDING,
        APPROVED,
        DENIED,
        EXPIRED
    }

    /**
     * Internal class to store challenge data.
     */
    private record ChallengeData(
            String challengeId,
            String userId,
            String deviceToken,
            Instant expiresAt,
            ChallengeStatus status,
            String location,
            String ipAddress,
            String deviceInfo
    ) {}

    /**
     * Internal class to track rate limiting.
     */
    private static class RateLimitData {
        final List<Instant> timestamps = new CopyOnWriteArrayList<>();
    }
}
