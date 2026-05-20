/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import io.github.mgrtomaszzurawski.ksef.xml.pef.InvoiceType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PefInvoiceTest {

    private static final String INVOICE_NUMBER = "PEF/2026/0001";
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal VAT = new BigDecimal("23");
    private static final String CURRENCY = "PLN";
    private static final String UNIT_CODE = "C62";

    @Test
    void formCode_whenInvoiceBuilt_returnsPef3() {
        PefInvoice invoice = minimalInvoice();
        assertEquals(FormCode.PEF3, invoice.formCode());
    }

    @Test
    void xml_whenInvoiceBuilt_producesUblInvoiceRootElement() {
        PefInvoice invoice = minimalInvoice();
        String xml = new String(invoice.xml(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(xml.contains("Invoice"),
                "XML must contain UBL Invoice root: " + xml);
    }

    @Test
    void xml_whenInvoiceBuilt_roundTripsThroughJaxbUnchanged() throws Exception {
        PefInvoice invoice = minimalInvoice();
        // Build context the same way JaxbInvoiceMarshaller does: include
        // the xml.pef ObjectFactory (carries @XmlElementDecl for the
        // {urn:oasis:Invoice-2}Invoice root) so the unmarshaller can
        // resolve the qualified element name.
        JAXBContext context = JAXBContext.newInstance(
                io.github.mgrtomaszzurawski.ksef.xml.pef.ObjectFactory.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object parsed = unmarshaller.unmarshal(new ByteArrayInputStream(invoice.xml()));
        InvoiceType result = parsed instanceof JAXBElement<?> wrapper
                ? (InvoiceType) wrapper.getValue() : (InvoiceType) parsed;
        assertNotNull(result);
        assertEquals(INVOICE_NUMBER, result.getID().getValue());
    }

    @Test
    void xml_whenCalledTwice_returnsDefensiveCopies() {
        PefInvoice invoice = minimalInvoice();
        byte[] firstCopy = invoice.xml();
        byte[] secondCopy = invoice.xml();
        assertNotSame(firstCopy, secondCopy);
    }

    private static PefInvoice minimalInvoice() {
        PefAddress address = new PefAddress("Marszalkowska 10", "Warszawa", "00-001", "PL");
        return PefInvoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 9))
                .currencyCode(CURRENCY)
                .supplier(new PefParty("1111111111", null, "Acme", "1111111111", address))
                .customer(new PefParty("9876543210", null, "Customer", "9876543210", address))
                .addLine(new PefInvoiceLine("1", new BigDecimal("1"), UNIT_CODE,
                        AMOUNT, "Consulting", VAT))
                .payableAmount(new BigDecimal("123.00"))
                .build();
    }
}
