package io.aegis.core;

/**
 * Base exception for all authentication-related errors.
 *
 * @since 1.0.0
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Creates an authentication exception with a message.
     *
     * @param message the error message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Creates an authentication exception with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an authentication exception with a cause.
     *
     * @param cause the underlying cause
     */
    public AuthenticationException(Throwable cause) {
        super(cause);
    }
}
