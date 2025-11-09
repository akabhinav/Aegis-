package io.aegis.mfa.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) push notification sender implementation.
 * Supports both Android and iOS devices through FCM.
 *
 * @since 1.0.0
 */
public class FcmPushNotificationSender implements PushNotificationSender {

    private static final Logger logger = LoggerFactory.getLogger(FcmPushNotificationSender.class);

    private final FirebaseApp firebaseApp;
    private final String appName;
    private final boolean highPriority;

    /**
     * Creates an FCM sender using service account credentials file.
     *
     * @param serviceAccountPath path to the Firebase service account JSON file
     * @throws IOException if credentials cannot be loaded
     */
    public FcmPushNotificationSender(String serviceAccountPath) throws IOException {
        this(serviceAccountPath, "aegis-push", true);
    }

    /**
     * Creates an FCM sender with custom configuration.
     *
     * @param serviceAccountPath path to the Firebase service account JSON file
     * @param appName unique name for this Firebase app instance
     * @param highPriority whether to send notifications with high priority
     * @throws IOException if credentials cannot be loaded
     */
    public FcmPushNotificationSender(String serviceAccountPath, String appName, boolean highPriority) throws IOException {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            throw new IllegalArgumentException("Service account path cannot be null or blank");
        }
        if (appName == null || appName.isBlank()) {
            throw new IllegalArgumentException("App name cannot be null or blank");
        }

        this.appName = appName;
        this.highPriority = highPriority;

        try (InputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Check if app already exists
            try {
                this.firebaseApp = FirebaseApp.getInstance(appName);
                logger.info("Using existing Firebase app: {}", appName);
            } catch (IllegalStateException e) {
                this.firebaseApp = FirebaseApp.initializeApp(options, appName);
                logger.info("Initialized new Firebase app: {}", appName);
            }
        }
    }

    /**
     * Creates an FCM sender using GoogleCredentials directly.
     *
     * @param credentials Google credentials
     * @param appName unique name for this Firebase app instance
     * @param highPriority whether to send notifications with high priority
     */
    public FcmPushNotificationSender(GoogleCredentials credentials, String appName, boolean highPriority) {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials cannot be null");
        }
        if (appName == null || appName.isBlank()) {
            throw new IllegalArgumentException("App name cannot be null or blank");
        }

        this.appName = appName;
        this.highPriority = highPriority;

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        // Check if app already exists
        try {
            this.firebaseApp = FirebaseApp.getInstance(appName);
            logger.info("Using existing Firebase app: {}", appName);
        } catch (IllegalStateException e) {
            this.firebaseApp = FirebaseApp.initializeApp(options, appName);
            logger.info("Initialized new Firebase app: {}", appName);
        }
    }

    @Override
    public PushResult send(String deviceToken, String title, String body, Map<String, String> data) throws PushException {
        if (deviceToken == null || deviceToken.isBlank()) {
            throw new PushException("Device token cannot be null or blank");
        }
        if (title == null || title.isBlank()) {
            throw new PushException("Title cannot be null or blank");
        }
        if (body == null || body.isBlank()) {
            throw new PushException("Body cannot be null or blank");
        }

        try {
            logger.debug("Sending push notification via FCM to device: {}", maskToken(deviceToken));

            // Build notification
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Build Android config
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(highPriority ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL)
                    .setNotification(AndroidNotification.builder()
                            .setClickAction("AEGIS_MFA_APPROVAL")
                            .build())
                    .build();

            // Build APNS config for iOS
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setCategory("AEGIS_MFA_APPROVAL")
                            .setContentAvailable(true)
                            .setSound("default")
                            .build())
                    .build();

            // Prepare data payload
            Map<String, String> dataPayload = new HashMap<>();
            if (data != null) {
                dataPayload.putAll(data);
            }
            dataPayload.put("type", "mfa_approval");
            dataPayload.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // Build message
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig)
                    .putAllData(dataPayload)
                    .build();

            // Send message
            String messageId = FirebaseMessaging.getInstance(firebaseApp).send(message);

            logger.info("Push notification sent successfully via FCM. Message ID: {}", messageId);

            return PushResult.success(messageId);

        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send push notification via FCM: {}", e.getMessage(), e);

            // Handle specific error codes
            String errorMessage = switch (e.getMessagingErrorCode()) {
                case INVALID_ARGUMENT -> "Invalid device token or message format";
                case UNREGISTERED -> "Device token is not registered";
                case SENDER_ID_MISMATCH -> "Device token belongs to different sender";
                case QUOTA_EXCEEDED -> "FCM quota exceeded";
                case UNAVAILABLE -> "FCM service temporarily unavailable";
                case INTERNAL -> "Internal FCM error";
                default -> "FCM error: " + e.getMessage();
            };

            throw new PushException(errorMessage, e);

        } catch (Exception e) {
            logger.error("Unexpected error sending push notification", e);
            throw new PushException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Masks a device token for logging (shows first and last 4 characters).
     *
     * @param token the device token
     * @return masked token
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    public String getAppName() {
        return appName;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    /**
     * Cleans up resources.
     */
    public void shutdown() {
        if (firebaseApp != null) {
            firebaseApp.delete();
            logger.info("Firebase app shutdown: {}", appName);
        }
    }
}
