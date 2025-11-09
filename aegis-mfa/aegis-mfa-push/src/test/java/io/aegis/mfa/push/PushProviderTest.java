package io.aegis.mfa.push;

import io.aegis.mfa.core.MfaChallenge;
import io.aegis.mfa.core.MfaMethod;
import io.aegis.mfa.core.MfaResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PushProvider.
 */
class PushProviderTest {

    private PushNotificationSender mockSender;
    private PushProvider provider;
    private PushConfiguration configuration;

    @BeforeEach
    void setUp() {
        mockSender = mock(PushNotificationSender.class);
        configuration = PushConfiguration.DEFAULT;
        provider = new PushProvider(configuration, mockSender);
    }

    @Test
    void getMethod() {
        assertEquals(MfaMethod.PUSH, provider.getMethod());
    }

    @Test
    void sendChallengeSuccess() throws Exception {
        String userId = "user123";
        String deviceToken = "device-token-abc";
        String messageId = "msg-123";

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success(messageId));

        MfaChallenge challenge = provider.sendChallenge(userId, deviceToken, "San Francisco", "192.168.1.1", "iPhone");

        assertNotNull(challenge);
        assertEquals(MfaMethod.PUSH, challenge.method());
        assertNotNull(challenge.challengeId());
        assertNotNull(challenge.expiresAt());
        assertEquals(messageId, challenge.metadata().get("message_id"));

        // Verify notification was sent
        verify(mockSender).send(eq(deviceToken), eq(configuration.title()), anyString(), anyMap());
    }

    @Test
    void sendChallengeWithNullOptionalParameters() throws Exception {
        String userId = "user123";
        String deviceToken = "device-token-abc";

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge(userId, deviceToken, null, null, null);

        assertNotNull(challenge);
        verify(mockSender).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void sendChallengeIncludesContextInData() throws Exception {
        String userId = "user123";
        String deviceToken = "device-token-abc";
        String location = "San Francisco, CA";
        String ipAddress = "192.168.1.1";
        String deviceInfo = "iPhone 15 Pro";

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        when(mockSender.send(anyString(), anyString(), anyString(), dataCaptor.capture()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        provider.sendChallenge(userId, deviceToken, location, ipAddress, deviceInfo);

        Map<String, String> capturedData = dataCaptor.getValue();
        assertNotNull(capturedData.get("challenge_id"));
        assertEquals(userId, capturedData.get("user_id"));
        assertEquals(location, capturedData.get("location"));
        assertEquals(ipAddress, capturedData.get("ip_address"));
        assertEquals(deviceInfo, capturedData.get("device_info"));
        assertEquals("mfa_approval", capturedData.get("type"));
    }

    @Test
    void sendChallengeNullUserId() {
        assertThrows(PushNotificationSender.PushException.class, () ->
                provider.sendChallenge(null, "device-token", null, null, null)
        );
    }

    @Test
    void sendChallengeBlankUserId() {
        assertThrows(PushNotificationSender.PushException.class, () ->
                provider.sendChallenge("   ", "device-token", null, null, null)
        );
    }

    @Test
    void sendChallengeNullDeviceToken() {
        assertThrows(PushNotificationSender.PushException.class, () ->
                provider.sendChallenge("user123", null, null, null, null)
        );
    }

    @Test
    void sendChallengeBlankDeviceToken() {
        assertThrows(PushNotificationSender.PushException.class, () ->
                provider.sendChallenge("user123", "   ", null, null, null)
        );
    }

    @Test
    void sendChallengeFailsWhenSenderFails() throws Exception {
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.failure("Network error"));

        assertThrows(PushNotificationSender.PushException.class, () ->
                provider.sendChallenge("user123", "device-token", null, null, null)
        );
    }

    @Test
    void approveSuccess() throws Exception {
        // Send challenge first
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge("user123", "device-token", null, null, null);
        String challengeId = challenge.challengeId();

        // Approve the challenge
        MfaResult result = provider.approve(challengeId, false);

        assertTrue(result.isSuccess());
        assertTrue(result instanceof MfaResult.Success);
        MfaResult.Success success = (MfaResult.Success) result;
        assertEquals("user123", success.userId());
        assertNotNull(success.authenticatedAt());
        assertEquals(challengeId, success.metadata().get("challenge_id"));
    }

    @Test
    void approveWithBiometric() throws Exception {
        PushConfiguration config = PushConfiguration.builder()
                .requireBiometric(true)
                .build();
        PushProvider providerWithBiometric = new PushProvider(config, mockSender);

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = providerWithBiometric.sendChallenge("user123", "device-token", null, null, null);

        // Approve with biometric
        MfaResult result = providerWithBiometric.approve(challenge.challengeId(), true);

        assertTrue(result.isSuccess());
        MfaResult.Success success = (MfaResult.Success) result;
        assertEquals(true, success.metadata().get("biometric_verified"));
    }

    @Test
    void approveFailsWhenBiometricRequiredButNotProvided() throws Exception {
        PushConfiguration config = PushConfiguration.builder()
                .requireBiometric(true)
                .build();
        PushProvider providerWithBiometric = new PushProvider(config, mockSender);

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = providerWithBiometric.sendChallenge("user123", "device-token", null, null, null);

        // Try to approve without biometric
        MfaResult result = providerWithBiometric.approve(challenge.challengeId(), false);

        assertFalse(result.isSuccess());
        assertTrue(result instanceof MfaResult.Failure);
    }

    @Test
    void approveNonExistentChallenge() {
        MfaResult result = provider.approve("non-existent-challenge", false);

        assertFalse(result.isSuccess());
        assertTrue(result instanceof MfaResult.Failure);
    }

    @Test
    void approveNullChallengeId() {
        MfaResult result = provider.approve(null, false);

        assertFalse(result.isSuccess());
    }

    @Test
    void approveBlankChallengeId() {
        MfaResult result = provider.approve("   ", false);

        assertFalse(result.isSuccess());
    }

    @Test
    void denySuccess() throws Exception {
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge("user123", "device-token", null, null, null);

        MfaResult result = provider.deny(challenge.challengeId());

        assertFalse(result.isSuccess());
        assertTrue(result instanceof MfaResult.Failure);
        MfaResult.Failure failure = (MfaResult.Failure) result;
        assertTrue(failure.reason().contains("denied"));
    }

    @Test
    void denyNonExistentChallenge() {
        MfaResult result = provider.deny("non-existent-challenge");

        assertFalse(result.isSuccess());
    }

    @Test
    void getChallengeStatusPending() throws Exception {
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge("user123", "device-token", null, null, null);

        Optional<PushProvider.ChallengeStatus> status = provider.getChallengeStatus(challenge.challengeId());

        assertTrue(status.isPresent());
        assertEquals(PushProvider.ChallengeStatus.PENDING, status.get());
    }

    @Test
    void getChallengeStatusNonExistent() {
        Optional<PushProvider.ChallengeStatus> status = provider.getChallengeStatus("non-existent");

        assertFalse(status.isPresent());
    }

    @Test
    void getPendingChallenges() throws Exception {
        String userId = "user123";

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        // Send multiple challenges
        MfaChallenge challenge1 = provider.sendChallenge(userId, "device-1", null, null, null);
        MfaChallenge challenge2 = provider.sendChallenge(userId, "device-2", null, null, null);
        provider.sendChallenge("other-user", "device-3", null, null, null);

        List<String> pendingChallenges = provider.getPendingChallenges(userId);

        assertEquals(2, pendingChallenges.size());
        assertTrue(pendingChallenges.contains(challenge1.challengeId()));
        assertTrue(pendingChallenges.contains(challenge2.challengeId()));
    }

    @Test
    void cancelChallenge() throws Exception {
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge("user123", "device-token", null, null, null);

        boolean cancelled = provider.cancelChallenge(challenge.challengeId());

        assertTrue(cancelled);

        // Verify it's no longer pending
        Optional<PushProvider.ChallengeStatus> status = provider.getChallengeStatus(challenge.challengeId());
        assertFalse(status.isPresent());
    }

    @Test
    void cancelNonExistentChallenge() {
        boolean cancelled = provider.cancelChallenge("non-existent");

        assertFalse(cancelled);
    }

    @Test
    void rateLimiting() throws Exception {
        String deviceToken = "device-token";

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        // Send up to the rate limit (default is 5 per minute)
        for (int i = 0; i < 5; i++) {
            provider.sendChallenge("user" + i, deviceToken, null, null, null);
        }

        // Next one should fail due to rate limit
        assertThrows(PushNotificationSender.PushException.class, () ->
                provider.sendChallenge("user6", deviceToken, null, null, null)
        );
    }

    @Test
    void maxPendingChallenges() throws Exception {
        String userId = "user123";

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        // Send up to the max pending (default is 3)
        for (int i = 0; i < 3; i++) {
            provider.sendChallenge(userId, "device-" + i, null, null, null);
        }

        // Next one should fail due to max pending
        assertThrows(PushNotificationSender.PushException.class, () ->
                provider.sendChallenge(userId, "device-4", null, null, null)
        );
    }

    @Test
    void cleanup() throws Exception {
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        provider.sendChallenge("user123", "device-token", null, null, null);

        assertEquals(1, provider.getPendingChallengeCount());

        provider.cleanup();

        // Non-expired challenges should still be there
        assertEquals(1, provider.getPendingChallengeCount());
    }

    @Test
    void constructorNullConfiguration() {
        assertThrows(IllegalArgumentException.class, () ->
                new PushProvider(null, mockSender)
        );
    }

    @Test
    void constructorNullSender() {
        assertThrows(IllegalArgumentException.class, () ->
                new PushProvider(configuration, null)
        );
    }

    @Test
    void getConfiguration() {
        assertEquals(configuration, provider.getConfiguration());
    }

    @Test
    void getPendingChallengeCount() throws Exception {
        assertEquals(0, provider.getPendingChallengeCount());

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        provider.sendChallenge("user123", "device-1", null, null, null);
        assertEquals(1, provider.getPendingChallengeCount());

        provider.sendChallenge("user456", "device-2", null, null, null);
        assertEquals(2, provider.getPendingChallengeCount());
    }

    @Test
    void challengeRemovedAfterApproval() throws Exception {
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge("user123", "device-token", null, null, null);
        assertEquals(1, provider.getPendingChallengeCount());

        provider.approve(challenge.challengeId(), false);

        assertEquals(0, provider.getPendingChallengeCount());
    }

    @Test
    void challengeRemovedAfterDenial() throws Exception {
        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge("user123", "device-token", null, null, null);
        assertEquals(1, provider.getPendingChallengeCount());

        provider.deny(challenge.challengeId());

        assertEquals(0, provider.getPendingChallengeCount());
    }

    @Test
    void approvalIncludesContextMetadata() throws Exception {
        String location = "San Francisco, CA";
        String ipAddress = "192.168.1.1";
        String deviceInfo = "iPhone 15";

        when(mockSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationSender.PushResult.success("msg-123"));

        MfaChallenge challenge = provider.sendChallenge("user123", "device-token", location, ipAddress, deviceInfo);

        MfaResult result = provider.approve(challenge.challengeId(), false);

        assertTrue(result.isSuccess());
        MfaResult.Success success = (MfaResult.Success) result;
        assertEquals(location, success.metadata().get("location"));
        assertEquals(ipAddress, success.metadata().get("ip_address"));
        assertEquals(deviceInfo, success.metadata().get("device_info"));
    }

    @Test
    void customConfiguration() {
        PushConfiguration customConfig = PushConfiguration.builder()
                .approvalTimeout(Duration.ofMinutes(5))
                .maxPendingChallenges(10)
                .rateLimitPerMinute(20)
                .build();

        PushProvider customProvider = new PushProvider(customConfig, mockSender);

        assertEquals(customConfig, customProvider.getConfiguration());
    }
}
