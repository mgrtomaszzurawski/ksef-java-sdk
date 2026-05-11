/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Read-side parity test for {@link PefKorInvoiceDocument}: build a known
 * {@link PefKorInvoice} via the SDK builder, marshal to UBL CreditNote
 * XML, parse back through {@code PefKorInvoiceDocument.from(byte[])},
 * and assert every flat-accessor returns the original input value.
 */
class PefKorInvoiceDocumentTest {

    private static final String CREDIT_NOTE_NUMBER = "PEFKOR/2026/0001";
    private static final String ORIGINAL_INVOICE_NUMBER = "PEF/2025/0099";
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");
    private static final BigDecimal VAT = new BigDecimal("23");
    private static final String UNIT_CODE = "C62";
    private static final String SUPPLIER_NIP = "1111111111";
    private static final String CUSTOMER_NIP = "9876543210";
    private static final String SUPPLIER_NAME = "Acme";
    private static final String CUSTOMER_NAME = "Customer";

    @Test
    void from_whenValidPefKorXml_parsesFormCodePefKor3() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertEquals(FormCode.PEF_KOR3, doc.formCode());
    }

    @Test
    void from_whenValidPefKorXml_preservesXmlBytes() {
        byte[] xml = minimalCreditNote().xml();
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(xml);
        assertEquals(xml.length, doc.xml().length);
    }

    @Test
    void from_whenValidPefKorXml_exposesJaxbEscapeHatch() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertNotNull(doc.creditNote(), "JAXB escape hatch creditNote() must not return null");
    }

    @Test
    void from_whenValidPefKorXml_creditNoteNumberRoundTrips() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertEquals(CREDIT_NOTE_NUMBER, doc.invoiceNumber());
    }

    @Test
    void from_whenValidPefKorXml_supplierNameRoundTrips() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertEquals(SUPPLIER_NAME, doc.supplierName());
    }

    @Test
    void from_whenValidPefKorXml_customerNameRoundTrips() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertEquals(CUSTOMER_NAME, doc.customerName());
    }

    @Test
    void from_whenValidPefKorXml_payableAmountRoundTrips() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertEquals(0, AMOUNT.compareTo(doc.payableAmount()));
    }

    @Test
    void from_whenValidPefKorXml_issueDateNotNull() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertNotNull(doc.issueDate());
    }

    @Test
    void from_whenValidPefKorXml_linesNonEmpty() {
        PefKorInvoiceDocument doc = PefKorInvoiceDocument.from(minimalCreditNote().xml());
        assertTrue(doc.lines().size() >= 1);
    }

    private static PefKorInvoice minimalCreditNote() {
        PefAddress address = new PefAddress("Marszalkowska 10", "Warszawa", "00-001", "PL");
        return PefKorInvoice.builder()
                .creditNoteNumber(CREDIT_NOTE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 9))
                .supplier(new PefParty(SUPPLIER_NIP, null, SUPPLIER_NAME, SUPPLIER_NIP, address))
                .customer(new PefParty(CUSTOMER_NIP, null, CUSTOMER_NAME, CUSTOMER_NIP, address))
                .addLine(new PefInvoiceLine("1", BigDecimal.ONE, UNIT_CODE,
                        AMOUNT, "Refund", VAT))
                .payableAmount(AMOUNT)
                .originalInvoiceNumber(ORIGINAL_INVOICE_NUMBER)
                .build();
    }
}
