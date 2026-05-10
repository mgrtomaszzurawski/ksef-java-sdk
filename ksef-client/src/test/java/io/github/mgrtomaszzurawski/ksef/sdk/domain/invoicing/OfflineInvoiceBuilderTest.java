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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineInvoiceBuilderTest {

    private static final String SELLER_NIP = "1234567890";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 9);
    private static final byte[] SIMPLE_XML = "<Faktura/>".getBytes(StandardCharsets.UTF_8);
    private static final FormCode CUSTOM_CODE = FormCode.custom("FA (3)", "1-0E", "FA");
    private static final int SHA256_LENGTH = 32;

    private static KsefCertificate certificate;

    @BeforeAll
    static void initCertificate() throws Exception {
        TestCertificates pair = TestCertificates.generateRsa();
        certificate = new KsefCertificate(pair.certificate(), pair.privateKey());
    }

    @Test
    void build_whenAllRequiredFieldsSet_producesOfflineInvoice() {
        // given
        Invoice invoice = Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML);

        // when
        OfflineInvoice offline = OfflineInvoiceBuilder.forInvoice(invoice)
                .signingCertificate(certificate)
                .offlineMode(OfflineMode.OFFLINE_24)
                .qrEnvironment(QrEnvironment.TEST)
                .contextType(QrContextType.NIP)
                .contextValue(SELLER_NIP)
                .sellerNip(SELLER_NIP)
                .issueDate(ISSUE_DATE)
                .build();

        // then
        assertNotNull(offline);
        assertEquals(OfflineMode.OFFLINE_24, offline.offlineMode());
        assertTrue(offline.hashOfCorrectedInvoice().isEmpty());
    }

    @Test
    void build_whenCertificateMissing_throwsIllegalStateException() {
        // given — builder with no certificate
        OfflineInvoiceBuilder builder = OfflineInvoiceBuilder
                .forInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML))
                .offlineMode(OfflineMode.OFFLINE_24)
                .qrEnvironment(QrEnvironment.TEST)
                .contextType(QrContextType.NIP)
                .contextValue(SELLER_NIP)
                .sellerNip(SELLER_NIP)
                .issueDate(ISSUE_DATE);

        // then
        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("signingCertificate"));
    }

    @Test
    void build_whenOfflineModeMissing_throwsIllegalStateException() {
        // given
        OfflineInvoiceBuilder builder = OfflineInvoiceBuilder
                .forInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML))
                .signingCertificate(certificate)
                .qrEnvironment(QrEnvironment.TEST)
                .contextType(QrContextType.NIP)
                .contextValue(SELLER_NIP)
                .sellerNip(SELLER_NIP)
                .issueDate(ISSUE_DATE);

        // then
        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("offlineMode"));
    }

    @Test
    void signingCertificate_whenNull_throwsNullPointerException() {
        // given
        OfflineInvoiceBuilder builder = OfflineInvoiceBuilder
                .forInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML));

        // then
        assertThrows(NullPointerException.class, () -> builder.signingCertificate(null));
    }

    @Test
    void offlineMode_whenNull_throwsNullPointerException() {
        // given
        OfflineInvoiceBuilder builder = OfflineInvoiceBuilder
                .forInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML));

        // then
        assertThrows(NullPointerException.class, () -> builder.offlineMode(null));
    }

    @Test
    void hashOfCorrectedInvoice_whenSetWithValidLength_carriesThroughBuild() {
        // given
        byte[] hash = new byte[SHA256_LENGTH];
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) i;
        }

        // when
        OfflineInvoice offline = OfflineInvoiceBuilder
                .forInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML))
                .signingCertificate(certificate)
                .offlineMode(OfflineMode.KSEF_UNAVAILABILITY)
                .qrEnvironment(QrEnvironment.TEST)
                .contextType(QrContextType.NIP)
                .contextValue(SELLER_NIP)
                .sellerNip(SELLER_NIP)
                .issueDate(ISSUE_DATE)
                .hashOfCorrectedInvoice(hash)
                .build();

        // then
        assertTrue(offline.hashOfCorrectedInvoice().isPresent());
        assertEquals(SHA256_LENGTH, offline.hashOfCorrectedInvoice().orElseThrow().length);
    }

    @Test
    void hashOfCorrectedInvoice_whenWrongLength_throwsIllegalArgumentException() {
        // given
        OfflineInvoiceBuilder builder = OfflineInvoiceBuilder
                .forInvoice(Invoice.fromXml(CUSTOM_CODE, SIMPLE_XML));

        // then
        assertThrows(IllegalArgumentException.class,
                () -> builder.hashOfCorrectedInvoice(new byte[SHA256_LENGTH - 1]));
    }
}
