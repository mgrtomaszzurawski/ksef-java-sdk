/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

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
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 * Render an arbitrary verification URL into a QR-code PNG.
 *
 * <p>Verification-URL construction is a separate responsibility — see
 * {@link KsefVerificationLinks} for KOD I (online) and KOD II (offline-certificate)
 * URL builders. This service only renders any string into a QR PNG; it does not
 * know about KSeF URL structure.
 */
public final class QrCodeService {

    private static final String IMAGE_FORMAT_PNG = "PNG";
    private static final int DEFAULT_QR_SIZE = 250;
    private static final int MAX_QR_SIZE = 4096;
    private static final int QR_MARGIN = 1;
    private static final String ERR_PAYLOAD_NULL = "payloadUrl must not be null or empty";
    private static final String ERR_SIZE_POSITIVE = "size must be positive";
    private static final String ERR_SIZE_TOO_LARGE = "size must not exceed " + MAX_QR_SIZE;
    private static final String ERR_QR_GENERATION = "Failed to generate QR code";

    /**
     * Render a payload URL into a 250x250 QR-code PNG.
     *
     * @param payloadUrl URL to encode (typically built via {@link KsefVerificationLinks})
     * @return PNG image bytes
     */
    public byte[] generateQrCode(String payloadUrl) {
        return generateQrCode(payloadUrl, DEFAULT_QR_SIZE);
    }

    /**
     * Render a payload URL into a QR-code PNG of the requested size.
     *
     * @param payloadUrl URL to encode (typically built via {@link KsefVerificationLinks})
     * @param size width and height of the QR code in pixels (positive, &le; {@value #MAX_QR_SIZE})
     * @return PNG image bytes
     */
    public byte[] generateQrCode(String payloadUrl, int size) {
        Objects.requireNonNull(payloadUrl, ERR_PAYLOAD_NULL);
        if (payloadUrl.isEmpty()) {
            throw new IllegalArgumentException(ERR_PAYLOAD_NULL);
        }
        if (size <= 0) {
            throw new IllegalArgumentException(ERR_SIZE_POSITIVE);
        }
        if (size > MAX_QR_SIZE) {
            throw new IllegalArgumentException(ERR_SIZE_TOO_LARGE);
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, QR_MARGIN
            );
            BitMatrix bitMatrix = writer.encode(payloadUrl, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, IMAGE_FORMAT_PNG, outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException(ERR_QR_GENERATION, exception);
        }
    }
}
