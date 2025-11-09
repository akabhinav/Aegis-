package io.aegis.mfa.totp;

import dev.samstevens.totp.code.HashingAlgorithm;
import io.aegis.mfa.MfaChallenge;
import io.aegis.mfa.MfaMethod;
import io.aegis.mfa.MfaResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TotpProvider.
 */
class TotpProviderTest {

    private TotpProvider provider;
    private TotpConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = TotpConfiguration.builder()
                .issuer("Test Issuer")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .discrepancy(1)
                .secretLength(20)
                .build();
        provider = new TotpProvider(configuration);
    }

    @Test
    void testGenerateEnrollmentChallenge() {
        // Given
        String userId = "test-user-123";

        // When
        MfaChallenge challenge = provider.generateEnrollmentChallenge(userId);

        // Then
        assertNotNull(challenge);
        assertEquals(userId, challenge.userId());
        assertEquals(MfaMethod.TOTP, challenge.method());
        assertNotNull(challenge.secret());
        assertFalse(challenge.secret().isBlank());
        assertFalse(challenge.isExpired());
        assertEquals("Test Issuer", challenge.metadata().get("issuer"));
        assertEquals(6, challenge.metadata().get("digits"));
        assertEquals(30, challenge.metadata().get("period"));
    }

    @Test
    void testGenerateEnrollmentChallenge_NullUserId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.generateEnrollmentChallenge(null)
        );
    }

    @Test
    void testVerify_ValidCode() throws Exception {
        // Given
        String userId = "test-user";
        MfaChallenge challenge = provider.generateEnrollmentChallenge(userId);
        String secret = challenge.secret();
        String code = provider.generateCurrentCode(secret);

        // When
        MfaResult result = provider.verify(userId, code, secret);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(userId, ((MfaResult.Success) result).userId());
    }

    @Test
    void testVerify_InvalidCode() {
        // Given
        String userId = "test-user";
        MfaChallenge challenge = provider.generateEnrollmentChallenge(userId);
        String secret = challenge.secret();
        String invalidCode = "000000";

        // When
        MfaResult result = provider.verify(userId, invalidCode, secret);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result instanceof MfaResult.Failure);
    }

    @Test
    void testVerify_NullCode() {
        // Given
        String userId = "test-user";
        String secret = "JBSWY3DPEHPK3PXP";

        // When
        MfaResult result = provider.verify(userId, null, secret);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Code cannot be null or blank", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_BlankCode() {
        // Given
        String userId = "test-user";
        String secret = "JBSWY3DPEHPK3PXP";

        // When
        MfaResult result = provider.verify(userId, "   ", secret);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Code cannot be null or blank", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_NullSecret() {
        // Given
        String userId = "test-user";
        String code = "123456";

        // When
        MfaResult result = provider.verify(userId, code, null);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Secret cannot be null or blank", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_InvalidCodeFormat() {
        // Given
        String userId = "test-user";
        String secret = "JBSWY3DPEHPK3PXP";
        String invalidCode = "12345"; // Only 5 digits

        // When
        MfaResult result = provider.verify(userId, invalidCode, secret);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Invalid code format", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerifyWithStoredSecret() throws Exception {
        // Given
        String userId = "test-user";
        MfaChallenge challenge = provider.generateEnrollmentChallenge(userId);
        String secret = challenge.secret();
        provider.storeSecret(userId, secret);
        String code = provider.generateCurrentCode(secret);

        // When
        MfaResult result = provider.verifyWithStoredSecret(userId, code);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void testVerifyWithStoredSecret_NoSecretStored() {
        // Given
        String userId = "unknown-user";
        String code = "123456";

        // When
        MfaResult result = provider.verifyWithStoredSecret(userId, code);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No TOTP secret found for user", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testStoreAndRemoveSecret() {
        // Given
        String userId = "test-user";
        String secret = "JBSWY3DPEHPK3PXP";

        // When - Store
        provider.storeSecret(userId, secret);

        // Then
        MfaResult result = provider.verifyWithStoredSecret(userId, "000000");
        assertNotNull(result); // Secret exists even if code is wrong

        // When - Remove
        provider.removeSecret(userId);

        // Then
        MfaResult resultAfterRemove = provider.verifyWithStoredSecret(userId, "000000");
        assertFalse(resultAfterRemove.isSuccess());
        assertEquals("No TOTP secret found for user", ((MfaResult.Failure) resultAfterRemove).reason());
    }

    @Test
    void testStoreSecret_InvalidSecret() {
        // Given
        String userId = "test-user";
        String invalidSecret = "invalid-secret!@#";

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.storeSecret(userId, invalidSecret)
        );
    }

    @Test
    void testGetProvisioningUri() {
        // Given
        String userId = "test@example.com";
        String secret = "JBSWY3DPEHPK3PXP";

        // When
        String uri = provider.getProvisioningUri(userId, secret);

        // Then
        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("Test Issuer"));
        assertTrue(uri.contains(userId));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
        assertTrue(uri.contains("algorithm=SHA1"));
    }

    @Test
    void testGenerateCurrentCode() throws Exception {
        // Given
        String secret = "JBSWY3DPEHPK3PXP";

        // When
        String code = provider.generateCurrentCode(secret);

        // Then
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void testConfiguration() {
        // When
        TotpConfiguration config = provider.getConfiguration();

        // Then
        assertNotNull(config);
        assertEquals("Test Issuer", config.issuer());
        assertEquals(HashingAlgorithm.SHA1, config.algorithm());
        assertEquals(6, config.digits());
        assertEquals(30, config.period());
        assertEquals(1, config.discrepancy());
    }

    @Test
    void testDefaultConstructor() {
        // When
        TotpProvider defaultProvider = new TotpProvider();

        // Then
        assertNotNull(defaultProvider);
        assertEquals(TotpConfiguration.DEFAULT, defaultProvider.getConfiguration());
    }
}
