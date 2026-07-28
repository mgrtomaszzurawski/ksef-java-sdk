/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.MarginScheme;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip guard for the {@code Fa/Adnotacje} marker set: the flat
 * cash-method / self-billing / reverse-charge / triangular markers
 * ({@code P_16}/{@code P_17}/{@code P_18}/{@code P_23}), the margin scheme
 * ({@code PMarzy} → {@link MarginScheme}) and the exemption choice
 * ({@code Zwolnienie}, {@code P_19} vs {@code P_19N}). Set through the
 * JAXB escape hatch, XSD-validated, and read back.
 *
 * <p>FA(3) exercises all four flat markers set, the travel-agency margin
 * branch and the exempt branch; FA(2) exercises a distinct marker pattern,
 * the used-goods margin branch and the explicitly-not-exempt branch — so
 * both hand-duplicated read paths are guarded.
 */
class AnnotationMarkersRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final byte YES = 1;
    private static final byte NO = 2;

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
    void fa3_allMarkersMarginTravelAndExempt_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/ADN/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var adn = faktura.getFa().getAdnotacje();
                    adn.setP16(YES);
                    adn.setP17(YES);
                    adn.setP18(YES);
                    adn.setP23(YES);
                    var margin = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Adnotacje.PMarzy();
                    margin.setPPMarzy(YES);
                    margin.setPPMarzy2(YES);
                    adn.setPMarzy(margin);
                    var zw = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Adnotacje.Zwolnienie();
                    zw.setP19(YES);
                    zw.setP19A("art. 43 ust. 1 pkt 1 ustawy");
                    adn.setZwolnienie(zw);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        assertTrue(document.cashMethod());
        assertTrue(document.selfBilling());
        assertTrue(document.reverseCharge());
        assertTrue(document.simplifiedTriangular());

        MarginScheme margin = document.marginScheme();
        assertEquals(Boolean.TRUE, margin.present());
        assertEquals(Boolean.TRUE, margin.travelAgency());
        assertNull(margin.usedGoods());
        assertNull(margin.none());

        assertEquals("art. 43 ust. 1 pkt 1 ustawy", document.vatExemption().legalBasisArticle());
    }

    @Test
    void fa2_distinctMarkersMarginUsedGoodsAndNotExempt_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/ADN/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var adn = faktura.getFa().getAdnotacje();
                    adn.setP16(YES);
                    adn.setP17(NO);
                    adn.setP18(YES);
                    adn.setP23(NO);
                    var margin = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Adnotacje.PMarzy();
                    margin.setPPMarzy(YES);
                    margin.setPPMarzy31(YES);
                    adn.setPMarzy(margin);
                    var zw = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Adnotacje.Zwolnienie();
                    zw.setP19N(YES);
                    adn.setZwolnienie(zw);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        Fa2InvoiceDocument document = Fa2InvoiceDocument.from(xml);

        assertTrue(document.cashMethod());
        assertFalse(document.selfBilling());
        assertTrue(document.reverseCharge());
        assertFalse(document.simplifiedTriangular());

        MarginScheme margin = document.marginScheme();
        assertEquals(Boolean.TRUE, margin.present());
        assertEquals(Boolean.TRUE, margin.usedGoods());
        assertNull(margin.travelAgency());
        assertNull(margin.none());

        // P_19N (explicitly not exempt) must not surface a VatExemption.
        assertNull(document.vatExemption());
    }
}
