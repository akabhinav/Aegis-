package io.aegis.mfa.email;

/**
 * Interface for sending emails.
 * Implementations can use different email providers (SMTP, SendGrid, AWS SES, etc.).
 *
 * @since 1.0.0
 */
public interface EmailSender {

    /**
     * Sends an email message.
     *
     * @param toAddress the recipient email address
     * @param subject the email subject
     * @param body the email body content
     * @param isHtml whether the body is HTML content
     * @return result of the send operation
     * @throws EmailException if sending fails
     */
    EmailResult send(String toAddress, String subject, String body, boolean isHtml) throws EmailException;

    /**
     * Result of an email send operation.
     *
     * @param success whether the send was successful
     * @param messageId the message ID from the provider (if successful)
     * @param error the error message (if failed)
     */
    record EmailResult(boolean success, String messageId, String error) {

        public static EmailResult success(String messageId) {
            return new EmailResult(true, messageId, null);
        }

        public static EmailResult failure(String error) {
            return new EmailResult(false, null, error);
        }
    }

    /**
     * Exception thrown when email sending fails.
     */
    class EmailException extends Exception {
        public EmailException(String message) {
            super(message);
        }

        public EmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
