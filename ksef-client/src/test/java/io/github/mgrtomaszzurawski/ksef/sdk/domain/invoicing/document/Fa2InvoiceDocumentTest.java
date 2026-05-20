/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Read-side parity test for {@link Fa2InvoiceDocument}: build a known
 * {@link Fa2Invoice} via the SDK builder, marshal to XML, parse back
 * through {@code Fa2InvoiceDocument.from(byte[])}, and assert every
 * flat-accessor returns the original input value.
 */
class Fa2InvoiceDocumentTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String BUYER_NIP = "9876543210";
    private static final String INVOICE_NUMBER = "FA2/2026/0001";
    private static final BigDecimal NET_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");
    private static final String VAT_RATE = "23";
    private static final String CURRENCY_PLN = "PLN";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 9);

    @Test
    void from_whenValidFa2Xml_parsesFormCodeFa2() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(FormCode.FA2, doc.formCode());
    }

    @Test
    void from_whenValidFa2Xml_preservesXmlBytes() {
        byte[] xml = minimalInvoice().xml();
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(xml);
        assertEquals(xml.length, doc.xml().length);
    }

    @Test
    void from_whenValidFa2Xml_exposesJaxbEscapeHatch() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertNotNull(doc.unsafeJaxbView(), "JAXB escape hatch unsafeJaxbView() must not return null");
    }

    @Test
    void from_whenValidFa2Xml_sellerNipRoundTrips() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(SELLER_NIP, doc.sellerNip());
    }

    @Test
    void from_whenValidFa2Xml_buyerNipRoundTrips() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(BUYER_NIP, doc.buyerNip());
    }

    @Test
    void from_whenValidFa2Xml_invoiceNumberRoundTrips() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(INVOICE_NUMBER, doc.invoiceNumber());
    }

    @Test
    void from_whenValidFa2Xml_currencyIsPln() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(CURRENCY_PLN, doc.currency());
    }

    @Test
    void from_whenValidFa2Xml_grossTotalRoundTrips() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(0, GROSS_AMOUNT.compareTo(doc.grossTotal()));
    }

    @Test
    void from_whenValidFa2Xml_issueDateRoundTripsExactly() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(ISSUE_DATE, doc.issueDate());
    }

    @Test
    void from_whenValidFa2Xml_emitsExactlyOneLineItem() {
        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(minimalInvoice().xml());
        assertEquals(1, doc.lineItems().size());
    }

    private static Fa2Invoice minimalInvoice() {
        return Fa2Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(ISSUE_DATE)
                .seller(new InvoiceParty(SELLER_NIP, "Acme", "00-001", "Warszawa", "Marszalkowska", "10", null))
                .buyer(new InvoiceParty(BUYER_NIP, "Customer", "00-002", "Krakow", null, "5", null))
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(new InvoiceLineItem(1, "Consulting", "szt.",
                        BigDecimal.ONE, NET_AMOUNT, NET_AMOUNT, VAT_RATE))
                .build();
    }
}
