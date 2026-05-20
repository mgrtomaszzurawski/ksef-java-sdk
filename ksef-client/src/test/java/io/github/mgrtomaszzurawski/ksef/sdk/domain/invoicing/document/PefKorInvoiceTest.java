/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.CreditNoteType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PefKorInvoiceTest {

    private static final String CREDIT_NOTE_NUMBER = "PEFKOR/2026/0001";
    private static final String ORIGINAL_INVOICE_NUMBER = "PEF/2025/0099";
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");
    private static final String UNIT_CODE = "C62";

    @Test
    void formCode_whenCreditNoteBuilt_returnsPefKor3() {
        PefKorInvoice creditNote = minimalCreditNote();
        assertEquals(FormCode.PEF_KOR3, creditNote.formCode());
    }

    @Test
    void xml_whenCreditNoteBuilt_producesUblCreditNoteRootElement() {
        PefKorInvoice creditNote = minimalCreditNote();
        String xml = new String(creditNote.xml(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("CreditNote"),
                "XML must contain UBL CreditNote root: " + xml);
    }

    @Test
    void xml_whenCreditNoteBuilt_roundTripsThroughJaxbUnchanged() throws Exception {
        PefKorInvoice creditNote = minimalCreditNote();
        JAXBContext context = JAXBContext.newInstance(
                io.github.mgrtomaszzurawski.ksef.xml.pefkor.ObjectFactory.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object parsed = unmarshaller.unmarshal(new ByteArrayInputStream(creditNote.xml()));
        CreditNoteType result = parsed instanceof JAXBElement<?> wrapper
                ? (CreditNoteType) wrapper.getValue() : (CreditNoteType) parsed;
        assertNotNull(result);
        assertEquals(CREDIT_NOTE_NUMBER, result.getID().getValue());
        assertEquals(1, result.getBillingReference().size());
    }

    @Test
    void xml_whenCalledTwice_returnsDefensiveCopies() {
        PefKorInvoice creditNote = minimalCreditNote();
        byte[] firstCopy = creditNote.xml();
        byte[] secondCopy = creditNote.xml();
        assertNotSame(firstCopy, secondCopy);
    }

    private static PefKorInvoice minimalCreditNote() {
        PefAddress address = new PefAddress("Marszalkowska 10", "Warszawa", "00-001", "PL");
        return PefKorInvoice.builder()
                .creditNoteNumber(CREDIT_NOTE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 9))
                .supplier(new PefParty("1111111111", null, "Acme", "1111111111", address))
                .customer(new PefParty("9876543210", null, "Customer", "9876543210", address))
                .addLine(new PefInvoiceLine("1", new BigDecimal("1"), UNIT_CODE,
                        AMOUNT, "Refund", new BigDecimal("23")))
                .payableAmount(AMOUNT)
                .originalInvoiceNumber(ORIGINAL_INVOICE_NUMBER)
                .build();
    }
}
