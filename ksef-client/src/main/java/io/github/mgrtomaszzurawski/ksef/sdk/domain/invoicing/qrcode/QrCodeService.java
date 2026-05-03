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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
    private static final int LABEL_PADDING_PX = 6;
    private static final int LABEL_FONT_SIZE = 14;
    private static final String LABEL_FONT_FAMILY = "SansSerif";
    /** Official KOD I label rendered when the KSeF number is unknown (offline mode). */
    public static final String LABEL_OFFLINE = "OFFLINE";
    /** Official KOD II label rendered below the offline-certificate verification QR. */
    public static final String LABEL_CERTIFICATE = "CERTYFIKAT";
    private static final String ERR_PAYLOAD_NULL = "payloadUrl must not be null or empty";
    private static final String ERR_LABEL_NULL = "label must not be null or empty";
    private static final String ERR_QR_PNG_NULL = "qrPng must not be null";
    private static final String ERR_SIZE_POSITIVE = "size must be positive";
    private static final String ERR_SIZE_TOO_LARGE = "size must not exceed " + MAX_QR_SIZE;
    private static final String ERR_QR_GENERATION = "Failed to generate QR code";
    private static final String ERR_QR_LABEL_RENDER = "Failed to render label below QR code";

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

    /**
     * Render an existing QR PNG with a text label drawn below the code, per
     * the official KSeF invoice-visualization examples.
     *
     * <p>Use {@link #LABEL_OFFLINE} for KOD I when the KSeF number is not yet
     * assigned, the actual KSeF number for KOD I once known, or
     * {@link #LABEL_CERTIFICATE} for KOD II.
     *
     * @param qrPng raw PNG bytes returned by {@link #generateQrCode}
     * @param label label text rendered below the code
     * @return PNG bytes of the QR plus label panel
     */
    public byte[] addLabelToQrCode(byte[] qrPng, String label) {
        Objects.requireNonNull(qrPng, ERR_QR_PNG_NULL);
        Objects.requireNonNull(label, ERR_LABEL_NULL);
        if (label.isEmpty()) {
            throw new IllegalArgumentException(ERR_LABEL_NULL);
        }
        try {
            BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrPng));
            if (qrImage == null) {
                throw new IllegalStateException(ERR_QR_LABEL_RENDER);
            }
            BufferedImage labelled = drawLabelBelow(qrImage, label);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(labelled, IMAGE_FORMAT_PNG, output);
            return output.toByteArray();
        } catch (IOException ioFailure) {
            throw new IllegalStateException(ERR_QR_LABEL_RENDER, ioFailure);
        }
    }

    /**
     * Convenience: render a QR PNG for {@code payloadUrl} and append the
     * supplied {@code label} below it.
     */
    public byte[] generateLabeledQrCode(String payloadUrl, String label) {
        return addLabelToQrCode(generateQrCode(payloadUrl), label);
    }

    private static BufferedImage drawLabelBelow(BufferedImage qrImage, String label) {
        Font font = new Font(LABEL_FONT_FAMILY, Font.BOLD, LABEL_FONT_SIZE);
        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D measure = measureImage.createGraphics();
        measure.setFont(font);
        FontMetrics metrics = measure.getFontMetrics();
        int labelHeight = metrics.getHeight() + LABEL_PADDING_PX * 2;
        measure.dispose();

        BufferedImage canvas = new BufferedImage(qrImage.getWidth(), qrImage.getHeight() + labelHeight,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.drawImage(qrImage, 0, 0, null);
        graphics.setColor(Color.BLACK);
        graphics.setFont(font);
        FontMetrics finalMetrics = graphics.getFontMetrics();
        int labelWidth = finalMetrics.stringWidth(label);
        int textX = (canvas.getWidth() - labelWidth) / 2;
        int textY = qrImage.getHeight() + LABEL_PADDING_PX + finalMetrics.getAscent();
        graphics.drawString(label, textX, textY);
        graphics.dispose();
        return canvas;
    }
}
