package io.aegis.core.exception;

import io.aegis.core.AuthenticationException;

/**
 * Exception thrown when a token is invalid or malformed.
 *
 * @since 1.0.0
 */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
