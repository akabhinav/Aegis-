package io.aegis.mfa.sms;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SmsConfiguration.
 */
class SmsConfigurationTest {

    @Test
    void testDefaultConfiguration() {
        // When
        SmsConfiguration config = SmsConfiguration.DEFAULT;

        // Then
        assertNotNull(config);
        assertEquals(6, config.codeLength());
        assertEquals(Duration.ofMinutes(5), config.validity());
        assertEquals(3, config.maxAttempts());
        assertEquals(3, config.rateLimitPerMinute());
        assertTrue(config.messageTemplate().contains("{code}"));
        assertEquals(SmsConfiguration.SenderType.TWILIO, config.senderType());
    }

    @Test
    void testBuilder_AllFields() {
        // When
        SmsConfiguration config = SmsConfiguration.builder()
                .codeLength(8)
                .validity(Duration.ofMinutes(10))
                .maxAttempts(5)
                .rateLimitPerMinute(5)
                .messageTemplate("Your OTP: {code}")
                .fromPhoneNumber("+1234567890")
                .senderType(SmsConfiguration.SenderType.AWS_SNS)
                .build();

        // Then
        assertEquals(8, config.codeLength());
        assertEquals(Duration.ofMinutes(10), config.validity());
        assertEquals(5, config.maxAttempts());
        assertEquals(5, config.rateLimitPerMinute());
        assertEquals("Your OTP: {code}", config.messageTemplate());
        assertEquals("+1234567890", config.fromPhoneNumber());
        assertEquals(SmsConfiguration.SenderType.AWS_SNS, config.senderType());
    }

    @Test
    void testBuilder_DefaultValues() {
        // When
        SmsConfiguration config = SmsConfiguration.builder().build();

        // Then
        assertEquals(6, config.codeLength());
        assertEquals(Duration.ofMinutes(5), config.validity());
        assertEquals(3, config.maxAttempts());
        assertEquals(3, config.rateLimitPerMinute());
    }

    @Test
    void testValidation_CodeLengthTooSmall() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().codeLength(3).build()
        );
    }

    @Test
    void testValidation_CodeLengthTooLarge() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().codeLength(9).build()
        );
    }

    @Test
    void testValidation_CodeLength_Min() {
        // When/Then
        assertDoesNotThrow(() ->
                SmsConfiguration.builder().codeLength(4).build()
        );
    }

    @Test
    void testValidation_CodeLength_Max() {
        // When/Then
        assertDoesNotThrow(() ->
                SmsConfiguration.builder().codeLength(8).build()
        );
    }

    @Test
    void testValidation_NullValidity() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().validity(null).build()
        );
    }

    @Test
    void testValidation_ZeroValidity() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().validity(Duration.ZERO).build()
        );
    }

    @Test
    void testValidation_NegativeValidity() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().validity(Duration.ofMinutes(-1)).build()
        );
    }

    @Test
    void testValidation_MaxAttemptsZero() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().maxAttempts(0).build()
        );
    }

    @Test
    void testValidation_MaxAttemptsNegative() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().maxAttempts(-1).build()
        );
    }

    @Test
    void testValidation_RateLimitZero() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().rateLimitPerMinute(0).build()
        );
    }

    @Test
    void testValidation_RateLimitNegative() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().rateLimitPerMinute(-1).build()
        );
    }

    @Test
    void testValidation_NullMessageTemplate() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().messageTemplate(null).build()
        );
    }

    @Test
    void testValidation_BlankMessageTemplate() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().messageTemplate("   ").build()
        );
    }

    @Test
    void testValidation_MessageTemplateWithoutPlaceholder() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder()
                        .messageTemplate("Your verification code is ready")
                        .build()
        );
    }

    @Test
    void testValidation_MessageTemplateWithPlaceholder() {
        // When/Then
        assertDoesNotThrow(() ->
                SmsConfiguration.builder()
                        .messageTemplate("Your code: {code}")
                        .build()
        );
    }

    @Test
    void testValidation_NullSenderType() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                SmsConfiguration.builder().senderType(null).build()
        );
    }

    @Test
    void testFormatMessage() {
        // Given
        SmsConfiguration config = SmsConfiguration.builder()
                .messageTemplate("Your OTP is {code}. Valid for 5 min.")
                .build();

        // When
        String formatted = config.formatMessage("123456");

        // Then
        assertEquals("Your OTP is 123456. Valid for 5 min.", formatted);
    }

    @Test
    void testFormatMessage_MultiplePlaceholders() {
        // Given
        SmsConfiguration config = SmsConfiguration.builder()
                .messageTemplate("Code: {code}. Use {code} to verify.")
                .build();

        // When
        String formatted = config.formatMessage("789012");

        // Then
        assertEquals("Code: 789012. Use 789012 to verify.", formatted);
    }

    @Test
    void testSenderTypes() {
        // When/Then
        assertEquals(3, SmsConfiguration.SenderType.values().length);
        assertNotNull(SmsConfiguration.SenderType.TWILIO);
        assertNotNull(SmsConfiguration.SenderType.AWS_SNS);
        assertNotNull(SmsConfiguration.SenderType.CUSTOM);
    }

    @Test
    void testBuilder_FluentInterface() {
        // When
        SmsConfiguration config = SmsConfiguration.builder()
                .codeLength(6)
                .validity(Duration.ofMinutes(3))
                .maxAttempts(4)
                .rateLimitPerMinute(2)
                .messageTemplate("Code: {code}")
                .fromPhoneNumber("+1234567890")
                .senderType(SmsConfiguration.SenderType.CUSTOM)
                .build();

        // Then
        assertEquals(6, config.codeLength());
        assertEquals(Duration.ofMinutes(3), config.validity());
        assertEquals(4, config.maxAttempts());
        assertEquals(2, config.rateLimitPerMinute());
        assertEquals("Code: {code}", config.messageTemplate());
        assertEquals("+1234567890", config.fromPhoneNumber());
        assertEquals(SmsConfiguration.SenderType.CUSTOM, config.senderType());
    }
}
