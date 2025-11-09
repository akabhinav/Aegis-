package io.aegis.mfa.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Twilio SMS sender implementation.
 * Requires Twilio account SID and auth token.
 *
 * @since 1.0.0
 */
public class TwilioSmsSender implements SmsSender {

    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;
    private final boolean initialized;

    /**
     * Creates a Twilio SMS sender.
     *
     * @param accountSid Twilio account SID
     * @param authToken Twilio auth token
     * @param fromPhoneNumber the sender phone number (E.164 format)
     */
    public TwilioSmsSender(String accountSid, String authToken, String fromPhoneNumber) {
        if (accountSid == null || accountSid.isBlank()) {
            throw new IllegalArgumentException("Account SID cannot be null or blank");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("Auth token cannot be null or blank");
        }
        if (fromPhoneNumber == null || fromPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("From phone number cannot be null or blank");
        }

        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhoneNumber = fromPhoneNumber;

        try {
            Twilio.init(accountSid, authToken);
            this.initialized = true;
            logger.info("Twilio SMS sender initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Twilio", e);
            this.initialized = false;
            throw new IllegalStateException("Failed to initialize Twilio", e);
        }
    }

    @Override
    public SmsResult send(String phoneNumber, String message) throws SmsException {
        if (!initialized) {
            throw new SmsException("Twilio is not initialized");
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new SmsException("Phone number cannot be null or blank");
        }

        if (message == null || message.isBlank()) {
            throw new SmsException("Message cannot be null or blank");
        }

        try {
            logger.debug("Sending SMS to {} via Twilio", phoneNumber);

            Message twilioMessage = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    message
            ).create();

            String messageSid = twilioMessage.getSid();
            logger.info("SMS sent successfully via Twilio. Message SID: {}", messageSid);

            return SmsResult.success(messageSid);

        } catch (com.twilio.exception.ApiException e) {
            logger.error("Twilio API error: {}", e.getMessage(), e);
            throw new SmsException("Twilio API error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to send SMS via Twilio", e);
            throw new SmsException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    public String getFromPhoneNumber() {
        return fromPhoneNumber;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
