package io.aegis.mfa.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS SNS SMS sender implementation.
 * Requires AWS credentials and region.
 *
 * @since 1.0.0
 */
public class AwsSnsSmsSender implements SmsSender {

    private static final Logger logger = LoggerFactory.getLogger(AwsSnsSmsSender.class);

    private final SnsClient snsClient;
    private final String senderName;

    /**
     * Creates an AWS SNS SMS sender with default credentials provider.
     *
     * @param region the AWS region
     * @param senderName the sender name (appears in SMS)
     */
    public AwsSnsSmsSender(Region region, String senderName) {
        if (region == null) {
            throw new IllegalArgumentException("Region cannot be null");
        }

        this.senderName = senderName;
        this.snsClient = SnsClient.builder()
                .region(region)
                .build();

        logger.info("AWS SNS SMS sender initialized for region: {}", region);
    }

    /**
     * Creates an AWS SNS SMS sender with explicit credentials.
     *
     * @param region the AWS region
     * @param accessKeyId AWS access key ID
     * @param secretAccessKey AWS secret access key
     * @param senderName the sender name (appears in SMS)
     */
    public AwsSnsSmsSender(Region region, String accessKeyId, String secretAccessKey, String senderName) {
        if (region == null) {
            throw new IllegalArgumentException("Region cannot be null");
        }
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalArgumentException("Access key ID cannot be null or blank");
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            throw new IllegalArgumentException("Secret access key cannot be null or blank");
        }

        this.senderName = senderName;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        this.snsClient = SnsClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();

        logger.info("AWS SNS SMS sender initialized for region: {}", region);
    }

    @Override
    public SmsResult send(String phoneNumber, String message) throws SmsSender.SmsException {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new SmsSender.SmsException("Phone number cannot be null or blank");
        }

        if (message == null || message.isBlank()) {
            throw new SmsSender.SmsException("Message cannot be null or blank");
        }

        try {
            logger.debug("Sending SMS to {} via AWS SNS", phoneNumber);

            // Set SMS attributes
            Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();

            // Set message type to Transactional for OTP (better delivery, higher cost)
            smsAttributes.put("AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                    .stringValue("Transactional")
                    .dataType("String")
                    .build());

            // Set sender name if provided
            if (senderName != null && !senderName.isBlank()) {
                smsAttributes.put("AWS.SNS.SMS.SenderID", MessageAttributeValue.builder()
                        .stringValue(senderName)
                        .dataType("String")
                        .build());
            }

            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(phoneNumber)
                    .messageAttributes(smsAttributes)
                    .build();

            PublishResponse response = snsClient.publish(request);
            String messageId = response.messageId();

            logger.info("SMS sent successfully via AWS SNS. Message ID: {}", messageId);

            return SmsResult.success(messageId);

        } catch (SnsException e) {
            logger.error("AWS SNS error: {}", e.awsErrorDetails().errorMessage(), e);
            throw new SmsSender.SmsException("AWS SNS error: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to send SMS via AWS SNS", e);
            throw new SmsSender.SmsException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the SNS client.
     */
    public void close() {
        if (snsClient != null) {
            snsClient.close();
            logger.info("AWS SNS client closed");
        }
    }

    public String getSenderName() {
        return senderName;
    }
}
