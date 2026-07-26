/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AdditionalDescription;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Round-trip guard for three small {@code Fa}-header nodes: the billing
 * period ({@code OkresFa} → {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePeriod}),
 * warehouse-issue numbers ({@code WZ}) and additional key/value
 * descriptions ({@code DodatkowyOpis} → {@link AdditionalDescription}).
 * Set through the JAXB escape hatch, XSD-validated, read back.
 *
 * <p>{@code OkresFa} is an XSD choice alternative to the single delivery
 * date {@code P_6}, so the builder's {@code deliveryDate} is left unset.
 */
class MiscHeaderRoundTripTest {

    private static final DatatypeFactory DTF = datatypeFactory();
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final LocalDate PERIOD_FROM = LocalDate.of(2026, 4, 1);
    private static final LocalDate PERIOD_TO = LocalDate.of(2026, 4, 30);

    private static DatatypeFactory datatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static XMLGregorianCalendar greg(LocalDate date) {
        return DTF.newXMLGregorianCalendarDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                DatatypeConstants.FIELD_UNDEFINED);
    }

    private static InvoiceParty seller() {
        return new InvoiceParty("1111111111", "Acme sp. z o.o.", "00-001", "Warszawa", "Marszalkowska", "10", null);
    }

    private static InvoiceParty buyer() {
        return new InvoiceParty("9876543210", "Customer sp. z o.o.", "00-002", "Krakow", null, "5", null);
    }

    private static InvoiceLineItem plainLine() {
        return InvoiceLineItem.builder().rowNumber(1).description("Consulting").unitOfMeasure("szt.")
                .quantity(BigDecimal.ONE).netUnitPrice(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("100.00")).vatRate("23").build();
    }

    private static void assertNoXsdErrors(byte[] xml, FormCode form) {
        List<ValidationIssue> issues = KsefXmlValidator.validate(xml, form);
        assertFalse(issues.stream().anyMatch(i -> i.severity() == Severity.ERROR || i.severity() == Severity.FATAL),
                "Generated XML must survive XSD validation: " + issues);
    }

    @Test
    void fa3_periodDeliveryNotesAndDescriptions_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/MISC/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var fa = faktura.getFa();
                    var okres = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.OkresFa();
                    okres.setP6Od(greg(PERIOD_FROM));
                    okres.setP6Do(greg(PERIOD_TO));
                    fa.setOkresFa(okres);
                    fa.getWZ().add("WZ/2026/001");
                    fa.getWZ().add("WZ/2026/002");
                    var kv = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TKluczWartosc();
                    kv.setNrWiersza(BigInteger.ONE);
                    kv.setKlucz("origin");
                    kv.setWartosc("PL");
                    fa.getDodatkowyOpis().add(kv);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);
        assertMisc(document.invoicePeriod(), document.deliveryNoteNumbers(), document.additionalDescriptions());
    }

    @Test
    void fa2_periodDeliveryNotesAndDescriptions_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/MISC/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var fa = faktura.getFa();
                    var okres = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.OkresFa();
                    okres.setP6Od(greg(PERIOD_FROM));
                    okres.setP6Do(greg(PERIOD_TO));
                    fa.setOkresFa(okres);
                    fa.getWZ().add("WZ/2026/001");
                    fa.getWZ().add("WZ/2026/002");
                    var kv = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TKluczWartosc();
                    kv.setNrWiersza(BigInteger.ONE);
                    kv.setKlucz("origin");
                    kv.setWartosc("PL");
                    fa.getDodatkowyOpis().add(kv);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        Fa2InvoiceDocument document = Fa2InvoiceDocument.from(xml);
        assertMisc(document.invoicePeriod(), document.deliveryNoteNumbers(), document.additionalDescriptions());
    }

    private static void assertMisc(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePeriod period,
            List<String> deliveryNotes, List<AdditionalDescription> descriptions) {
        assertEquals(PERIOD_FROM, period.from());
        assertEquals(PERIOD_TO, period.to());
        assertEquals(List.of("WZ/2026/001", "WZ/2026/002"), deliveryNotes);
        assertEquals(1, descriptions.size());
        assertEquals(1, descriptions.get(0).rowNumber());
        assertEquals("origin", descriptions.get(0).key());
        assertEquals("PL", descriptions.get(0).value());
    }
}
