/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineInvoiceTest {

    private static final String SELLER_NIP = "1234567890";
    private static final String CONTEXT_VALUE = SELLER_NIP;
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 9);
    private static final byte[] SIMPLE_XML = "<Faktura><Header/></Faktura>".getBytes(StandardCharsets.UTF_8);
    private static final FormCode CUSTOM_CODE = FormCode.custom("FA (3)", "1-0E", "FA");

    private static KsefCertificate certificate;

    @BeforeAll
    static void initCertificate() throws Exception {
        TestCertificates pair = TestCertificates.generateRsa();
        certificate = new KsefCertificate(pair.certificate(), pair.privateKey());
    }

    @Test
    void fromInvoice_whenAllInputsValid_producesNonEmptyKodIAndKodIIPng() {
        // given
        Invoice invoice = Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML);

        // when
        OfflineInvoice offline = OfflineInvoice.fromInvoice(
                invoice, certificate, OfflineMode.OFFLINE_24,
                QrEnvironment.TEST, QrContextType.NIP, CONTEXT_VALUE,
                SELLER_NIP, ISSUE_DATE);

        // then
        assertNotNull(offline.kodIQrPng());
        assertNotNull(offline.kodIIQrPng());
        assertTrue(offline.kodIQrPng().length > 0);
        assertTrue(offline.kodIIQrPng().length > 0);
    }

    @Test
    void xml_whenAccessed_returnsUnderlyingInvoiceContent() {
        // given
        Invoice invoice = Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML);
        OfflineInvoice offline = newOfflineInvoice(invoice);

        // when
        byte[] xmlOut = offline.xml();

        // then — wire-level offlineMode marker is set at the request layer,
        // not inside the XML; xml() is identical to the underlying invoice.
        assertArrayEquals(SIMPLE_XML, xmlOut);
    }

    @Test
    void xml_whenCalledTwice_returnsDefensiveCopies() {
        // given
        OfflineInvoice offline = newOfflineInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML));

        // when
        byte[] firstCopy = offline.xml();
        byte[] secondCopy = offline.xml();

        // then
        assertNotSame(firstCopy, secondCopy);
        assertArrayEquals(firstCopy, secondCopy);
    }

    @Test
    void kodIQrPng_whenCalledTwice_returnsDefensiveCopies() {
        // given
        OfflineInvoice offline = newOfflineInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML));

        // when
        byte[] firstCopy = offline.kodIQrPng();
        byte[] secondCopy = offline.kodIQrPng();

        // then
        assertNotSame(firstCopy, secondCopy);
        assertArrayEquals(firstCopy, secondCopy);
    }

    @Test
    void kodIIQrPng_whenCalledTwice_returnsDefensiveCopies() {
        // given
        OfflineInvoice offline = newOfflineInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML));

        // when
        byte[] firstCopy = offline.kodIIQrPng();
        byte[] secondCopy = offline.kodIIQrPng();

        // then
        assertNotSame(firstCopy, secondCopy);
        assertArrayEquals(firstCopy, secondCopy);
    }

    @Test
    void formCode_whenAccessed_delegatesToUnderlyingInvoice() {
        // given
        Invoice invoice = Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML);
        OfflineInvoice offline = newOfflineInvoice(invoice);

        // then
        assertEquals(CUSTOM_CODE, offline.formCode());
    }

    @Test
    void underlyingInvoice_whenAccessed_returnsSameReference() {
        // given
        Invoice invoice = Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML);
        OfflineInvoice offline = newOfflineInvoice(invoice);

        // then
        assertSame(invoice, offline.underlyingInvoice());
    }

    @Test
    void offlineMode_whenAccessed_returnsConfiguredValue() {
        // given
        Invoice invoice = Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML);
        OfflineInvoice offline = OfflineInvoice.fromInvoice(
                invoice, certificate, OfflineMode.KSEF_EMERGENCY,
                QrEnvironment.TEST, QrContextType.NIP, CONTEXT_VALUE,
                SELLER_NIP, ISSUE_DATE);

        // then
        assertEquals(OfflineMode.KSEF_EMERGENCY, offline.offlineMode());
    }

    @Test
    void hashOfCorrectedInvoice_whenNotSet_returnsEmpty() {
        // given
        OfflineInvoice offline = newOfflineInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML));

        // then
        assertTrue(offline.hashOfCorrectedInvoice().isEmpty());
    }

    private static OfflineInvoice newOfflineInvoice(Invoice invoice) {
        return OfflineInvoice.fromInvoice(invoice, certificate, OfflineMode.OFFLINE_24,
                QrEnvironment.TEST, QrContextType.NIP, CONTEXT_VALUE, SELLER_NIP, ISSUE_DATE);
    }
}
