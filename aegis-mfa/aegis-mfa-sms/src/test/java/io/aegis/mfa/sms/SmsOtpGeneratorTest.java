package io.aegis.mfa.sms;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SmsOtpGenerator.
 */
class SmsOtpGeneratorTest {

    @Test
    void testDefaultConstructor() {
        // When
        SmsOtpGenerator generator = new SmsOtpGenerator();

        // Then
        assertNotNull(generator);
        assertEquals(6, generator.getCodeLength());
    }

    @Test
    void testCustomCodeLength() {
        // When
        SmsOtpGenerator generator = new SmsOtpGenerator(8);

        // Then
        assertEquals(8, generator.getCodeLength());
    }

    @Test
    void testConstructor_InvalidLength_TooSmall() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new SmsOtpGenerator(3)
        );
    }

    @Test
    void testConstructor_InvalidLength_TooLarge() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new SmsOtpGenerator(9)
        );
    }

    @Test
    void testConstructor_ValidLength_Min() {
        // When/Then
        assertDoesNotThrow(() ->
                new SmsOtpGenerator(4)
        );
    }

    @Test
    void testConstructor_ValidLength_Max() {
        // When/Then
        assertDoesNotThrow(() ->
                new SmsOtpGenerator(8)
        );
    }

    @Test
    void testGenerateCode_DefaultLength() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator();

        // When
        String code = generator.generateCode();

        // Then
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void testGenerateCode_CustomLength_4() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(4);

        // When
        String code = generator.generateCode();

        // Then
        assertNotNull(code);
        assertEquals(4, code.length());
        assertTrue(code.matches("\\d{4}"));
    }

    @Test
    void testGenerateCode_CustomLength_8() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(8);

        // When
        String code = generator.generateCode();

        // Then
        assertNotNull(code);
        assertEquals(8, code.length());
        assertTrue(code.matches("\\d{8}"));
    }

    @Test
    void testGenerateCode_IsNumeric() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator();

        // When
        String code = generator.generateCode();

        // Then
        assertTrue(code.matches("\\d+"));
    }

    @Test
    void testGenerateCode_NoLeadingZeroLoss() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);
        boolean hasLeadingZero = false;

        // When - Generate multiple codes to find one with leading zero
        for (int i = 0; i < 1000; i++) {
            String code = generator.generateCode();
            if (code.startsWith("0")) {
                hasLeadingZero = true;
                assertEquals(6, code.length()); // Should still be 6 digits
                break;
            }
        }

        // Then - We should eventually get codes with leading zeros
        assertTrue(hasLeadingZero, "Should generate codes with leading zeros");
    }

    @Test
    void testGenerateCode_IsUnique() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator();
        Set<String> codes = new HashSet<>();
        int iterations = 100;

        // When
        for (int i = 0; i < iterations; i++) {
            codes.add(generator.generateCode());
        }

        // Then - Most codes should be unique (allow some duplicates due to randomness)
        assertTrue(codes.size() > iterations * 0.95); // At least 95% unique
    }

    @Test
    void testGenerateCode_InValidRange_6Digits() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);

        // When
        for (int i = 0; i < 100; i++) {
            String code = generator.generateCode();
            int codeValue = Integer.parseInt(code);

            // Then - Should be between 000000 and 999999
            assertTrue(codeValue >= 0 && codeValue <= 999999);
            assertEquals(6, code.length());
        }
    }

    @Test
    void testGenerateCode_InValidRange_4Digits() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(4);

        // When
        for (int i = 0; i < 100; i++) {
            String code = generator.generateCode();
            int codeValue = Integer.parseInt(code);

            // Then - Should be between 0000 and 9999
            assertTrue(codeValue >= 0 && codeValue <= 9999);
            assertEquals(4, code.length());
        }
    }

    @Test
    void testIsValidFormat_ValidCode() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator();
        String code = generator.generateCode();

        // When
        boolean isValid = generator.isValidFormat(code);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidFormat_NullCode() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator();

        // When
        boolean isValid = generator.isValidFormat(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidFormat_WrongLength() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("12345"); // Only 5 digits

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidFormat_NonNumeric() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("12a456");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidFormat_WithSpaces() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("123 456");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsValidFormat_WithLeadingZeros() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("000123");

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidFormat_AllZeros() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("000000");

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidFormat_AllNines() {
        // Given
        SmsOtpGenerator generator = new SmsOtpGenerator(6);

        // When
        boolean isValid = generator.isValidFormat("999999");

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsValidFormat_DifferentLengths() {
        // Given
        SmsOtpGenerator generator4 = new SmsOtpGenerator(4);
        SmsOtpGenerator generator6 = new SmsOtpGenerator(6);
        SmsOtpGenerator generator8 = new SmsOtpGenerator(8);

        // When/Then
        assertTrue(generator4.isValidFormat("1234"));
        assertFalse(generator4.isValidFormat("123456"));

        assertTrue(generator6.isValidFormat("123456"));
        assertFalse(generator6.isValidFormat("1234"));

        assertTrue(generator8.isValidFormat("12345678"));
        assertFalse(generator8.isValidFormat("123456"));
    }
}
