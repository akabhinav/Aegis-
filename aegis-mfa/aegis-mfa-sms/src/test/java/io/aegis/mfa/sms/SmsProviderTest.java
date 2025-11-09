package io.aegis.mfa.sms;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SmsProvider.
 */
@ExtendWith(MockitoExtension.class)
class SmsProviderTest {

    @Mock
    private SmsSender mockSmsSender;

    private SmsProvider provider;
    private SmsConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = SmsConfiguration.builder()
                .codeLength(6)
                .validity(Duration.ofMinutes(5))
                .maxAttempts(3)
                .rateLimitPerMinute(3)
                .messageTemplate("Your code is: {code}")
                .build();
        provider = new SmsProvider(configuration, mockSmsSender);
    }

    @Test
    void testConstructor_NullConfiguration() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new SmsProvider(null, mockSmsSender)
        );
    }

    @Test
    void testConstructor_NullSmsSender() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new SmsProvider(configuration, null)
        );
    }

    @Test
    void testSendChallenge_Success() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        // When
        MfaChallenge challenge = provider.sendChallenge(userId, phoneNumber);

        // Then
        assertNotNull(challenge);
        assertEquals(userId, challenge.userId());
        assertEquals(MfaMethod.SMS, challenge.method());
        assertNotNull(challenge.secret());
        assertEquals(6, challenge.secret().length());
        assertFalse(challenge.isExpired());
        assertEquals(phoneNumber, challenge.metadata().get("phoneNumber"));
        assertEquals("msg-123", challenge.metadata().get("messageId"));

        verify(mockSmsSender, times(1)).send(eq(phoneNumber), contains("Your code is:"));
    }

    @Test
    void testSendChallenge_NullUserId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.sendChallenge(null, "+1234567890")
        );
    }

    @Test
    void testSendChallenge_BlankUserId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.sendChallenge("   ", "+1234567890")
        );
    }

    @Test
    void testSendChallenge_NullPhoneNumber() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.sendChallenge("user123", null)
        );
    }

    @Test
    void testSendChallenge_BlankPhoneNumber() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                provider.sendChallenge("user123", "   ")
        );
    }

    @Test
    void testSendChallenge_SmsSenderFailure() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.failure("Network error"));

        // When/Then
        assertThrows(SmsSender.SmsException.class, () ->
                provider.sendChallenge(userId, phoneNumber)
        );
    }

    @Test
    void testVerify_Success() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, phoneNumber);
        String code = challenge.secret();

        // When
        MfaResult result = provider.verify(userId, phoneNumber, code);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(userId, ((MfaResult.Success) result).userId());
    }

    @Test
    void testVerify_InvalidCode() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        provider.sendChallenge(userId, phoneNumber);

        // When
        MfaResult result = provider.verify(userId, phoneNumber, "000000");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Invalid code", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_NullUserId() {
        // When
        MfaResult result = provider.verify(null, "+1234567890", "123456");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("User ID cannot be null or blank", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_NullPhoneNumber() {
        // When
        MfaResult result = provider.verify("user123", null, "123456");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Phone number cannot be null or blank", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_NullCode() {
        // When
        MfaResult result = provider.verify("user123", "+1234567890", null);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Code cannot be null or blank", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_BlankCode() {
        // When
        MfaResult result = provider.verify("user123", "+1234567890", "   ");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Code cannot be null or blank", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_InvalidCodeFormat() {
        // When
        MfaResult result = provider.verify("user123", "+1234567890", "12345"); // Only 5 digits

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Invalid code format", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_NoActiveChallenge() {
        // When
        MfaResult result = provider.verify("user123", "+1234567890", "123456");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_MaxAttemptsExceeded() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        provider.sendChallenge(userId, phoneNumber);

        // When - Try 4 times with wrong code (max is 3)
        provider.verify(userId, phoneNumber, "000000");
        provider.verify(userId, phoneNumber, "111111");
        provider.verify(userId, phoneNumber, "222222");
        MfaResult result = provider.verify(userId, phoneNumber, "333333");

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testVerify_ChallengeClearedAfterSuccess() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, phoneNumber);
        String code = challenge.secret();

        // When
        MfaResult result1 = provider.verify(userId, phoneNumber, code);
        MfaResult result2 = provider.verify(userId, phoneNumber, code); // Try again with same code

        // Then
        assertTrue(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result2).reason());
    }

    @Test
    void testRateLimiting() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        // When - Send 3 challenges (rate limit is 3 per minute)
        provider.sendChallenge(userId, phoneNumber);
        provider.sendChallenge(userId, phoneNumber);
        provider.sendChallenge(userId, phoneNumber);

        // Then - 4th attempt should fail
        assertThrows(SmsSender.SmsException.class, () ->
                provider.sendChallenge(userId, phoneNumber)
        );
    }

    @Test
    void testClearAll() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, phoneNumber);
        String code = challenge.secret();

        // When
        provider.clearAll();
        MfaResult result = provider.verify(userId, phoneNumber, code);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testRemoveChallenge() throws SmsSender.SmsException {
        // Given
        String userId = "user123";
        String phoneNumber = "+1234567890";
        when(mockSmsSender.send(anyString(), anyString()))
                .thenReturn(SmsSender.SmsResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, phoneNumber);
        String code = challenge.secret();

        // When
        provider.removeChallenge(phoneNumber);
        MfaResult result = provider.verify(userId, phoneNumber, code);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("No active challenge found", ((MfaResult.Failure) result).reason());
    }

    @Test
    void testGetConfiguration() {
        // When
        SmsConfiguration config = provider.getConfiguration();

        // Then
        assertNotNull(config);
        assertEquals(configuration, config);
    }

    @Test
    void testGetOtpGenerator() {
        // When
        SmsOtpGenerator generator = provider.getOtpGenerator();

        // Then
        assertNotNull(generator);
        assertEquals(6, generator.getCodeLength());
    }

    @Test
    void testDefaultConstructor() {
        // When
        SmsProvider defaultProvider = new SmsProvider(mockSmsSender);

        // Then
        assertNotNull(defaultProvider);
        assertEquals(SmsConfiguration.DEFAULT, defaultProvider.getConfiguration());
    }
}
