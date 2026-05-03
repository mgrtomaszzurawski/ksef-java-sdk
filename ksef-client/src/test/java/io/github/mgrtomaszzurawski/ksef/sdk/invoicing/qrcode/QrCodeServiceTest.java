/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {

    private static final String SAMPLE_PAYLOAD_URL = "https://qr-test.ksef.mf.gov.pl/invoice/1234567890/04-04-2026/abc";
    private static final int CUSTOM_QR_SIZE = 300;
    private static final int OVERSIZE_QR_SIZE = 5000;
    private static final int PNG_HEADER_LENGTH = 8;

    // PNG magic bytes: 137 80 78 71 13 10 26 10
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Test
    void generateQrCode_whenDefaultSize_returnsPngBytes() {
        QrCodeService service = new QrCodeService();

        byte[] pngBytes = service.generateQrCode(SAMPLE_PAYLOAD_URL);

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > PNG_HEADER_LENGTH);
        assertPngMagicBytes(pngBytes);
    }

    @Test
    void generateQrCode_whenCustomSize_returnsPngBytes() {
        QrCodeService service = new QrCodeService();

        byte[] pngBytes = service.generateQrCode(SAMPLE_PAYLOAD_URL, CUSTOM_QR_SIZE);

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > PNG_HEADER_LENGTH);
        assertPngMagicBytes(pngBytes);
    }

    @Test
    void generateQrCode_whenPayloadNull_throwsNullPointer() {
        QrCodeService service = new QrCodeService();

        assertThrows(NullPointerException.class, () -> service.generateQrCode(null));
    }

    @Test
    void generateQrCode_whenPayloadEmpty_throwsIllegalArgument() {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class, () -> service.generateQrCode(""));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void generateQrCode_whenInvalidSize_throwsIllegalArgument(int size) {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class,
                () -> service.generateQrCode(SAMPLE_PAYLOAD_URL, size));
    }

    @Test
    void generateQrCode_whenSizeTooLarge_throwsIllegalArgument() {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class,
                () -> service.generateQrCode(SAMPLE_PAYLOAD_URL, OVERSIZE_QR_SIZE));
    }

    @Test
    void addLabelToQrCode_whenLabelOffline_returnsTallerPngWithMagicHeader() {
        QrCodeService service = new QrCodeService();
        byte[] base = service.generateQrCode(SAMPLE_PAYLOAD_URL);

        byte[] labelled = service.addLabelToQrCode(base, QrCodeService.LABEL_OFFLINE);

        assertNotNull(labelled);
        assertPngMagicBytes(labelled);
        assertTrue(labelled.length > base.length,
                "labelled PNG must be larger than the source QR; got base=" + base.length
                        + " labelled=" + labelled.length);
    }

    @Test
    void generateLabeledQrCode_whenLabelCertificate_combinesGenerateAndLabel() {
        QrCodeService service = new QrCodeService();

        byte[] labelled = service.generateLabeledQrCode(SAMPLE_PAYLOAD_URL, QrCodeService.LABEL_CERTIFICATE);

        assertNotNull(labelled);
        assertPngMagicBytes(labelled);
    }

    @Test
    void addLabelToQrCode_whenLabelEmpty_throwsIllegalArgument() {
        QrCodeService service = new QrCodeService();
        byte[] base = service.generateQrCode(SAMPLE_PAYLOAD_URL);

        assertThrows(IllegalArgumentException.class, () -> service.addLabelToQrCode(base, ""));
    }

    private static void assertPngMagicBytes(byte[] pngBytes) {
        byte[] header = new byte[PNG_HEADER_LENGTH];
        System.arraycopy(pngBytes, 0, header, 0, PNG_HEADER_LENGTH);
        assertArrayEquals(PNG_MAGIC, header);
    }
}
