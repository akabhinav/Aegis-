package io.aegis.core.exception;

import io.aegis.core.AuthenticationException;

/**
 * Exception thrown when a token has expired.
 *
 * @since 1.0.0
 */
public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
