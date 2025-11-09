package io.aegis.core.context;

/**
 * Enumeration of supported authentication context types.
 *
 * @since 1.0.0
 */
public enum AuthenticationContextType {
    /**
     * JWT (JSON Web Token) authentication
     */
    JWT,

    /**
     * OAuth 2.0 / OpenID Connect authentication
     */
    OAUTH2,

    /**
     * API Key authentication
     */
    API_KEY,

    /**
     * HTTP Basic authentication
     */
    BASIC,

    /**
     * Mutual TLS (mTLS) certificate authentication
     */
    MTLS,

    /**
     * SAML 2.0 authentication
     */
    SAML,

    /**
     * Session-based authentication
     */
    SESSION,

    /**
     * Custom authentication mechanism
     */
    CUSTOM
}
