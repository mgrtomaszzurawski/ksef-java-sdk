/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Read-side parity test for {@link PefInvoiceDocument}: build a known
 * {@link PefInvoice} via the SDK builder, marshal to UBL XML, parse back
 * through {@code PefInvoiceDocument.from(byte[])}, and assert every
 * flat-accessor returns the original input value.
 */
class PefInvoiceDocumentTest {

    private static final String INVOICE_NUMBER = "PEF/2026/0001";
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal VAT = new BigDecimal("23");
    private static final BigDecimal PAYABLE_AMOUNT = new BigDecimal("123.00");
    private static final String CURRENCY = "PLN";
    private static final String UNIT_CODE = "C62";
    private static final String SUPPLIER_NIP = "1111111111";
    private static final String CUSTOMER_NIP = "9876543210";
    private static final String SUPPLIER_NAME = "Acme";
    private static final String CUSTOMER_NAME = "Customer";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 9);

    @Test
    void from_whenValidPefXml_parsesFormCodePef3() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(FormCode.PEF3, doc.formCode());
    }

    @Test
    void from_whenValidPefXml_preservesXmlBytes() {
        byte[] xml = minimalInvoice().xml();
        PefInvoiceDocument doc = PefInvoiceDocument.from(xml);
        assertEquals(xml.length, doc.xml().length);
    }

    @Test
    void from_whenValidPefXml_exposesJaxbEscapeHatch() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertNotNull(doc.unsafeJaxbView(), "JAXB escape hatch unsafeJaxbView() must not return null");
    }

    @Test
    void from_whenValidPefXml_invoiceNumberRoundTrips() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(INVOICE_NUMBER, doc.invoiceNumber());
    }

    @Test
    void from_whenValidPefXml_currencyRoundTrips() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(CURRENCY, doc.currency());
    }

    @Test
    void from_whenValidPefXml_supplierNameRoundTrips() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(SUPPLIER_NAME, doc.supplierName());
    }

    @Test
    void from_whenValidPefXml_customerNameRoundTrips() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(CUSTOMER_NAME, doc.customerName());
    }

    @Test
    void from_whenValidPefXml_payableAmountRoundTrips() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(0, PAYABLE_AMOUNT.compareTo(doc.payableAmount()));
    }

    @Test
    void from_whenValidPefXml_issueDateRoundTripsExactly() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(ISSUE_DATE, doc.issueDate());
    }

    @Test
    void from_whenValidPefXml_emitsExactlyOneLine() {
        PefInvoiceDocument doc = PefInvoiceDocument.from(minimalInvoice().xml());
        assertEquals(1, doc.lines().size());
    }

    private static PefInvoice minimalInvoice() {
        PefAddress address = new PefAddress("Marszalkowska 10", "Warszawa", "00-001", "PL");
        return PefInvoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(ISSUE_DATE)
                .currencyCode(CURRENCY)
                .supplier(new PefParty(SUPPLIER_NIP, null, SUPPLIER_NAME, SUPPLIER_NIP, address))
                .customer(new PefParty(CUSTOMER_NIP, null, CUSTOMER_NAME, CUSTOMER_NIP, address))
                .addLine(new PefInvoiceLine("1", BigDecimal.ONE, UNIT_CODE,
                        AMOUNT, "Consulting", VAT))
                .payableAmount(PAYABLE_AMOUNT)
                .build();
    }
}
