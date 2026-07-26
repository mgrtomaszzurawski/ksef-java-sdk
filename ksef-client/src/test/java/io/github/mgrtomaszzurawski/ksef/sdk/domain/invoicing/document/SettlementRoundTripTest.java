/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceSettlement;
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

/**
 * Round-trip guard for the {@code Fa/Rozliczenie} tree, surfaced as the
 * {@link InvoiceSettlement} nested record on both documents. The block is
 * set through the JAXB escape hatch (the builder does not surface it),
 * XSD-validated, and read back through {@code settlement()}.
 *
 * <p>{@code DoZaplaty} / {@code DoRozliczenia} are an XSD choice, so FA(3)
 * exercises the amount-payable branch and FA(2) the overpayment branch.
 */
class SettlementRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final BigDecimal GROSS = new BigDecimal("123.00");
    private static final BigDecimal CHARGE = new BigDecimal("10.00");
    private static final BigDecimal DEDUCTION = new BigDecimal("5.00");
    private static final BigDecimal DUE = new BigDecimal("128.00");
    private static final BigDecimal OVERPAID = new BigDecimal("7.00");

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

    private static void assertChargesAndDeductions(InvoiceSettlement settlement) {
        assertEquals(1, settlement.charges().size());
        assertEquals(0, CHARGE.compareTo(settlement.charges().get(0).amount()));
        assertEquals("packaging", settlement.charges().get(0).reason());
        assertEquals(0, CHARGE.compareTo(settlement.chargesTotal()));
        assertEquals(1, settlement.deductions().size());
        assertEquals(0, DEDUCTION.compareTo(settlement.deductions().get(0).amount()));
        assertEquals("advance", settlement.deductions().get(0).reason());
        assertEquals(0, DEDUCTION.compareTo(settlement.deductionsTotal()));
    }

    @Test
    void fa3_settlement_amountDueBranch_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/SET/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(GROSS).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var r = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Rozliczenie();
                    var ob = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Rozliczenie.Obciazenia();
                    ob.setKwota(CHARGE);
                    ob.setPowod("packaging");
                    r.getObciazenia().add(ob);
                    r.setSumaObciazen(CHARGE);
                    var od = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Rozliczenie.Odliczenia();
                    od.setKwota(DEDUCTION);
                    od.setPowod("advance");
                    r.getOdliczenia().add(od);
                    r.setSumaOdliczen(DEDUCTION);
                    r.setDoZaplaty(DUE);
                    faktura.getFa().setRozliczenie(r);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        InvoiceSettlement settlement = Fa3InvoiceDocument.from(xml).settlement();

        assertChargesAndDeductions(settlement);
        assertEquals(0, DUE.compareTo(settlement.amountDue()));
        assertNull(settlement.amountToSettle());
    }

    @Test
    void fa2_settlement_overpaymentBranch_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/SET/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(GROSS).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var r = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Rozliczenie();
                    var ob = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Rozliczenie.Obciazenia();
                    ob.setKwota(CHARGE);
                    ob.setPowod("packaging");
                    r.getObciazenia().add(ob);
                    r.setSumaObciazen(CHARGE);
                    var od = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Rozliczenie.Odliczenia();
                    od.setKwota(DEDUCTION);
                    od.setPowod("advance");
                    r.getOdliczenia().add(od);
                    r.setSumaOdliczen(DEDUCTION);
                    r.setDoRozliczenia(OVERPAID);
                    faktura.getFa().setRozliczenie(r);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        InvoiceSettlement settlement = Fa2InvoiceDocument.from(xml).settlement();

        assertChargesAndDeductions(settlement);
        assertNull(settlement.amountDue());
        assertEquals(0, OVERPAID.compareTo(settlement.amountToSettle()));
    }
}
