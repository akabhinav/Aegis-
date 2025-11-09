package io.aegis.mfa.totp;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QRCodeGenerator.
 */
class QRCodeGeneratorTest {

    private QRCodeGenerator generator;
    private String validProvisioningUri;

    @BeforeEach
    void setUp() {
        generator = new QRCodeGenerator();
        validProvisioningUri = "otpauth://totp/Test:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Test&algorithm=SHA1&digits=6&period=30";
    }

    @Test
    void testDefaultConstructor() {
        // When
        QRCodeGenerator defaultGenerator = new QRCodeGenerator();

        // Then
        assertNotNull(defaultGenerator);
        assertEquals(300, defaultGenerator.getWidth());
        assertEquals(300, defaultGenerator.getHeight());
        assertEquals(ErrorCorrectionLevel.M, defaultGenerator.getErrorCorrectionLevel());
    }

    @Test
    void testCustomConstructor() {
        // When
        QRCodeGenerator customGenerator = new QRCodeGenerator(400, 400, ErrorCorrectionLevel.H);

        // Then
        assertEquals(400, customGenerator.getWidth());
        assertEquals(400, customGenerator.getHeight());
        assertEquals(ErrorCorrectionLevel.H, customGenerator.getErrorCorrectionLevel());
    }

    @Test
    void testConstructor_InvalidWidth() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new QRCodeGenerator(0, 300, ErrorCorrectionLevel.M)
        );
    }

    @Test
    void testConstructor_InvalidHeight() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                new QRCodeGenerator(300, -100, ErrorCorrectionLevel.M)
        );
    }

    @Test
    void testGenerateQRCode() throws QRCodeGenerator.QRCodeGenerationException {
        // When
        BufferedImage qrImage = generator.generateQRCode(validProvisioningUri);

        // Then
        assertNotNull(qrImage);
        assertEquals(300, qrImage.getWidth());
        assertEquals(300, qrImage.getHeight());
    }

    @Test
    void testGenerateQRCode_NullUri() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateQRCode(null)
        );
    }

    @Test
    void testGenerateQRCode_BlankUri() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateQRCode("   ")
        );
    }

    @Test
    void testGenerateQRCodeBase64() throws QRCodeGenerator.QRCodeGenerationException {
        // When
        String base64Image = generator.generateQRCodeBase64(validProvisioningUri);

        // Then
        assertNotNull(base64Image);
        assertFalse(base64Image.isBlank());
        // Base64 string should only contain valid Base64 characters
        assertTrue(base64Image.matches("^[A-Za-z0-9+/]*={0,2}$"));
    }

    @Test
    void testGenerateQRCodeDataUrl() throws QRCodeGenerator.QRCodeGenerationException {
        // When
        String dataUrl = generator.generateQRCodeDataUrl(validProvisioningUri);

        // Then
        assertNotNull(dataUrl);
        assertTrue(dataUrl.startsWith("data:image/png;base64,"));
        assertTrue(dataUrl.length() > 30); // Should contain substantial data
    }

    @Test
    void testGenerateQRCodeToFile(@TempDir Path tempDir) throws QRCodeGenerator.QRCodeGenerationException {
        // Given
        Path qrCodePath = tempDir.resolve("test-qr.png");

        // When
        generator.generateQRCodeToFile(validProvisioningUri, qrCodePath.toString());

        // Then
        assertTrue(qrCodePath.toFile().exists());
        assertTrue(qrCodePath.toFile().length() > 0);
    }

    @Test
    void testGenerateQRCodeToFile_NullPath() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateQRCodeToFile(validProvisioningUri, null)
        );
    }

    @Test
    void testGenerateQRCodeToFile_BlankPath() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateQRCodeToFile(validProvisioningUri, "   ")
        );
    }

    @Test
    void testGenerateQRCode_DifferentSizes() throws QRCodeGenerator.QRCodeGenerationException {
        // Given
        QRCodeGenerator smallGenerator = new QRCodeGenerator(200, 200, ErrorCorrectionLevel.L);
        QRCodeGenerator largeGenerator = new QRCodeGenerator(500, 500, ErrorCorrectionLevel.H);

        // When
        BufferedImage smallQR = smallGenerator.generateQRCode(validProvisioningUri);
        BufferedImage largeQR = largeGenerator.generateQRCode(validProvisioningUri);

        // Then
        assertEquals(200, smallQR.getWidth());
        assertEquals(200, smallQR.getHeight());
        assertEquals(500, largeQR.getWidth());
        assertEquals(500, largeQR.getHeight());
    }

    @Test
    void testGenerateQRCode_LongUri() throws QRCodeGenerator.QRCodeGenerationException {
        // Given
        String longUri = "otpauth://totp/VeryLongIssuerNameWithManyCharacters:very.long.email.address@subdomain.example.com?secret=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP&issuer=VeryLongIssuerNameWithManyCharacters&algorithm=SHA512&digits=8&period=60";

        // When
        BufferedImage qrImage = generator.generateQRCode(longUri);

        // Then
        assertNotNull(qrImage);
        assertEquals(300, qrImage.getWidth());
    }

    @Test
    void testGenerateQRCode_SpecialCharactersInUri() throws QRCodeGenerator.QRCodeGenerationException {
        // Given
        String uriWithSpecialChars = "otpauth://totp/Test%20App:user%2Btest%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=Test%20App";

        // When
        BufferedImage qrImage = generator.generateQRCode(uriWithSpecialChars);

        // Then
        assertNotNull(qrImage);
    }

    @Test
    void testGenerateMultipleQRCodes_AreDifferent() throws QRCodeGenerator.QRCodeGenerationException {
        // Given
        String uri1 = "otpauth://totp/App1:user1@example.com?secret=JBSWY3DPEHPK3PXP&issuer=App1";
        String uri2 = "otpauth://totp/App2:user2@example.com?secret=ABCDEFGHIJKLMNOP&issuer=App2";

        // When
        String base64_1 = generator.generateQRCodeBase64(uri1);
        String base64_2 = generator.generateQRCodeBase64(uri2);

        // Then
        assertNotEquals(base64_1, base64_2);
    }

    @Test
    void testErrorCorrectionLevels() throws QRCodeGenerator.QRCodeGenerationException {
        // Given
        QRCodeGenerator generatorL = new QRCodeGenerator(300, 300, ErrorCorrectionLevel.L);
        QRCodeGenerator generatorM = new QRCodeGenerator(300, 300, ErrorCorrectionLevel.M);
        QRCodeGenerator generatorQ = new QRCodeGenerator(300, 300, ErrorCorrectionLevel.Q);
        QRCodeGenerator generatorH = new QRCodeGenerator(300, 300, ErrorCorrectionLevel.H);

        // When
        BufferedImage qrL = generatorL.generateQRCode(validProvisioningUri);
        BufferedImage qrM = generatorM.generateQRCode(validProvisioningUri);
        BufferedImage qrQ = generatorQ.generateQRCode(validProvisioningUri);
        BufferedImage qrH = generatorH.generateQRCode(validProvisioningUri);

        // Then - All should generate successfully
        assertNotNull(qrL);
        assertNotNull(qrM);
        assertNotNull(qrQ);
        assertNotNull(qrH);
    }
}
