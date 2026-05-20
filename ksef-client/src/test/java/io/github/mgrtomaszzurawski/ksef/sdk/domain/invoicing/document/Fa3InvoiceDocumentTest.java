/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.testfixtures.Fa3InvoiceFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Read-side parity test for {@link Fa3InvoiceDocument}: build a known
 * {@link Fa3Invoice} via the SDK builder, marshal to XML, parse back
 * through {@code Fa3InvoiceDocument.from(byte[])}, and assert every
 * flat-accessor returns the original input value.
 *
 * <p>Pins the read-through accessor pattern introduced by ADR-030. A
 * regression that drops a getter or wires it to the wrong XML element
 * fails here.
 */
class Fa3InvoiceDocumentTest {

    private static final String SELLER_NIP_EXPECTED = "1111111111";
    private static final String BUYER_NIP_EXPECTED = "9876543210";
    private static final String INVOICE_NUMBER_EXPECTED = "FA/2026/FIXTURE/0001";
    private static final LocalDate ISSUE_DATE_EXPECTED = LocalDate.of(2026, 5, 11);
    private static final String CURRENCY_PLN = "PLN";
    private static final BigDecimal GROSS_EXPECTED = new BigDecimal("123.00");

    @Test
    void from_whenValidFa3Xml_parsesFormCodeFa3() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(FormCode.FA3, doc.formCode());
    }

    @Test
    void from_whenValidFa3Xml_preservesXmlBytes() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        byte[] originalXml = invoice.xml();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(originalXml);
        assertEquals(originalXml.length, doc.xml().length);
    }

    @Test
    void from_whenValidFa3Xml_exposesJaxbEscapeHatch() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertNotNull(doc.unsafeJaxbView(), "JAXB escape hatch unsafeJaxbView() must not return null");
    }

    @Test
    void from_whenValidFa3Xml_sellerNipRoundTripsThroughDocument() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(SELLER_NIP_EXPECTED, doc.sellerNip());
    }

    @Test
    void from_whenValidFa3Xml_buyerNipRoundTripsThroughDocument() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(BUYER_NIP_EXPECTED, doc.buyerNip());
    }

    @Test
    void from_whenValidFa3Xml_invoiceNumberRoundTrips() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(INVOICE_NUMBER_EXPECTED, doc.invoiceNumber());
    }

    @Test
    void from_whenValidFa3Xml_currencyIsPln() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(CURRENCY_PLN, doc.currency());
    }

    @Test
    void from_whenValidFa3Xml_grossTotalRoundTrips() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(0, GROSS_EXPECTED.compareTo(doc.grossTotal()),
                "grossTotal must round-trip preserving numeric value");
    }

    @Test
    void from_whenValidFa3Xml_issueDateRoundTripsExactly() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(ISSUE_DATE_EXPECTED, doc.issueDate());
    }

    @Test
    void from_whenValidFa3Xml_emitsExactlyOneLineItem() {
        Fa3Invoice invoice = Fa3InvoiceFixtures.minimalValid();
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(invoice.xml());
        assertEquals(1, doc.lineItems().size());
    }
}
