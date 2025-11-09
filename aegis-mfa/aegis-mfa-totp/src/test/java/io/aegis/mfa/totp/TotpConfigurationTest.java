package io.aegis.mfa.totp;

import dev.samstevens.totp.code.HashingAlgorithm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TotpConfiguration.
 */
class TotpConfigurationTest {

    @Test
    void testDefaultConfiguration() {
        // When
        TotpConfiguration config = TotpConfiguration.DEFAULT;

        // Then
        assertNotNull(config);
        assertEquals("Aegis", config.issuer());
        assertEquals(HashingAlgorithm.SHA1, config.algorithm());
        assertEquals(6, config.digits());
        assertEquals(30, config.period());
        assertEquals(1, config.discrepancy());
        assertEquals(20, config.secretLength());
    }

    @Test
    void testBuilder_AllFields() {
        // When
        TotpConfiguration config = TotpConfiguration.builder()
                .issuer("Custom Issuer")
                .algorithm(HashingAlgorithm.SHA256)
                .digits(8)
                .period(60)
                .discrepancy(2)
                .secretLength(32)
                .build();

        // Then
        assertEquals("Custom Issuer", config.issuer());
        assertEquals(HashingAlgorithm.SHA256, config.algorithm());
        assertEquals(8, config.digits());
        assertEquals(60, config.period());
        assertEquals(2, config.discrepancy());
        assertEquals(32, config.secretLength());
    }

    @Test
    void testBuilder_DefaultValues() {
        // When
        TotpConfiguration config = TotpConfiguration.builder().build();

        // Then
        assertEquals("Aegis", config.issuer());
        assertEquals(HashingAlgorithm.SHA1, config.algorithm());
        assertEquals(6, config.digits());
        assertEquals(30, config.period());
        assertEquals(1, config.discrepancy());
        assertEquals(20, config.secretLength());
    }

    @Test
    void testValidation_NullIssuer() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration(null, HashingAlgorithm.SHA1, 6, 30, 1, 20)
        );
    }

    @Test
    void testValidation_BlankIssuer() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("   ", HashingAlgorithm.SHA1, 6, 30, 1, 20)
        );
    }

    @Test
    void testValidation_NullAlgorithm() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", null, 6, 30, 1, 20)
        );
    }

    @Test
    void testValidation_InvalidDigits_Five() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 5, 30, 1, 20)
        );
    }

    @Test
    void testValidation_InvalidDigits_Seven() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 7, 30, 1, 20)
        );
    }

    @Test
    void testValidation_ValidDigits_Six() {
        // When/Then
        assertDoesNotThrow(() ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 30, 1, 20)
        );
    }

    @Test
    void testValidation_ValidDigits_Eight() {
        // When/Then
        assertDoesNotThrow(() ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 8, 30, 1, 20)
        );
    }

    @Test
    void testValidation_InvalidPeriod_Zero() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 0, 1, 20)
        );
    }

    @Test
    void testValidation_InvalidPeriod_Negative() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, -30, 1, 20)
        );
    }

    @Test
    void testValidation_InvalidDiscrepancy_Negative() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 30, -1, 20)
        );
    }

    @Test
    void testValidation_ValidDiscrepancy_Zero() {
        // When/Then
        assertDoesNotThrow(() ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 30, 0, 20)
        );
    }

    @Test
    void testValidation_SecretLength_TooSmall() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 30, 1, 15)
        );
    }

    @Test
    void testValidation_SecretLength_TooLarge() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 30, 1, 129)
        );
    }

    @Test
    void testValidation_SecretLength_MinValid() {
        // When/Then
        assertDoesNotThrow(() ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 30, 1, 16)
        );
    }

    @Test
    void testValidation_SecretLength_MaxValid() {
        // When/Then
        assertDoesNotThrow(() ->
                new TotpConfiguration("Issuer", HashingAlgorithm.SHA1, 6, 30, 1, 128)
        );
    }

    @Test
    void testBuilder_FluentInterface() {
        // When
        TotpConfiguration config = TotpConfiguration.builder()
                .issuer("Test")
                .algorithm(HashingAlgorithm.SHA512)
                .digits(8)
                .period(45)
                .discrepancy(3)
                .secretLength(24)
                .build();

        // Then
        assertEquals("Test", config.issuer());
        assertEquals(HashingAlgorithm.SHA512, config.algorithm());
        assertEquals(8, config.digits());
        assertEquals(45, config.period());
        assertEquals(3, config.discrepancy());
        assertEquals(24, config.secretLength());
    }
}
