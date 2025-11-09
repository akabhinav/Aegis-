package io.aegis.mfa.push;

import java.util.Map;

/**
 * Interface for sending push notifications.
 * Implementations can use different push notification providers (FCM, APNS, etc.).
 *
 * @since 1.0.0
 */
public interface PushNotificationSender {

    /**
     * Sends a push notification to a device.
     *
     * @param deviceToken the device token (FCM registration token, APNS device token, etc.)
     * @param title the notification title
     * @param body the notification body
     * @param data additional data payload
     * @return result of the send operation
     * @throws PushException if sending fails
     */
    PushResult send(String deviceToken, String title, String body, Map<String, String> data) throws PushException;

    /**
     * Sends a push notification to multiple devices.
     *
     * @param deviceTokens the device tokens
     * @param title the notification title
     * @param body the notification body
     * @param data additional data payload
     * @return results for each device
     * @throws PushException if sending fails
     */
    default Map<String, PushResult> sendMulti(Iterable<String> deviceTokens, String title,
                                               String body, Map<String, String> data) throws PushException {
        Map<String, PushResult> results = new java.util.HashMap<>();
        for (String token : deviceTokens) {
            try {
                PushResult result = send(token, title, body, data);
                results.put(token, result);
            } catch (PushException e) {
                results.put(token, PushResult.failure(e.getMessage()));
            }
        }
        return results;
    }

    /**
     * Result of a push notification send operation.
     *
     * @param success whether the send was successful
     * @param messageId the message ID from the provider (if successful)
     * @param error the error message (if failed)
     */
    record PushResult(boolean success, String messageId, String error) {

        public static PushResult success(String messageId) {
            return new PushResult(true, messageId, null);
        }

        public static PushResult failure(String error) {
            return new PushResult(false, null, error);
        }
    }

    /**
     * Exception thrown when push notification sending fails.
     */
    class PushException extends Exception {
        public PushException(String message) {
            super(message);
        }

        public PushException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
