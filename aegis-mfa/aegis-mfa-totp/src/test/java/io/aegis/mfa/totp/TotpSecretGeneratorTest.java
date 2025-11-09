package io.aegis.mfa.totp;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TotpSecretGenerator.
 */
class TotpSecretGeneratorTest {

    @Test
    void testDefaultConstructor() {
        // When
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // Then
        assertNotNull(generator);
        assertEquals(20, generator.getSecretLength());
    }

    @Test
    void testCustomSecretLength() {
        // When
        TotpSecretGenerator generator = new TotpSecretGenerator(32);

        // Then
        assertEquals(32, generator.getSecretLength());
    }

    @Test
    void testConstructor_InvalidLength_TooSmall() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpSecretGenerator(15)
        );
    }

    @Test
    void testConstructor_InvalidLength_TooLarge() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new TotpSecretGenerator(129)
        );
    }

    @Test
    void testConstructor_ValidLength_Min() {
        // When/Then
        assertDoesNotThrow(() ->
                new TotpSecretGenerator(16)
        );
    }

    @Test
    void testConstructor_ValidLength_Max() {
        // When/Then
        assertDoesNotThrow(() ->
                new TotpSecretGenerator(128)
        );
    }

    @Test
    void testGenerateSecret() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        String secret = generator.generateSecret();

        // Then
        assertNotNull(secret);
        assertFalse(secret.isBlank());
        assertTrue(secret.length() >= 16);
    }

    @Test
    void testGenerateSecret_IsBase32() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        String secret = generator.generateSecret();

        // Then
        // Base32 alphabet is A-Z and 2-7
        assertTrue(secret.matches("^[A-Z2-7]+=*$"));
    }

    @Test
    void testGenerateSecret_IsUnique() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();
        Set<String> secrets = new HashSet<>();
        int iterations = 100;

        // When
        for (int i = 0; i < iterations; i++) {
            secrets.add(generator.generateSecret());
        }

        // Then - All generated secrets should be unique
        assertEquals(iterations, secrets.size());
    }

    @Test
    void testIsValidSecret_ValidSecret() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();
        String secret = generator.generateSecret();

        // When
        boolean isValid = generator.isValidSecret(secret);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidSecret_NullSecret() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        boolean isValid = generator.isValidSecret(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidSecret_BlankSecret() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        boolean isValid = generator.isValidSecret("   ");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidSecret_EmptySecret() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        boolean isValid = generator.isValidSecret("");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidSecret_InvalidCharacters() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        boolean isValid = generator.isValidSecret("invalid-secret!@#$");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidSecret_LowercaseLetters() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        boolean isValid = generator.isValidSecret("jbswy3dpehpk3pxp");

        // Then
        assertFalse(isValid); // Base32 should be uppercase
    }

    @Test
    void testIsValidSecret_TooShort() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        boolean isValid = generator.isValidSecret("ABCDEFG");

        // Then
        assertFalse(isValid); // Less than 16 characters
    }

    @Test
    void testIsValidSecret_ValidWithPadding() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When
        boolean isValid = generator.isValidSecret("JBSWY3DPEHPK3PXP====");

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidSecret_ValidBase32Alphabet() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When - Test all valid Base32 characters
        boolean isValid = generator.isValidSecret("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567");

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidSecret_InvalidBase32Characters() {
        // Given
        TotpSecretGenerator generator = new TotpSecretGenerator();

        // When - '0', '1', '8', '9' are not valid in Base32
        boolean isValid1 = generator.isValidSecret("ABCDEFGH01234567IJKLMNOP");
        boolean isValid2 = generator.isValidSecret("ABCDEFGH89IJKLMNOPQRSTUV");

        // Then
        assertFalse(isValid1);
        assertFalse(isValid2);
    }
}
