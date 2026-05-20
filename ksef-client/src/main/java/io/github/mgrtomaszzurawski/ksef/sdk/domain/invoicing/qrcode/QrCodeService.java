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
import java.security.PrivateKey;
import java.time.LocalDate;
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
 *
 * @since 0.1.0
 */
public final class QrCodeService implements QrCodes {

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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public byte[] generateLabeledQrCode(String payloadUrl, String label) {
        return addLabelToQrCode(generateQrCode(payloadUrl), label);
    }

    /**
     * One-shot KOD I (invoice verification) QR-code generator. Composes the
     * canonical {@code https://qr-{env}.ksef.mf.gov.pl/invoice/{nip}/{date}/{hash}}
     * verification URL per {@code ksef-docs/kody-qr.md} and renders it as
     * a labelled PNG.
     *
     * <p>The {@code label} is the text drawn below the QR — typically the
     * KSeF number once assigned (post-acceptance), or
     * {@link #LABEL_OFFLINE} when the invoice was issued offline and the
     * canonical KSeF number is not yet available.
     *
     * @param environment QR environment whose host is embedded in the URL
     * @param sellerNip 10-digit NIP of the invoice issuer
     * @param issueDate invoice issue date
     * @param invoiceSha256 32-byte SHA-256 hash of the canonical invoice XML
     * @param label label text rendered below the QR
     * @return PNG bytes of the labelled KOD I QR
     */
    @Override
    public byte[] generateKodIQr(QrEnvironment environment,
                                  String sellerNip,
                                  LocalDate issueDate,
                                  byte[] invoiceSha256,
                                  String label) {
        String verificationUrl = KsefVerificationLinks.buildInvoiceVerificationUrl(
                environment, sellerNip, issueDate, invoiceSha256);
        return generateLabeledQrCode(verificationUrl, label);
    }

    /**
     * One-shot KOD II (offline-certificate authenticity) QR-code generator.
     * Composes the canonical KOD II URL by signing the canonical payload
     * with the consumer's KSeF Offline certificate {@link PrivateKey}, then
     * renders the URL as a PNG labelled with
     * {@link #LABEL_CERTIFICATE}. Auto-detects RSASSA-PSS (RSA key) or
     * ECDSA-P256 with IEEE-P1363 encoding (EC key) per
     * {@code ksef-docs/kody-qr.md:197-210}.
     *
     * <p>Use this overload when you have the private key in-process. If
     * signing is delegated to an HSM or external signer, build the URL
     * manually via
     * {@link KsefVerificationLinks#canonicalCertificateSigningPayload(QrEnvironment, KsefVerificationLinks.CertificateSigningInput)}
     * then {@link KsefVerificationLinks#buildCertificateVerificationUrl(QrEnvironment, KsefVerificationLinks.CertificateVerificationParams)}
     * and call {@link #generateLabeledQrCode(String, String)} with
     * {@link #LABEL_CERTIFICATE}.
     *
     * @param environment QR environment whose host is embedded in the URL
     * @param input certificate-verification parameters (no signature yet)
     * @param privateKey RSA or EC private key from the seller's KSeF Offline certificate
     * @return PNG bytes of the labelled KOD II QR
     */
    @Override
    public byte[] generateKodIIQr(QrEnvironment environment,
                                   KsefVerificationLinks.CertificateSigningInput input,
                                   PrivateKey privateKey) {
        String verificationUrl = new QrSigningService()
                .certificateVerificationUrl(environment, input, privateKey);
        return generateLabeledQrCode(verificationUrl, LABEL_CERTIFICATE);
    }

    private static BufferedImage drawLabelBelow(BufferedImage qrImage, String label) {
        Font font = new Font(LABEL_FONT_FAMILY, Font.BOLD, LABEL_FONT_SIZE);
        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D measure = measureImage.createGraphics();
        measure.setFont(font);
        FontMetrics metrics = measure.getFontMetrics();
        int labelHeight = metrics.getHeight() + LABEL_PADDING_PX * 2;
        int measuredLabelWidth = metrics.stringWidth(label);
        measure.dispose();

        // Canvas width must accommodate the wider of (a) the QR image or
        // (b) the rendered label text + horizontal padding. Without this,
        // labels longer than the QR width (e.g. a 35-character KSeF
        // number at 14pt bold sans-serif on a 250 px QR) get clipped at
        // the canvas edge. Codex round-7 F2.
        int canvasWidth = Math.max(qrImage.getWidth(), measuredLabelWidth + LABEL_PADDING_PX * 2);

        BufferedImage canvas = new BufferedImage(canvasWidth, qrImage.getHeight() + labelHeight,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // Center the QR horizontally on the (potentially wider) canvas.
        int qrX = (canvasWidth - qrImage.getWidth()) / 2;
        graphics.drawImage(qrImage, qrX, 0, null);
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
