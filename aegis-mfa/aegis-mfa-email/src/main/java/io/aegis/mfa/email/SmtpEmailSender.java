package io.aegis.mfa.email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

/**
 * SMTP email sender implementation using JavaMail.
 *
 * @since 1.0.0
 */
public class SmtpEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String fromName;
    private final boolean useTls;
    private final boolean useSsl;
    private final Session session;

    /**
     * Creates an SMTP email sender with TLS.
     *
     * @param host SMTP server host
     * @param port SMTP server port
     * @param username SMTP username
     * @param password SMTP password
     * @param fromAddress sender email address
     * @param fromName sender name
     */
    public SmtpEmailSender(String host, int port, String username, String password,
                           String fromAddress, String fromName) {
        this(host, port, username, password, fromAddress, fromName, true, false);
    }

    /**
     * Creates an SMTP email sender with custom security settings.
     *
     * @param host SMTP server host
     * @param port SMTP server port
     * @param username SMTP username
     * @param password SMTP password
     * @param fromAddress sender email address
     * @param fromName sender name
     * @param useTls whether to use STARTTLS
     * @param useSsl whether to use SSL
     */
    public SmtpEmailSender(String host, int port, String username, String password,
                           String fromAddress, String fromName, boolean useTls, boolean useSsl) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be null or blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalArgumentException("From address cannot be null or blank");
        }

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.useTls = useTls;
        this.useSsl = useSsl;

        // Configure SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", username != null && !username.isBlank());
        props.put("mail.smtp.starttls.enable", useTls);
        props.put("mail.smtp.ssl.enable", useSsl);

        // Trust all certificates (for development - should be configured properly in production)
        if (useTls || useSsl) {
            props.put("mail.smtp.ssl.trust", "*");
        }

        // Create session
        if (username != null && !username.isBlank()) {
            this.session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            this.session = Session.getInstance(props);
        }

        logger.info("SMTP email sender initialized for host: {} on port: {}", host, port);
    }

    @Override
    public EmailResult send(String toAddress, String subject, String body, boolean isHtml) throws EmailException {
        if (toAddress == null || toAddress.isBlank()) {
            throw new EmailException("To address cannot be null or blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new EmailException("Subject cannot be null or blank");
        }
        if (body == null || body.isBlank()) {
            throw new EmailException("Body cannot be null or blank");
        }

        try {
            logger.debug("Sending email to {} via SMTP", toAddress);

            // Create message
            MimeMessage message = new MimeMessage(session);

            // Set from address
            if (fromName != null && !fromName.isBlank()) {
                message.setFrom(new InternetAddress(fromAddress, fromName));
            } else {
                message.setFrom(new InternetAddress(fromAddress));
            }

            // Set to address
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));

            // Set subject
            message.setSubject(subject);

            // Set content
            if (isHtml) {
                message.setContent(body, "text/html; charset=utf-8");
            } else {
                message.setText(body, "utf-8");
            }

            // Send message
            Transport.send(message);

            // Generate a message ID
            String messageId = UUID.randomUUID().toString();

            logger.info("Email sent successfully via SMTP to: {}. Message ID: {}", toAddress, messageId);

            return EmailResult.success(messageId);

        } catch (MessagingException e) {
            logger.error("Failed to send email via SMTP: {}", e.getMessage(), e);
            throw new EmailException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending email", e);
            throw new EmailException("Unexpected error: " + e.getMessage(), e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public boolean isUseSsl() {
        return useSsl;
    }
}
