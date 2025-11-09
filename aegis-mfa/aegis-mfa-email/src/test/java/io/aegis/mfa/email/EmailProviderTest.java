package io.aegis.mfa.email;

import io.aegis.mfa.MfaChallenge;
import io.aegis.mfa.MfaMethod;
import io.aegis.mfa.MfaResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailProvider.
 */
@ExtendWith(MockitoExtension.class)
class EmailProviderTest {

    @Mock
    private EmailSender mockEmailSender;

    private EmailProvider provider;
    private EmailConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = EmailConfiguration.builder()
                .codeLength(6)
                .validity(Duration.ofMinutes(10))
                .maxAttempts(3)
                .rateLimitPerMinute(5)
                .subject("Your Code")
                .build();
        provider = new EmailProvider(configuration, mockEmailSender);
    }

    @Test
    void testConstructor_NullConfiguration() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new EmailProvider(null, mockEmailSender)
        );
    }

    @Test
    void testConstructor_NullEmailSender() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new EmailProvider(configuration, null)
        );
    }

    @Test
    void testSendChallenge_Success() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-123"));

        // When
        MfaChallenge challenge = provider.sendChallenge(userId, emailAddress);

        // Then
        assertNotNull(challenge);
        assertEquals(userId, challenge.userId());
        assertEquals(MfaMethod.EMAIL, challenge.method());
        assertNotNull(challenge.secret());
        assertEquals(6, challenge.secret().length());
        assertFalse(challenge.isExpired());
        assertEquals(emailAddress, challenge.metadata().get("emailAddress"));
        assertEquals("msg-123", challenge.metadata().get("messageId"));

        verify(mockEmailSender, times(1)).send(eq(emailAddress), eq("Your Code"), anyString(), eq(true));
    }

    @Test
    void testSendChallenge_WithRecipientName() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        String recipientName = "John Doe";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-456"));

        // When
        MfaChallenge challenge = provider.sendChallenge(userId, emailAddress, recipientName);

        // Then
        assertNotNull(challenge);
        verify(mockEmailSender).send(eq(emailAddress), anyString(), contains("John Doe"), anyBoolean());
    }

    @Test
    void testSendChallenge_NullUserId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.sendChallenge(null, "test@example.com")
        );
    }

    @Test
    void testSendChallenge_NullEmailAddress() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.sendChallenge("user123", null)
        );
    }

    @Test
    void testSendChallenge_EmailSenderFailure() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.failure("SMTP error"));

        // When/Then
        assertThrows(EmailSender.EmailException.class, () ->
                provider.sendChallenge(userId, emailAddress)
        );
    }

    @Test
    void testVerify_Success() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, emailAddress);
        String code = challenge.secret();

        // When
        MfaResult result = provider.verify(userId, emailAddress, code);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(userId, ((MfaResult.Success) result).userId());
    }

    @Test
    void testVerify_InvalidCode() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-123"));

        provider.sendChallenge(userId, emailAddress);

        // When
        MfaResult result = provider.verify(userId, emailAddress, "000000");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Invalid code", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_NoActiveChallenge() {
        // When
        MfaResult result = provider.verify("user123", "test@example.com", "123456");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_MaxAttemptsExceeded() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-123"));

        provider.sendChallenge(userId, emailAddress);

        // When - Try 4 times with wrong code (max is 3)
        provider.verify(userId, emailAddress, "000000");
        provider.verify(userId, emailAddress, "111111");
        provider.verify(userId, emailAddress, "222222");
        MfaResult result = provider.verify(userId, emailAddress, "333333");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testRateLimiting() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-123"));

        // When - Send 5 challenges (rate limit is 5 per minute)
        for (int i = 0; i < 5; i++) {
            provider.sendChallenge(userId, emailAddress);
        }

        // Then - 6th attempt should fail
        assertThrows(EmailSender.EmailException.class, () ->
                provider.sendChallenge(userId, emailAddress)
        );
    }

    @Test
    void testClearAll() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, emailAddress);
        String code = challenge.secret();

        // When
        provider.clearAll();
        MfaResult result = provider.verify(userId, emailAddress, code);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testRemoveChallenge() throws EmailSender.EmailException {
        // Given
        String userId = "user123";
        String emailAddress = "test@example.com";
        when(mockEmailSender.send(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(EmailSender.EmailResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, emailAddress);
        String code = challenge.secret();

        // When
        provider.removeChallenge(emailAddress);
        MfaResult result = provider.verify(userId, emailAddress, code);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testGetConfiguration() {
        // When
        EmailConfiguration config = provider.getConfiguration();

        // Then
        assertNotNull(config);
        assertEquals(configuration, config);
    }

    @Test
    void testGetOtpGenerator() {
        // When
        EmailOtpGenerator generator = provider.getOtpGenerator();

        // Then
        assertNotNull(generator);
        assertEquals(6, generator.getCodeLength());
    }

    @Test
    void testGetEmailTemplate() {
        // When
        EmailTemplate template = provider.getEmailTemplate();

        // Then
        assertNotNull(template);
        assertEquals(configuration, template.getConfiguration());
    }

    @Test
    void testDefaultConstructor() {
        // When
        EmailProvider defaultProvider = new EmailProvider(mockEmailSender);

        // Then
        assertNotNull(defaultProvider);
        assertEquals(EmailConfiguration.DEFAULT, defaultProvider.getConfiguration());
    }
}
