package io.aegis.core.exception;

import io.aegis.core.AuthenticationException;

/**
 * Exception thrown when provided credentials are invalid.
 *
 * @since 1.0.0
 */
public class InvalidCredentialsException extends AuthenticationException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
