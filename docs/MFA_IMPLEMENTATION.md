# Multi-Factor Authentication (MFA) Implementation Guide

## Overview

Multi-Factor Authentication adds an extra layer of security by requiring users to provide two or more verification factors:
- **Something you know** - Password
- **Something you have** - Phone, security token, authenticator app
- **Something you are** - Biometric (fingerprint, face)

## Supported MFA Methods

1. **TOTP** (Time-based One-Time Password) - Google Authenticator, Authy
2. **SMS OTP** - Text message codes
3. **Email OTP** - Email verification codes
4. **Push Notifications** - Approve login on mobile app
5. **Backup Codes** - Single-use recovery codes
6. **Hardware Tokens** - YubiKey, RSA SecurID

## Architecture

```
┌────────────────┐
│  Primary Auth  │
│  (Password)    │
└───────┬────────┘
        │
        ↓ Success
┌────────────────────┐
│  MFA Required?     │
└───────┬────────────┘
        │ Yes
        ↓
┌────────────────────┐
│  MFA Challenge     │
│  - TOTP            │
│  - SMS             │
│  - Email           │
│  - Push            │
└───────┬────────────┘
        │
        ↓ Verify
┌────────────────────┐
│  Grant Access      │
└────────────────────┘
```

## Module Structure

```
aegis-mfa/
├── aegis-mfa-core/
│   └── src/main/java/io/aegis/mfa/
│       ├── MfaProvider.java
│       ├── MfaChallenge.java
│       ├── MfaConfiguration.java
│       └── MfaResult.java
│
├── aegis-mfa-totp/
│   └── src/main/java/io/aegis/mfa/totp/
│       ├── TotpProvider.java
│       ├── TotpConfiguration.java
│       ├── TotpSecret.java
│       └── QRCodeGenerator.java
│
├── aegis-mfa-sms/
│   └── src/main/java/io/aegis/mfa/sms/
│       ├── SmsProvider.java
│       ├── SmsConfiguration.java
│       └── SmsGateway.java
│
├── aegis-mfa-email/
│   └── src/main/java/io/aegis/mfa/email/
│       ├── EmailProvider.java
│       └── EmailConfiguration.java
│
└── aegis-mfa-push/
    └── src/main/java/io/aegis/mfa/push/
        ├── PushProvider.java
        └── PushNotificationService.java
```

## 1. TOTP Implementation

### Dependencies

```xml
<!-- aegis-mfa-totp/pom.xml -->
<dependencies>
    <dependency>
        <groupId>io.aegis</groupId>
        <artifactId>aegis-mfa-core</artifactId>
    </dependency>

    <!-- TOTP library -->
    <dependency>
        <groupId>dev.samstevens.totp</groupId>
        <artifactId>totp</artifactId>
        <version>1.7.1</version>
    </dependency>

    <!-- QR Code generation -->
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>core</artifactId>
        <version>3.5.2</version>
    </dependency>
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>javase</artifactId>
        <version>3.5.2</version>
    </dependency>
</dependencies>
```

### TOTP Provider

```java
package io.aegis.mfa.totp;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.*;
import dev.samstevens.totp.secret.*;
import dev.samstevens.totp.time.*;
import io.aegis.mfa.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TOTP (Time-based One-Time Password) MFA provider.
 * Compatible with Google Authenticator, Authy, Microsoft Authenticator, etc.
 */
public class TotpProvider implements MfaProvider {

    private static final Logger logger = LoggerFactory.getLogger(TotpProvider.class);

    private final TotpConfiguration configuration;
    private final SecretGenerator secretGenerator;
    private final CodeGenerator codeGenerator;
    private final CodeVerifier codeVerifier;
    private final QrDataFactory qrDataFactory;
    private final QrGenerator qrGenerator;

    public TotpProvider(TotpConfiguration configuration) {
        this.configuration = configuration;
        this.secretGenerator = new DefaultSecretGenerator();
        this.codeGenerator = new DefaultCodeGenerator(
                configuration.getHashAlgorithm(),
                configuration.getDigits()
        );
        this.codeVerifier = new DefaultCodeVerifier(
                codeGenerator,
                new SystemTimeProvider()
        );
        this.qrDataFactory = new QrDataFactory(
                configuration.getHashAlgorithm(),
                configuration.getDigits(),
                configuration.getPeriod()
        );
        this.qrGenerator = new ZxingPngQrGenerator();
    }

    /**
     * Generate a new secret for a user.
     */
    public TotpSecret generateSecret(String username) {
        String secret = secretGenerator.generate();

        logger.debug("Generated TOTP secret for user: {}", username);

        return new TotpSecret(
                secret,
                configuration.getIssuer(),
                username
        );
    }

    /**
     * Generate QR code data URL for secret enrollment.
     */
    public String generateQRCode(TotpSecret secret) throws MfaException {
        try {
            String data = qrDataFactory.newBuilder()
                    .label(secret.username())
                    .secret(secret.secret())
                    .issuer(secret.issuer())
                    .build()
                    .getUri();

            return qrGenerator.generate(data);

        } catch (QrGenerationException e) {
            throw new MfaException("Failed to generate QR code", e);
        }
    }

    /**
     * Verify a TOTP code.
     */
    @Override
    public MfaResult verify(MfaChallenge challenge, String code) {
        try {
            String secret = challenge.getSecret();

            // Verify with time window tolerance
            boolean valid = codeVerifier.isValidCode(
                    secret,
                    code,
                    configuration.getTimeWindow()
            );

            if (valid) {
                logger.info("TOTP verification successful for user: {}",
                        challenge.getUserId());
                return MfaResult.success(challenge.getUserId());
            } else {
                logger.warn("TOTP verification failed for user: {}",
                        challenge.getUserId());
                return MfaResult.failure("Invalid code");
            }

        } catch (Exception e) {
            logger.error("TOTP verification error", e);
            return MfaResult.failure("Verification error: " + e.getMessage());
        }
    }

    @Override
    public MfaChallenge createChallenge(String userId, Map<String, Object> metadata) {
        // TOTP doesn't need to create a challenge - the secret is stored
        String secret = (String) metadata.get("secret");

        return MfaChallenge.builder()
                .userId(userId)
                .method(MfaMethod.TOTP)
                .secret(secret)
                .expiresAt(Instant.now().plus(configuration.getCodeLifetime()))
                .build();
    }

    @Override
    public MfaMethod getMethod() {
        return MfaMethod.TOTP;
    }

    @Override
    public String getName() {
        return "TotpProvider";
    }
}
```

### Configuration

```java
package io.aegis.mfa.totp;

import dev.samstevens.totp.code.HashingAlgorithm;
import java.time.Duration;

public class TotpConfiguration {

    private String issuer = "Aegis";
    private HashingAlgorithm hashAlgorithm = HashingAlgorithm.SHA1;
    private int digits = 6;
    private int period = 30; // seconds
    private int timeWindow = 1; // Allow 1 period before/after
    private Duration codeLifetime = Duration.ofSeconds(90);

    // Getters, setters, builder...

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TotpConfiguration config = new TotpConfiguration();

        public Builder issuer(String issuer) {
            config.issuer = issuer;
            return this;
        }

        public Builder hashAlgorithm(HashingAlgorithm algorithm) {
            config.hashAlgorithm = algorithm;
            return this;
        }

        public Builder digits(int digits) {
            config.digits = digits;
            return this;
        }

        public Builder period(int period) {
            config.period = period;
            return this;
        }

        public Builder timeWindow(int window) {
            config.timeWindow = window;
            return this;
        }

        public TotpConfiguration build() {
            return config;
        }
    }
}
```

### Usage Example

```java
// Setup
TotpConfiguration config = TotpConfiguration.builder()
        .issuer("MyApp")
        .digits(6)
        .period(30)
        .build();

TotpProvider totpProvider = new TotpProvider(config);

// Enrollment - Generate secret for user
TotpSecret secret = totpProvider.generateSecret("user@example.com");
String qrCodeUrl = totpProvider.generateQRCode(secret);

// Display QR code to user
// User scans with Google Authenticator

// Store secret in database
userRepository.setTotpSecret(userId, secret.secret());

// Later - Verify code during login
MfaChallenge challenge = MfaChallenge.builder()
        .userId(userId)
        .method(MfaMethod.TOTP)
        .secret(secret.secret())
        .build();

MfaResult result = totpProvider.verify(challenge, userEnteredCode);

if (result.isSuccess()) {
    // Grant access
}
```

## 2. SMS OTP Implementation

### SMS Provider

```java
package io.aegis.mfa.sms;

import io.aegis.mfa.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SMS-based OTP provider.
 */
public class SmsOtpProvider implements MfaProvider {

    private static final Logger logger = LoggerFactory.getLogger(SmsOtpProvider.class);

    private final SmsConfiguration configuration;
    private final SmsGateway smsGateway;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    public SmsOtpProvider(SmsConfiguration configuration, SmsGateway smsGateway) {
        this.configuration = configuration;
        this.smsGateway = smsGateway;
    }

    @Override
    public MfaChallenge createChallenge(String userId, Map<String, Object> metadata) {
        // Generate OTP
        String otp = generateOtp();

        // Get phone number
        String phoneNumber = (String) metadata.get("phoneNumber");
        if (phoneNumber == null) {
            throw new MfaException("Phone number required for SMS OTP");
        }

        // Send SMS
        sendSms(phoneNumber, otp);

        // Store OTP
        String key = userId + ":" + System.currentTimeMillis();
        otpStore.put(key, otp);

        // Schedule cleanup
        scheduleCleanup(key, configuration.getCodeLifetime());

        return MfaChallenge.builder()
                .userId(userId)
                .method(MfaMethod.SMS)
                .secret(key) // Use key to lookup OTP
                .expiresAt(Instant.now().plus(configuration.getCodeLifetime()))
                .metadata(Map.of("phoneNumber", phoneNumber))
                .build();
    }

    @Override
    public MfaResult verify(MfaChallenge challenge, String code) {
        String storedOtp = otpStore.get(challenge.getSecret());

        if (storedOtp == null) {
            logger.warn("OTP expired or not found for user: {}", challenge.getUserId());
            return MfaResult.failure("Code expired or invalid");
        }

        if (storedOtp.equals(code)) {
            // Remove used OTP
            otpStore.remove(challenge.getSecret());

            logger.info("SMS OTP verification successful for user: {}",
                    challenge.getUserId());
            return MfaResult.success(challenge.getUserId());
        } else {
            logger.warn("SMS OTP verification failed for user: {}",
                    challenge.getUserId());
            return MfaResult.failure("Invalid code");
        }
    }

    @Override
    public MfaMethod getMethod() {
        return MfaMethod.SMS;
    }

    private String generateOtp() {
        int digits = configuration.getCodeLength();
        int bound = (int) Math.pow(10, digits);
        int otp = random.nextInt(bound);
        return String.format("%0" + digits + "d", otp);
    }

    private void sendSms(String phoneNumber, String otp) {
        String message = String.format(
                configuration.getMessageTemplate(),
                otp,
                configuration.getCodeLifetime().toMinutes()
        );

        smsGateway.send(phoneNumber, message);
        logger.debug("Sent SMS OTP to: {}", maskPhoneNumber(phoneNumber));
    }

    private String maskPhoneNumber(String phone) {
        if (phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    private void scheduleCleanup(String key, Duration delay) {
        // Use scheduled executor in production
        new Thread(() -> {
            try {
                Thread.sleep(delay.toMillis());
                otpStore.remove(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

### SMS Gateway Interface

```java
package io.aegis.mfa.sms;

/**
 * Interface for SMS gateway implementations.
 */
public interface SmsGateway {

    /**
     * Send SMS to phone number.
     */
    void send(String phoneNumber, String message);

    /**
     * Twilio implementation
     */
    class TwilioGateway implements SmsGateway {
        private final String accountSid;
        private final String authToken;
        private final String fromNumber;

        // Implementation using Twilio SDK
    }

    /**
     * AWS SNS implementation
     */
    class AwsSnsGateway implements SmsGateway {
        // Implementation using AWS SDK
    }
}
```

## 3. Backup Codes

```java
package io.aegis.mfa.backup;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Backup codes generator for account recovery.
 */
public class BackupCodesGenerator {

    private static final int CODE_LENGTH = 8;
    private static final int CODE_COUNT = 10;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();

    /**
     * Generate backup codes for a user.
     */
    public List<BackupCode> generateCodes(String userId) {
        List<BackupCode> codes = new ArrayList<>();

        for (int i = 0; i < CODE_COUNT; i++) {
            String code = generateCode();
            codes.add(new BackupCode(
                    userId,
                    code,
                    false, // not used
                    Instant.now()
            ));
        }

        return codes;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARS.length());
            code.append(CHARS.charAt(index));

            // Add hyphen after 4 characters
            if (i == 3) {
                code.append('-');
            }
        }

        return code.toString();
    }

    /**
     * Verify and mark backup code as used.
     */
    public boolean verifyCode(String userId, String code,
                             BackupCodeRepository repository) {
        Optional<BackupCode> backupCode = repository.findByUserIdAndCode(userId, code);

        if (backupCode.isEmpty()) {
            return false;
        }

        BackupCode bc = backupCode.get();
        if (bc.isUsed()) {
            return false;
        }

        // Mark as used
        repository.markAsUsed(bc.getId());
        return true;
    }
}

record BackupCode(
    String userId,
    String code,
    boolean used,
    Instant createdAt
) {}
```

## 4. MFA Flow Integration

### Enhanced Authentication Manager

```java
public class MfaAwareAuthenticationManager {

    private final AuthenticationManager primaryAuthManager;
    private final MfaManager mfaManager;
    private final MfaRequirementPolicy mfaPolicy;

    public AuthenticationResult authenticate(
            AuthenticationContext primaryContext,
            MfaContext mfaContext
    ) {
        // Step 1: Primary authentication
        AuthenticationResult primaryResult =
                primaryAuthManager.authenticate(primaryContext);

        if (!primaryResult.isSuccess()) {
            return primaryResult;
        }

        UserPrincipal user = primaryResult.getPrincipal().get();

        // Step 2: Check if MFA is required
        if (!mfaPolicy.isMfaRequired(user)) {
            return primaryResult; // No MFA needed
        }

        // Step 3: MFA challenge
        if (mfaContext == null || mfaContext.getCode() == null) {
            return AuthenticationResult.failure("MFA required", Map.of(
                    "mfaRequired", true,
                    "availableMethods", mfaManager.getEnabledMethods(user.getId())
            ));
        }

        // Step 4: Verify MFA
        MfaResult mfaResult = mfaManager.verify(
                user.getId(),
                mfaContext.getMethod(),
                mfaContext.getCode()
        );

        if (mfaResult.isSuccess()) {
            return AuthenticationResult.success(user, Map.of(
                    "mfaVerified", true,
                    "mfaMethod", mfaContext.getMethod()
            ));
        } else {
            return AuthenticationResult.failure("MFA verification failed");
        }
    }
}
```

## Configuration

```yaml
aegis:
  mfa:
    enabled: true

    # TOTP
    totp:
      enabled: true
      issuer: MyApp
      digits: 6
      period: 30
      time-window: 1

    # SMS
    sms:
      enabled: true
      gateway: twilio
      code-length: 6
      code-lifetime: PT5M
      message-template: "Your verification code is: %s (valid for %d minutes)"
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
        from-number: ${TWILIO_PHONE_NUMBER}

    # Email
    email:
      enabled: true
      code-length: 6
      code-lifetime: PT10M
      subject: "Your verification code"
      template: "email-otp-template"

    # Backup codes
    backup-codes:
      enabled: true
      count: 10
      length: 8

    # Policies
    policy:
      require-for-admin: true
      require-for-sensitive-ops: true
      remember-device: true
      remember-duration: P30D
```

## Frontend Integration

```javascript
// MFA enrollment
async function enrollTotp() {
    // Get secret and QR code
    const response = await fetch('/api/mfa/totp/enroll', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    });

    const { secret, qrCode } = await response.json();

    // Display QR code
    document.getElementById('qrCode').src = qrCode;

    // Verify setup
    const code = prompt('Enter code from authenticator app:');
    await verifyTotpSetup(secret, code);
}

async function verifyTotpSetup(secret, code) {
    const response = await fetch('/api/mfa/totp/verify-setup', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ secret, code })
    });

    if (response.ok) {
        alert('TOTP enabled successfully!');
    }
}

// MFA during login
async function loginWithMfa(username, password) {
    // Primary authentication
    const authResponse = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });

    const authResult = await authResponse.json();

    // Check if MFA required
    if (authResult.mfaRequired) {
        const method = prompt('Select MFA method: ' +
            authResult.availableMethods.join(', '));

        const code = prompt('Enter verification code:');

        // Verify MFA
        const mfaResponse = await fetch('/api/auth/mfa/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: authResult.sessionId,
                method: method,
                code: code
            })
        });

        return mfaResponse.json();
    }

    return authResult;
}
```

## Testing

```java
@Test
void testTotpFlow() {
    // Setup
    TotpConfiguration config = TotpConfiguration.builder()
            .issuer("TestApp")
            .build();

    TotpProvider provider = new TotpProvider(config);

    // Generate secret
    TotpSecret secret = provider.generateSecret("test@example.com");

    // Generate code (simulate authenticator app)
    CodeGenerator codeGen = new DefaultCodeGenerator();
    String code = codeGen.generate(secret.secret(), System.currentTimeMillis() / 1000 / 30);

    // Verify
    MfaChallenge challenge = MfaChallenge.builder()
            .userId("user-123")
            .method(MfaMethod.TOTP)
            .secret(secret.secret())
            .build();

    MfaResult result = provider.verify(challenge, code);

    assertTrue(result.isSuccess());
}
```

## Best Practices

1. **Rate Limiting**: Limit MFA attempts (3-5 per user per hour)
2. **Backup Codes**: Always provide backup codes
3. **Recovery**: Implement account recovery flow
4. **Device Memory**: Allow "remember this device"
5. **Step-up Auth**: Require MFA for sensitive operations
6. **Multiple Methods**: Allow users to choose preferred method
7. **Graceful Degradation**: SMS fallback if TOTP not available
8. **Audit Trail**: Log all MFA events
9. **Time Sync**: Handle clock skew for TOTP
10. **User Education**: Provide clear setup instructions
