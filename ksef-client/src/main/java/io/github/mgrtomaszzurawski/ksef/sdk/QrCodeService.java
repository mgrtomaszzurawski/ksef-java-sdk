/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Service for generating KSeF invoice verification QR codes.
 *
 * <p>KSeF assigns a unique number to each invoice. This service generates a verification
 * URL and QR code image that can be printed on the invoice to allow recipients to
 * verify the invoice in the KSeF system.</p>
 */
public final class QrCodeService {

    private static final String VERIFICATION_URL_PREFIX = "https://ksef.mf.gov.pl/web/verify/";
    private static final String TEST_VERIFICATION_URL_PREFIX = "https://ksef-test.mf.gov.pl/web/verify/";
    private static final String IMAGE_FORMAT_PNG = "PNG";
    private static final int DEFAULT_QR_SIZE = 250;
    private static final String ERR_KSEF_NUMBER_EMPTY = "ksefNumber must not be null or empty";
    private static final String ERR_SIZE_POSITIVE = "size must be positive";
    private static final String ERR_QR_GENERATION = "Failed to generate QR code";

    private final boolean testEnvironment;

    /**
     * Create a QR code service for the production environment.
     */
    public QrCodeService() {
        this(false);
    }

    /**
     * Create a QR code service for the specified environment.
     *
     * @param testEnvironment true for test environment URLs, false for production
     */
    public QrCodeService(boolean testEnvironment) {
        this.testEnvironment = testEnvironment;
    }

    /**
     * Generate the KSeF verification URL for an invoice.
     *
     * @param ksefNumber the unique KSeF invoice number
     * @return the verification URL
     */
    public String getVerificationUrl(String ksefNumber) {
        requireNonEmpty(ksefNumber);
        String prefix = testEnvironment ? TEST_VERIFICATION_URL_PREFIX : VERIFICATION_URL_PREFIX;
        return prefix + ksefNumber;
    }

    /**
     * Generate a QR code image as PNG bytes for the KSeF verification URL.
     * Uses the default size of 250x250 pixels.
     *
     * @param ksefNumber the unique KSeF invoice number
     * @return PNG image bytes
     */
    public byte[] generateQrCode(String ksefNumber) {
        return generateQrCode(ksefNumber, DEFAULT_QR_SIZE);
    }

    /**
     * Generate a QR code image as PNG bytes for the KSeF verification URL.
     *
     * @param ksefNumber the unique KSeF invoice number
     * @param size the width and height of the QR code in pixels
     * @return PNG image bytes
     */
    public byte[] generateQrCode(String ksefNumber, int size) {
        requireNonEmpty(ksefNumber);
        if (size <= 0) {
            throw new IllegalArgumentException(ERR_SIZE_POSITIVE);
        }

        String verificationUrl = getVerificationUrl(ksefNumber);
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix bitMatrix = writer.encode(verificationUrl, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, IMAGE_FORMAT_PNG, outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException(ERR_QR_GENERATION, exception);
        }
    }

    private static void requireNonEmpty(String ksefNumber) {
        if (ksefNumber == null || ksefNumber.isEmpty()) {
            throw new IllegalArgumentException(ERR_KSEF_NUMBER_EMPTY);
        }
    }
}
