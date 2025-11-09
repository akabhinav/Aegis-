package io.aegis.mfa.email;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailConfiguration.
 */
class EmailConfigurationTest {

    @Test
    void testDefaultConfiguration() {
        // When
        EmailConfiguration config = EmailConfiguration.DEFAULT;

        // Then
        assertNotNull(config);
        assertEquals(6, config.codeLength());
        assertEquals(Duration.ofMinutes(10), config.validity());
        assertEquals(3, config.maxAttempts());
        assertEquals(5, config.rateLimitPerMinute());
        assertEquals("Your Verification Code", config.subject());
        assertEquals("Aegis Security", config.fromName());
        assertEquals(EmailConfiguration.TemplateType.HTML, config.templateType());
    }

    @Test
    void testBuilder_AllFields() {
        // When
        EmailConfiguration config = EmailConfiguration.builder()
                .codeLength(8)
                .validity(Duration.ofMinutes(15))
                .maxAttempts(5)
                .rateLimitPerMinute(3)
                .subject("Your OTP Code")
                .fromAddress("noreply@example.com")
                .fromName("Example App")
                .templateType(EmailConfiguration.TemplateType.PLAIN_TEXT)
                .build();

        // Then
        assertEquals(8, config.codeLength());
        assertEquals(Duration.ofMinutes(15), config.validity());
        assertEquals(5, config.maxAttempts());
        assertEquals(3, config.rateLimitPerMinute());
        assertEquals("Your OTP Code", config.subject());
        assertEquals("noreply@example.com", config.fromAddress());
        assertEquals("Example App", config.fromName());
        assertEquals(EmailConfiguration.TemplateType.PLAIN_TEXT, config.templateType());
    }

    @Test
    void testValidation_CodeLengthTooSmall() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().codeLength(3).build()
        );
    }

    @Test
    void testValidation_CodeLengthTooLarge() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().codeLength(9).build()
        );
    }

    @Test
    void testValidation_NullValidity() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().validity(null).build()
        );
    }

    @Test
    void testValidation_ZeroValidity() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().validity(Duration.ZERO).build()
        );
    }

    @Test
    void testValidation_NegativeValidity() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().validity(Duration.ofMinutes(-1)).build()
        );
    }

    @Test
    void testValidation_MaxAttemptsZero() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().maxAttempts(0).build()
        );
    }

    @Test
    void testValidation_RateLimitZero() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().rateLimitPerMinute(0).build()
        );
    }

    @Test
    void testValidation_NullSubject() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().subject(null).build()
        );
    }

    @Test
    void testValidation_BlankSubject() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().subject("   ").build()
        );
    }

    @Test
    void testValidation_NullTemplateType() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                EmailConfiguration.builder().templateType(null).build()
        );
    }

    @Test
    void testTemplateTypes() {
        // When/Then
        assertEquals(2, EmailConfiguration.TemplateType.values().length);
        assertNotNull(EmailConfiguration.TemplateType.PLAIN_TEXT);
        assertNotNull(EmailConfiguration.TemplateType.HTML);
    }
}
