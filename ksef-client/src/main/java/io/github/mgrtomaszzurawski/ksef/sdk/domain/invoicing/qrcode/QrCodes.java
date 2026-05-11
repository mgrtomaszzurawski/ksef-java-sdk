/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

import java.security.PrivateKey;
import java.time.LocalDate;

/**
 * QR-code generation surface — KOD I (invoice verification) + KOD II
 * (offline-certificate authenticity) facades, plus the lower-level
 * "any URL into a labelled PNG" primitives used by them.
 *
 * <p>Default implementation is {@link QrCodeService}; access via
 * {@code KsefClient.qrCode()}.
 *
 * @since 1.0.0
 */
public interface QrCodes {

    /**
     * Official KOD I label rendered when the KSeF number is unknown
     * (offline mode), per {@code ksef-docs/kody-qr.md}.
     */
    String LABEL_OFFLINE = "OFFLINE";

    /**
     * Official KOD II label rendered below an offline-certificate
     * verification QR, per {@code ksef-docs/kody-qr.md}.
     */
    String LABEL_CERTIFICATE = "CERTYFIKAT";

    /** Render a payload URL into a 250x250 QR-code PNG. */
    byte[] generateQrCode(String payloadUrl);

    /** Render a payload URL into a QR-code PNG of the requested size. */
    byte[] generateQrCode(String payloadUrl, int size);

    /** Render an existing QR PNG with a text label drawn below the code. */
    byte[] addLabelToQrCode(byte[] qrPng, String label);

    /** Convenience: render a QR PNG for {@code payloadUrl} and append the supplied {@code label} below it. */
    byte[] generateLabeledQrCode(String payloadUrl, String label);

    /**
     * One-shot KOD I (invoice verification) QR-code generator. See
     * {@link QrCodeService#generateKodIQr(QrEnvironment, String, LocalDate, byte[], String)}
     * for full semantics.
     */
    byte[] generateKodIQr(QrEnvironment environment,
                          String sellerNip,
                          LocalDate issueDate,
                          byte[] invoiceSha256,
                          String label);

    /**
     * One-shot KOD II (offline-certificate authenticity) QR-code
     * generator. See
     * {@link QrCodeService#generateKodIIQr(QrEnvironment, KsefVerificationLinks.CertificateSigningInput, PrivateKey)}
     * for full semantics.
     */
    byte[] generateKodIIQr(QrEnvironment environment,
                           KsefVerificationLinks.CertificateSigningInput input,
                           PrivateKey privateKey);
}
