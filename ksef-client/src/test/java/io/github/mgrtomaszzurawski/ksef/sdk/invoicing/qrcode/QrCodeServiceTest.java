/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {

    private static final String TEST_KSEF_NUMBER = "1234567890-20260404-ABCDEF123456-78";
    private static final String PROD_URL_PREFIX = "https://ksef.mf.gov.pl/web/verify/";
    private static final String TEST_URL_PREFIX = "https://ksef-test.mf.gov.pl/web/verify/";
    private static final int CUSTOM_QR_SIZE = 300;
    private static final int PNG_HEADER_LENGTH = 8;

    // PNG magic bytes: 137 80 78 71 13 10 26 10
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Test
    void getVerificationUrl_production_returnsProdUrl() {
        QrCodeService service = new QrCodeService();

        String url = service.getVerificationUrl(TEST_KSEF_NUMBER);

        assertEquals(PROD_URL_PREFIX + TEST_KSEF_NUMBER, url);
    }

    @Test
    void getVerificationUrl_testEnvironment_returnsTestUrl() {
        QrCodeService service = new QrCodeService(true);

        String url = service.getVerificationUrl(TEST_KSEF_NUMBER);

        assertEquals(TEST_URL_PREFIX + TEST_KSEF_NUMBER, url);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getVerificationUrl_whenNullOrEmpty_throwsIllegalArgument(String input) {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class, () -> service.getVerificationUrl(input));
    }

    @Test
    void generateQrCode_defaultSize_returnsPngBytes() {
        QrCodeService service = new QrCodeService();

        byte[] pngBytes = service.generateQrCode(TEST_KSEF_NUMBER);

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > PNG_HEADER_LENGTH);
        assertPngMagicBytes(pngBytes);
    }

    @Test
    void generateQrCode_customSize_returnsPngBytes() {
        QrCodeService service = new QrCodeService();

        byte[] pngBytes = service.generateQrCode(TEST_KSEF_NUMBER, CUSTOM_QR_SIZE);

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > PNG_HEADER_LENGTH);
        assertPngMagicBytes(pngBytes);
    }

    @Test
    void generateQrCode_testEnvironment_returnsPngBytes() {
        QrCodeService service = new QrCodeService(true);

        byte[] pngBytes = service.generateQrCode(TEST_KSEF_NUMBER);

        assertNotNull(pngBytes);
        assertPngMagicBytes(pngBytes);
    }

    @Test
    void generateQrCode_whenNullKsefNumber_throwsIllegalArgument() {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class, () -> service.generateQrCode(null));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, -1, -100})
    void generateQrCode_whenInvalidSize_throwsIllegalArgument(int size) {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class,
                () -> service.generateQrCode(TEST_KSEF_NUMBER, size));
    }

    @Test
    void generateQrCode_whenSizeTooLarge_throwsIllegalArgument() {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class,
                () -> service.generateQrCode(TEST_KSEF_NUMBER, 5000));
    }

    @Test
    void getVerificationUrl_whenInvalidCharacters_throwsIllegalArgument() {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class,
                () -> service.getVerificationUrl("../../../etc/passwd"));
    }

    @Test
    void getVerificationUrl_whenQueryString_throwsIllegalArgument() {
        QrCodeService service = new QrCodeService();

        assertThrows(IllegalArgumentException.class,
                () -> service.getVerificationUrl("valid-number?evil=param"));
    }

    private static void assertPngMagicBytes(byte[] pngBytes) {
        byte[] header = new byte[PNG_HEADER_LENGTH];
        System.arraycopy(pngBytes, 0, header, 0, PNG_HEADER_LENGTH);
        assertArrayEquals(PNG_MAGIC, header);
    }
}
