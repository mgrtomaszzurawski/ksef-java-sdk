/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceFooter;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.RegistryEntry;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Round-trip guard for the {@code Faktura/Stopka} footer, surfaced as the
 * {@link InvoiceFooter} nested record on both documents. Set through the
 * JAXB escape hatch (the builder does not surface it), XSD-validated, and
 * read back through {@code footer()}.
 */
class FooterRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final String NOTE_ONE = "Thank you for your business.";
    private static final String NOTE_TWO = "Complaints within 14 days.";
    private static final String FULL_NAME = "Acme Spolka z ograniczona odpowiedzialnoscia";
    private static final String KRS = "0000123456";
    private static final String REGON = "123456789";
    private static final String BDO = "000012345";

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

    private static void assertFooter(InvoiceFooter footer) {
        assertNotNull(footer);
        assertEquals(List.of(NOTE_ONE, NOTE_TWO), footer.notes());
        assertEquals(1, footer.registries().size());
        RegistryEntry registry = footer.registries().get(0);
        assertEquals(FULL_NAME, registry.fullName());
        assertEquals(KRS, registry.krs());
        assertEquals(REGON, registry.regon());
        assertEquals(BDO, registry.bdo());
    }

    @Test
    void fa3_footer_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/FTR/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var stopka = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Stopka();
                    var first = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Stopka.Informacje();
                    first.setStopkaFaktury(NOTE_ONE);
                    var second = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Stopka.Informacje();
                    second.setStopkaFaktury(NOTE_TWO);
                    stopka.getInformacje().add(first);
                    stopka.getInformacje().add(second);
                    var registry = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Stopka.Rejestry();
                    registry.setPelnaNazwa(FULL_NAME);
                    registry.setKRS(KRS);
                    registry.setREGON(REGON);
                    registry.setBDO(BDO);
                    stopka.getRejestry().add(registry);
                    faktura.setStopka(stopka);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        assertFooter(Fa3InvoiceDocument.from(xml).footer());
    }

    @Test
    void fa2_footer_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/FTR/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var stopka = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Stopka();
                    var first = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Stopka.Informacje();
                    first.setStopkaFaktury(NOTE_ONE);
                    var second = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Stopka.Informacje();
                    second.setStopkaFaktury(NOTE_TWO);
                    stopka.getInformacje().add(first);
                    stopka.getInformacje().add(second);
                    var registry = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Stopka.Rejestry();
                    registry.setPelnaNazwa(FULL_NAME);
                    registry.setKRS(KRS);
                    registry.setREGON(REGON);
                    registry.setBDO(BDO);
                    stopka.getRejestry().add(registry);
                    faktura.setStopka(stopka);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        assertFooter(Fa2InvoiceDocument.from(xml).footer());
    }
}
