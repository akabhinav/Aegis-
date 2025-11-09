package io.aegis.mfa.email;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailTemplate.
 */
class EmailTemplateTest {

    @Test
    void testGeneratePlainTextBody() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .validity(Duration.ofMinutes(10))
                .fromName("Test App")
                .templateType(EmailConfiguration.TemplateType.PLAIN_TEXT)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generatePlainTextBody("123456", "John Doe");

        // Then
        assertNotNull(body);
        assertTrue(body.contains("Hello John Doe,"));
        assertTrue(body.contains("123456"));
        assertTrue(body.contains("10 minutes"));
        assertTrue(body.contains("Test App"));
    }

    @Test
    void testGeneratePlainTextBody_NoRecipientName() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .templateType(EmailConfiguration.TemplateType.PLAIN_TEXT)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generatePlainTextBody("987654", null);

        // Then
        assertNotNull(body);
        assertTrue(body.contains("Hello,"));
        assertTrue(body.contains("987654"));
    }

    @Test
    void testGenerateHtmlBody() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .validity(Duration.ofMinutes(5))
                .fromName("My App")
                .templateType(EmailConfiguration.TemplateType.HTML)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generateHtmlBody("456789", "Jane Smith");

        // Then
        assertNotNull(body);
        assertTrue(body.contains("<!DOCTYPE html"));
        assertTrue(body.contains("Hello Jane Smith,"));
        assertTrue(body.contains("456789"));
        assertTrue(body.contains("5 minutes"));
        assertTrue(body.contains("My App"));
        assertTrue(body.contains("<html"));
        assertTrue(body.contains("</html>"));
    }

    @Test
    void testGenerateHtmlBody_NoRecipientName() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .templateType(EmailConfiguration.TemplateType.HTML)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generateHtmlBody("111111", null);

        // Then
        assertNotNull(body);
        assertTrue(body.contains("Hello,"));
        assertTrue(body.contains("111111"));
    }

    @Test
    void testGenerateBody_PlainText() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .templateType(EmailConfiguration.TemplateType.PLAIN_TEXT)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generateBody("123456", "Test User");

        // Then
        assertNotNull(body);
        assertFalse(body.contains("<html"));
        assertTrue(body.contains("123456"));
    }

    @Test
    void testGenerateBody_Html() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .templateType(EmailConfiguration.TemplateType.HTML)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generateBody("654321", "Test User");

        // Then
        assertNotNull(body);
        assertTrue(body.contains("<html"));
        assertTrue(body.contains("654321"));
    }

    @Test
    void testHtmlBodyContainsSecurityNote() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .templateType(EmailConfiguration.TemplateType.HTML)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generateHtmlBody("123456", "User");

        // Then
        assertTrue(body.contains("Security Note"));
        assertTrue(body.contains("didn't request"));
    }

    @Test
    void testPlainTextBodyContainsSecurityNote() {
        // Given
        EmailConfiguration config = EmailConfiguration.builder()
                .templateType(EmailConfiguration.TemplateType.PLAIN_TEXT)
                .build();
        EmailTemplate template = new EmailTemplate(config);

        // When
        String body = template.generatePlainTextBody("123456", "User");

        // Then
        assertTrue(body.contains("didn't request"));
    }
}
