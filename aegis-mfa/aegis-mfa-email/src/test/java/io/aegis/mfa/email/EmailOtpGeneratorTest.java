package io.aegis.mfa.email;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailOtpGenerator.
 */
class EmailOtpGeneratorTest {

    @Test
    void testDefaultConstructor() {
        // When
        EmailOtpGenerator generator = new EmailOtpGenerator();

        // Then
        assertNotNull(generator);
        assertEquals(6, generator.getCodeLength());
    }

    @Test
    void testCustomCodeLength() {
        // When
        EmailOtpGenerator generator = new EmailOtpGenerator(8);

        // Then
        assertEquals(8, generator.getCodeLength());
    }

    @Test
    void testConstructor_InvalidLength_TooSmall() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new EmailOtpGenerator(3)
        );
    }

    @Test
    void testConstructor_InvalidLength_TooLarge() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new EmailOtpGenerator(9)
        );
    }

    @Test
    void testGenerateCode_DefaultLength() {
        // Given
        EmailOtpGenerator generator = new EmailOtpGenerator();

        // When
        String code = generator.generateCode();

        // Then
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void testGenerateCode_CustomLength() {
        // Given
        EmailOtpGenerator generator = new EmailOtpGenerator(8);

        // When
        String code = generator.generateCode();

        // Then
        assertEquals(8, code.length());
        assertTrue(code.matches("\\d{8}"));
    }

    @Test
    void testGenerateCode_IsUnique() {
        // Given
        EmailOtpGenerator generator = new EmailOtpGenerator();
        Set<String> codes = new HashSet<>();
        int iterations = 100;

        // When
        for (int i = 0; i < iterations; i++) {
            codes.add(generator.generateCode());
        }

        // Then - Most codes should be unique
        assertTrue(codes.size() > iterations * 0.95);
    }

    @Test
    void testIsValidFormat_ValidCode() {
        // Given
        EmailOtpGenerator generator = new EmailOtpGenerator();
        String code = generator.generateCode();

        // When
        boolean isValid = generator.isValidFormat(code);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidFormat_NullCode() {
        // Given
        EmailOtpGenerator generator = new EmailOtpGenerator();

        // When
        boolean isValid = generator.isValidFormat(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidFormat_WrongLength() {
        // Given
        EmailOtpGenerator generator = new EmailOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("12345");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidFormat_NonNumeric() {
        // Given
        EmailOtpGenerator generator = new EmailOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("12a456");

        // Then
        assertFalse(isValid);
    }
}
