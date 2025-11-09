package io.aegis.mfa.push;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PushConfiguration.
 */
class PushConfigurationTest {

    @Test
    void defaultConfiguration() {
        PushConfiguration config = PushConfiguration.DEFAULT;

        assertNotNull(config);
        assertEquals(Duration.ofMinutes(2), config.approvalTimeout());
        assertEquals(3, config.maxPendingChallenges());
        assertEquals(5, config.rateLimitPerMinute());
        assertEquals("Login Request", config.title());
        assertNotNull(config.body());
        assertFalse(config.requireBiometric());
        assertFalse(config.allowAutoApprove());
    }

    @Test
    void builderWithDefaults() {
        PushConfiguration config = PushConfiguration.builder().build();

        assertNotNull(config);
        assertEquals(PushConfiguration.DEFAULT.approvalTimeout(), config.approvalTimeout());
        assertEquals(PushConfiguration.DEFAULT.maxPendingChallenges(), config.maxPendingChallenges());
        assertEquals(PushConfiguration.DEFAULT.rateLimitPerMinute(), config.rateLimitPerMinute());
    }

    @Test
    void builderWithCustomValues() {
        Duration customTimeout = Duration.ofMinutes(5);
        String customTitle = "Custom Title";
        String customBody = "Custom body with {location}";

        PushConfiguration config = PushConfiguration.builder()
                .approvalTimeout(customTimeout)
                .maxPendingChallenges(10)
                .rateLimitPerMinute(3)
                .title(customTitle)
                .body(customBody)
                .requireBiometric(true)
                .allowAutoApprove(true)
                .build();

        assertEquals(customTimeout, config.approvalTimeout());
        assertEquals(10, config.maxPendingChallenges());
        assertEquals(3, config.rateLimitPerMinute());
        assertEquals(customTitle, config.title());
        assertEquals(customBody, config.body());
        assertTrue(config.requireBiometric());
        assertTrue(config.allowAutoApprove());
    }

    @Test
    void formatBodyWithAllParameters() {
        PushConfiguration config = PushConfiguration.builder()
                .body("Login from {location} ({ip}) on {device}")
                .build();

        String formatted = config.formatBody("San Francisco, CA", "192.168.1.1", "iPhone 15 Pro");

        assertEquals("Login from San Francisco, CA (192.168.1.1) on iPhone 15 Pro", formatted);
    }

    @Test
    void formatBodyWithNullParameters() {
        PushConfiguration config = PushConfiguration.builder()
                .body("Login from {location} ({ip}) on {device}")
                .build();

        String formatted = config.formatBody(null, null, null);

        assertEquals("Login from Unknown location (Unknown IP) on Unknown device", formatted);
    }

    @Test
    void formatBodyWithPartialParameters() {
        PushConfiguration config = PushConfiguration.builder()
                .body("Login from {location} on {device}")
                .build();

        String formatted = config.formatBody("New York", null, "Android Phone");

        assertEquals("Login from New York on Android Phone", formatted);
    }

    @Test
    void validationNullApprovalTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .approvalTimeout(null)
                        .build()
        );
    }

    @Test
    void validationNegativeApprovalTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .approvalTimeout(Duration.ofMinutes(-1))
                        .build()
        );
    }

    @Test
    void validationZeroApprovalTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .approvalTimeout(Duration.ZERO)
                        .build()
        );
    }

    @Test
    void validationZeroMaxPendingChallenges() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .maxPendingChallenges(0)
                        .build()
        );
    }

    @Test
    void validationNegativeMaxPendingChallenges() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .maxPendingChallenges(-1)
                        .build()
        );
    }

    @Test
    void validationZeroRateLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .rateLimitPerMinute(0)
                        .build()
        );
    }

    @Test
    void validationNegativeRateLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .rateLimitPerMinute(-1)
                        .build()
        );
    }

    @Test
    void validationNullTitle() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .title(null)
                        .build()
        );
    }

    @Test
    void validationBlankTitle() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .title("   ")
                        .build()
        );
    }

    @Test
    void validationNullBody() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .body(null)
                        .build()
        );
    }

    @Test
    void validationBlankBody() {
        assertThrows(IllegalArgumentException.class, () ->
                PushConfiguration.builder()
                        .body("   ")
                        .build()
        );
    }

    @Test
    void builderChaining() {
        PushConfiguration config = PushConfiguration.builder()
                .approvalTimeout(Duration.ofMinutes(3))
                .maxPendingChallenges(5)
                .rateLimitPerMinute(10)
                .title("Test")
                .body("Test body")
                .requireBiometric(true)
                .allowAutoApprove(false)
                .build();

        assertNotNull(config);
        assertEquals(Duration.ofMinutes(3), config.approvalTimeout());
        assertEquals(5, config.maxPendingChallenges());
        assertEquals(10, config.rateLimitPerMinute());
        assertTrue(config.requireBiometric());
        assertFalse(config.allowAutoApprove());
    }

    @Test
    void recordEquality() {
        PushConfiguration config1 = PushConfiguration.builder()
                .approvalTimeout(Duration.ofMinutes(2))
                .title("Test")
                .body("Body")
                .build();

        PushConfiguration config2 = PushConfiguration.builder()
                .approvalTimeout(Duration.ofMinutes(2))
                .title("Test")
                .body("Body")
                .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }
}
