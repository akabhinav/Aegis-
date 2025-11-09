package io.aegis.mfa.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates QR codes for TOTP enrollment.
 * QR codes can be scanned by authenticator apps like Google Authenticator, Authy, etc.
 *
 * @since 1.0.0
 */
public class QRCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeGenerator.class);

    private final int width;
    private final int height;
    private final ErrorCorrectionLevel errorCorrectionLevel;

    /**
     * Creates a QR code generator with default size (300x300) and medium error correction.
     */
    public QRCodeGenerator() {
        this(300, 300, ErrorCorrectionLevel.M);
    }

    /**
     * Creates a QR code generator with custom settings.
     *
     * @param width the width of the QR code in pixels
     * @param height the height of the QR code in pixels
     * @param errorCorrectionLevel the error correction level
     */
    public QRCodeGenerator(int width, int height, ErrorCorrectionLevel errorCorrectionLevel) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.errorCorrectionLevel = errorCorrectionLevel;
    }

    /**
     * Generates a QR code image for the provisioning URI.
     *
     * @param provisioningUri the TOTP provisioning URI (otpauth://...)
     * @return BufferedImage containing the QR code
     * @throws QRCodeGenerationException if QR code generation fails
     */
    public BufferedImage generateQRCode(String provisioningUri) throws QRCodeGenerationException {
        if (provisioningUri == null || provisioningUri.isBlank()) {
            throw new IllegalArgumentException("Provisioning URI cannot be null or blank");
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    provisioningUri,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
            );

            logger.debug("Generated QR code for provisioning URI");
            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (WriterException e) {
            logger.error("Failed to generate QR code", e);
            throw new QRCodeGenerationException("Failed to generate QR code", e);
        }
    }

    /**
     * Generates a QR code and returns it as a Base64-encoded PNG image.
     * Useful for web applications to embed in HTML.
     *
     * @param provisioningUri the TOTP provisioning URI
     * @return Base64-encoded PNG image data
     * @throws QRCodeGenerationException if QR code generation fails
     */
    public String generateQRCodeBase64(String provisioningUri) throws QRCodeGenerationException {
        BufferedImage qrImage = generateQRCode(provisioningUri);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(qrImage, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            logger.error("Failed to encode QR code as Base64", e);
            throw new QRCodeGenerationException("Failed to encode QR code as Base64", e);
        }
    }

    /**
     * Generates a QR code and returns it as a data URL for direct HTML embedding.
     * Format: data:image/png;base64,{base64Data}
     *
     * @param provisioningUri the TOTP provisioning URI
     * @return data URL containing the QR code
     * @throws QRCodeGenerationException if QR code generation fails
     */
    public String generateQRCodeDataUrl(String provisioningUri) throws QRCodeGenerationException {
        String base64Image = generateQRCodeBase64(provisioningUri);
        return "data:image/png;base64," + base64Image;
    }

    /**
     * Generates a QR code and saves it to a file.
     *
     * @param provisioningUri the TOTP provisioning URI
     * @param filePath the path where to save the QR code image
     * @throws QRCodeGenerationException if QR code generation or file saving fails
     */
    public void generateQRCodeToFile(String provisioningUri, String filePath) throws QRCodeGenerationException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be null or blank");
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    provisioningUri,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
            );

            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

            logger.info("QR code saved to file: {}", filePath);

        } catch (WriterException | IOException e) {
            logger.error("Failed to save QR code to file: {}", filePath, e);
            throw new QRCodeGenerationException("Failed to save QR code to file", e);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ErrorCorrectionLevel getErrorCorrectionLevel() {
        return errorCorrectionLevel;
    }

    /**
     * Exception thrown when QR code generation fails.
     */
    public static class QRCodeGenerationException extends Exception {
        public QRCodeGenerationException(String message) {
            super(message);
        }

        public QRCodeGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
