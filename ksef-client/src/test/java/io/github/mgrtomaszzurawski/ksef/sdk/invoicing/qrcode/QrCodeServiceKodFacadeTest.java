/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceKodFacadeTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String CERTIFICATE_SERIAL = "0123456789ABCDEF";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 9);
    private static final int RSA_KEY_SIZE = 2048;
    private static final byte[] PNG_MAGIC_HEADER = new byte[] {(byte) 0x89, 'P', 'N', 'G'};

    @Test
    void generateKodIQr_returnsLabelledPngBytes() {
        QrCodeService service = new QrCodeService();
        byte[] invoiceSha256 = new byte[32];
        new SecureRandom().nextBytes(invoiceSha256);

        byte[] qrCodePng =service.generateKodIQr(QrEnvironment.TEST, SELLER_NIP, ISSUE_DATE, invoiceSha256,
                QrCodeService.LABEL_OFFLINE);

        assertNotNull(qrCodePng);
        assertTrue(qrCodePng.length > PNG_MAGIC_HEADER.length, "KOD I QR PNG must be non-trivially sized");
        for (int i = 0; i < PNG_MAGIC_HEADER.length; i++) {
            assertTrue(qrCodePng[i] == PNG_MAGIC_HEADER[i], "byte " + i + " must match PNG magic header");
        }
    }

    @Test
    void generateKodIIQr_returnsLabelledPngBytes_forRsaKey() throws Exception {
        QrCodeService service = new QrCodeService();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        byte[] invoiceSha256 = new byte[32];
        new SecureRandom().nextBytes(invoiceSha256);
        KsefVerificationLinks.CertificateSigningInput input = new KsefVerificationLinks.CertificateSigningInput(
                QrContextType.NIP, SELLER_NIP, SELLER_NIP, CERTIFICATE_SERIAL, invoiceSha256);

        byte[] qrCodePng =service.generateKodIIQr(QrEnvironment.TEST, input, keyPair.getPrivate());

        assertNotNull(qrCodePng);
        assertTrue(qrCodePng.length > PNG_MAGIC_HEADER.length, "KOD II QR PNG must be non-trivially sized");
        for (int i = 0; i < PNG_MAGIC_HEADER.length; i++) {
            assertTrue(qrCodePng[i] == PNG_MAGIC_HEADER[i], "byte " + i + " must match PNG magic header");
        }
    }
}
