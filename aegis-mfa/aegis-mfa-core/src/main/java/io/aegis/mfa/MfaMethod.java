package io.aegis.mfa;

/**
 * Enumeration of supported MFA methods.
 *
 * @since 1.0.0
 */
public enum MfaMethod {
    /**
     * Time-based One-Time Password (Google Authenticator, Authy, etc.)
     */
    TOTP,

    /**
     * SMS One-Time Password
     */
    SMS,

    /**
     * Email One-Time Password
     */
    EMAIL,

    /**
     * Push notification to mobile device
     */
    PUSH,

    /**
     * Backup/Recovery codes
     */
    BACKUP_CODE,

    /**
     * Hardware security token (YubiKey, RSA SecurID)
     */
    HARDWARE_TOKEN
}
