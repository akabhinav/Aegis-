package io.aegis.mfa.email;

/**
 * Email template generator for OTP emails.
 * Provides both plain text and HTML templates.
 *
 * @since 1.0.0
 */
public class EmailTemplate {

    private final EmailConfiguration configuration;

    public EmailTemplate(EmailConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Generates an email body with the OTP code.
     *
     * @param code the OTP code
     * @param recipientName optional recipient name
     * @return the email body (HTML or plain text based on configuration)
     */
    public String generateBody(String code, String recipientName) {
        return configuration.templateType() == EmailConfiguration.TemplateType.HTML
                ? generateHtmlBody(code, recipientName)
                : generatePlainTextBody(code, recipientName);
    }

    /**
     * Generates a plain text email body.
     *
     * @param code the OTP code
     * @param recipientName optional recipient name
     * @return plain text email body
     */
    public String generatePlainTextBody(String code, String recipientName) {
        String greeting = recipientName != null && !recipientName.isBlank()
                ? "Hello " + recipientName + ","
                : "Hello,";

        long validityMinutes = configuration.validity().toMinutes();

        return String.format("""
                %s

                Your verification code is:

                %s

                This code will expire in %d minutes.

                If you didn't request this code, please ignore this email.

                Best regards,
                %s
                """,
                greeting,
                code,
                validityMinutes,
                configuration.fromName());
    }

    /**
     * Generates an HTML email body.
     *
     * @param code the OTP code
     * @param recipientName optional recipient name
     * @return HTML email body
     */
    public String generateHtmlBody(String code, String recipientName) {
        String greeting = recipientName != null && !recipientName.isBlank()
                ? "Hello " + recipientName + ","
                : "Hello,";

        long validityMinutes = configuration.validity().toMinutes();

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Verification Code</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background-color: #f9f9f9;
                            border-radius: 8px;
                            padding: 30px;
                            margin: 20px 0;
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .code-container {
                            background-color: #fff;
                            border: 2px solid #4CAF50;
                            border-radius: 8px;
                            padding: 20px;
                            text-align: center;
                            margin: 30px 0;
                        }
                        .code {
                            font-size: 32px;
                            font-weight: bold;
                            letter-spacing: 8px;
                            color: #4CAF50;
                            font-family: 'Courier New', monospace;
                        }
                        .expiry {
                            color: #666;
                            font-size: 14px;
                            margin-top: 15px;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin-top: 30px;
                            border-radius: 4px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                            color: #666;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>%s</h2>
                        </div>

                        <p>We received a request to verify your account. Please use the following verification code:</p>

                        <div class="code-container">
                            <div class="code">%s</div>
                            <div class="expiry">This code will expire in %d minutes</div>
                        </div>

                        <p>Enter this code to complete your verification.</p>

                        <div class="warning">
                            <strong>⚠️ Security Note:</strong> If you didn't request this code, please ignore this email. Your account is still secure.
                        </div>

                        <div class="footer">
                            Best regards,<br>
                            <strong>%s</strong>
                        </div>
                    </div>
                </body>
                </html>
                """,
                greeting,
                code,
                validityMinutes,
                configuration.fromName());
    }

    public EmailConfiguration getConfiguration() {
        return configuration;
    }
}
