package io.aegis.mfa.sms;

/**
 * Interface for sending SMS messages.
 * Implementations can use different SMS providers (Twilio, AWS SNS, etc.).
 *
 * @since 1.0.0
 */
public interface SmsSender {

    /**
     * Sends an SMS message to a phone number.
     *
     * @param phoneNumber the recipient phone number (E.164 format recommended)
     * @param message the message to send
     * @return result of the send operation
     * @throws SmsException if sending fails
     */
    SmsResult send(String phoneNumber, String message) throws SmsException;

    /**
     * Result of an SMS send operation.
     *
     * @param success whether the send was successful
     * @param messageId the message ID from the provider (if successful)
     * @param error the error message (if failed)
     */
    record SmsResult(boolean success, String messageId, String error) {

        public static SmsResult success(String messageId) {
            return new SmsResult(true, messageId, null);
        }

        public static SmsResult failure(String error) {
            return new SmsResult(false, null, error);
        }
    }

    /**
     * Exception thrown when SMS sending fails.
     */
    class SmsException extends Exception {
        public SmsException(String message) {
            super(message);
        }

        public SmsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
