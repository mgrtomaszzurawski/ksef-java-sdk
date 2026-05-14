/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Gate test for the Bundle B JAXB escape-hatch contract: mutations on
 * the value returned by {@code unsafeJaxbView()} must NOT bleed into the
 * flat accessor outputs nor into the {@code xml()} bytes.
 *
 * <p>The four typed invoice classes (Fa2Invoice / Fa3Invoice /
 * PefInvoice / PefKorInvoice) all snapshot their flat-accessor values
 * at construction time (Bundle F-1 closure), so this test asserts the
 * snapshot is observable through the public surface.
 *
 * <p>{@code xml()} returns a defensive copy of the bytes captured at
 * {@code build()}/{@code from()} time and is therefore immune to JAXB
 * tree mutations by construction; this test pins that behaviour as a
 * regression gate.
 */
class UnsafeJaxbViewMutationIsolationTest {

    private static final String ORIGINAL_SELLER_NIP = "1111111111";
    private static final String MUTATED_SELLER_NIP = "9999999999";
    private static final String INVOICE_NUMBER = "FA/2026/0099";

    @Test
    void fa3Invoice_mutatingUnsafeJaxbView_doesNotAffectFlatAccessorsOrXmlBytes() {
        Fa3Invoice invoice = buildFa3Invoice();
        byte[] xmlBefore = invoice.xml();
        String sellerNipBefore = invoice.sellerNip();
        assertEquals(ORIGINAL_SELLER_NIP, sellerNipBefore);

        invoice.unsafeJaxbView().getPodmiot1().getDaneIdentyfikacyjne().setNIP(MUTATED_SELLER_NIP);

        assertEquals(sellerNipBefore, invoice.sellerNip(),
                "Fa3Invoice flat accessor must snapshot at construction");
        byte[] xmlAfter = invoice.xml();
        assertEquals(new String(xmlBefore), new String(xmlAfter),
                "Fa3Invoice.xml() must not reflect JAXB tree mutations");
    }

    @Test
    void fa2Invoice_mutatingUnsafeJaxbView_doesNotAffectFlatAccessorsOrXmlBytes() {
        Fa2Invoice invoice = buildFa2Invoice();
        byte[] xmlBefore = invoice.xml();
        String sellerNipBefore = invoice.sellerNip();
        assertEquals(ORIGINAL_SELLER_NIP, sellerNipBefore);

        invoice.unsafeJaxbView().getPodmiot1().getDaneIdentyfikacyjne().setNIP(MUTATED_SELLER_NIP);

        assertEquals(sellerNipBefore, invoice.sellerNip(),
                "Fa2Invoice flat accessor must snapshot at construction");
        byte[] xmlAfter = invoice.xml();
        assertEquals(new String(xmlBefore), new String(xmlAfter),
                "Fa2Invoice.xml() must not reflect JAXB tree mutations");
    }

    @Test
    void fa3InvoiceDocument_mutatingUnsafeJaxbView_doesNotAffectFlatAccessorsOrXmlBytes() {
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(buildFa3Invoice().xml());
        byte[] xmlBefore = document.xml();
        String sellerNipBefore = document.sellerNip();
        assertEquals(ORIGINAL_SELLER_NIP, sellerNipBefore);

        document.unsafeJaxbView().getPodmiot1().getDaneIdentyfikacyjne().setNIP(MUTATED_SELLER_NIP);

        assertEquals(sellerNipBefore, document.sellerNip(),
                "Fa3InvoiceDocument flat accessor must snapshot at construction");
        byte[] xmlAfter = document.xml();
        assertEquals(new String(xmlBefore), new String(xmlAfter),
                "Fa3InvoiceDocument.xml() must not reflect JAXB tree mutations");
    }

    @Test
    void toJaxbCopy_returnsDisconnectedTreeThatCanBeMutatedSafely() {
        Fa3Invoice invoice = buildFa3Invoice();
        var clone = invoice.toJaxbCopy();
        clone.getPodmiot1().getDaneIdentyfikacyjne().setNIP(MUTATED_SELLER_NIP);

        assertNotEquals(MUTATED_SELLER_NIP,
                invoice.unsafeJaxbView().getPodmiot1().getDaneIdentyfikacyjne().getNIP(),
                "toJaxbCopy() must be disconnected from the internal JAXB tree");
        assertEquals(ORIGINAL_SELLER_NIP, invoice.sellerNip(),
                "Fa3Invoice flat accessor must not observe mutations on a toJaxbCopy() clone");
    }

    @Test
    void pefInvoice_mutatingUnsafeJaxbView_doesNotAffectFlatAccessorsOrXmlBytes() {
        PefInvoice invoice = buildPefInvoice();
        byte[] xmlBefore = invoice.xml();
        String invoiceNumberBefore = invoice.invoiceNumber();
        assertNotNull(invoiceNumberBefore);

        invoice.unsafeJaxbView().getID().setValue("MUTATED");

        assertEquals(invoiceNumberBefore, invoice.invoiceNumber(),
                "PefInvoice flat accessor must snapshot at construction");
        byte[] xmlAfter = invoice.xml();
        assertEquals(new String(xmlBefore), new String(xmlAfter),
                "PefInvoice.xml() must not reflect UBL JAXB tree mutations");
    }

    @Test
    void pefKorInvoice_mutatingUnsafeJaxbView_doesNotAffectFlatAccessorsOrXmlBytes() {
        PefKorInvoice invoice = buildPefKorInvoice();
        byte[] xmlBefore = invoice.xml();
        String invoiceNumberBefore = invoice.invoiceNumber();
        assertNotNull(invoiceNumberBefore);

        invoice.unsafeJaxbView().getID().setValue("MUTATED");

        assertEquals(invoiceNumberBefore, invoice.invoiceNumber(),
                "PefKorInvoice flat accessor must snapshot at construction");
        byte[] xmlAfter = invoice.xml();
        assertEquals(new String(xmlBefore), new String(xmlAfter),
                "PefKorInvoice.xml() must not reflect UBL JAXB tree mutations");
    }

    private static Fa3Invoice buildFa3Invoice() {
        return Fa3Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 12))
                .seller(new InvoiceParty(ORIGINAL_SELLER_NIP, "Acme sp. z o.o.", "00-001",
                        "Warszawa", "Marszalkowska", "10", null))
                .buyer(new InvoiceParty("9876543210", "Customer sp. z o.o.", "00-002",
                        "Krakow", null, "5", null))
                .totalGrossAmount(new BigDecimal("123.00"))
                .addLineItem(new InvoiceLineItem(1, "Consulting", "szt.", new BigDecimal("1"),
                        new BigDecimal("100.00"), new BigDecimal("100.00"), "23"))
                .build();
    }

    private static Fa2Invoice buildFa2Invoice() {
        return Fa2Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 12))
                .seller(new InvoiceParty(ORIGINAL_SELLER_NIP, "Acme sp. z o.o.", "00-001",
                        "Warszawa", "Marszalkowska", "10", null))
                .buyer(new InvoiceParty("9876543210", "Customer sp. z o.o.", "00-002",
                        "Krakow", null, "5", null))
                .totalGrossAmount(new BigDecimal("123.00"))
                .addLineItem(new InvoiceLineItem(1, "Consulting", "szt.", new BigDecimal("1"),
                        new BigDecimal("100.00"), new BigDecimal("100.00"), "23"))
                .build();
    }

    private static PefInvoice buildPefInvoice() {
        return PefInvoice.builder()
                .invoiceNumber("PEF/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 12))
                .supplier(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty(
                        "PL:" + ORIGINAL_SELLER_NIP, null, "Acme sp. z o.o.", ORIGINAL_SELLER_NIP,
                        new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress(
                                "Marszalkowska 10", "Warszawa", "00-001", "PL")))
                .customer(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty(
                        "PL:9876543210", null, "Customer sp. z o.o.", "9876543210",
                        new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress(
                                "Rynek 5", "Krakow", "00-002", "PL")))
                .addLine(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine(
                        "1", new BigDecimal("1"), "C62", new BigDecimal("100.00"),
                        "Consulting", new BigDecimal("23")))
                .payableAmount(new BigDecimal("123.00"))
                .build();
    }

    private static PefKorInvoice buildPefKorInvoice() {
        return PefKorInvoice.builder()
                .creditNoteNumber("KOR/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 12))
                .supplier(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty(
                        "PL:" + ORIGINAL_SELLER_NIP, null, "Acme sp. z o.o.", ORIGINAL_SELLER_NIP,
                        new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress(
                                "Marszalkowska 10", "Warszawa", "00-001", "PL")))
                .customer(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty(
                        "PL:9876543210", null, "Customer sp. z o.o.", "9876543210",
                        new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress(
                                "Rynek 5", "Krakow", "00-002", "PL")))
                .addLine(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine(
                        "1", new BigDecimal("1"), "C62", new BigDecimal("100.00"),
                        "Consulting", new BigDecimal("23")))
                .payableAmount(new BigDecimal("123.00"))
                .originalInvoiceNumber("PEF/2026/0001")
                .build();
    }
}
