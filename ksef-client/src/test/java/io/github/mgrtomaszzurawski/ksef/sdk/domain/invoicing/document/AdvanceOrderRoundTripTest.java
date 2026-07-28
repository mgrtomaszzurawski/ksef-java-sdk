/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AdvanceOrder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OrderLine;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Round-trip guard for the {@code Fa/Zamowienie} advance-order tree,
 * surfaced as {@link AdvanceOrder} + {@link OrderLine} on both documents.
 * Set through the JAXB escape hatch (the builder does not surface it),
 * XSD-validated, and read back through {@code advanceOrder()}.
 *
 * <p>FA(3) exercises the full {@code ZamowienieWiersz} scalar surface with
 * distinct values (including the coded {@code GTUZ}/{@code ProceduraZ} and
 * the {@code TWybor1} markers); FA(2) exercises a distinct subset so the
 * hand-duplicated FA(2) mapper is guarded against a swapped accessor.
 */
class AdvanceOrderRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final BigDecimal ORDER_VALUE = new BigDecimal("123.00");

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
    void fa3_advanceOrderFullLine_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/ZAM/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(ORDER_VALUE).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var order = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Zamowienie();
                    order.setWartoscZamowienia(ORDER_VALUE);
                    var line = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Zamowienie.ZamowienieWiersz();
                    line.setNrWierszaZam(BigInteger.ONE);
                    line.setUUIDZ("uuid-zam-1");
                    line.setP7Z("Ordered widget");
                    line.setIndeksZ("IDX-1");
                    line.setGTINZ("05901234567890");
                    line.setPKWiUZ("12.34.56");
                    line.setCNZ("8471");
                    line.setPKOBZ("1122");
                    line.setP8AZ("szt.");
                    line.setP8BZ(new BigDecimal("2"));
                    line.setP9AZ(new BigDecimal("50.00"));
                    line.setP11NettoZ(new BigDecimal("100.00"));
                    line.setP11VatZ(new BigDecimal("23.00"));
                    line.setP12Z("23");
                    line.setP12ZXII(new BigDecimal("7.00"));
                    line.setP12ZZal15((byte) 1);
                    line.setGTUZ(io.github.mgrtomaszzurawski.ksef.xml.fa3.TGTU.GTU_01);
                    line.setProceduraZ(io.github.mgrtomaszzurawski.ksef.xml.fa3.TOznaczenieProceduryZ.WSTO_EE);
                    line.setKwotaAkcyzyZ(new BigDecimal("4.00"));
                    line.setStanPrzedZ((byte) 1);
                    order.getZamowienieWiersz().add(line);
                    faktura.getFa().setZamowienie(order);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        AdvanceOrder advanceOrder = Fa3InvoiceDocument.from(xml).advanceOrder();

        assertEquals(0, ORDER_VALUE.compareTo(advanceOrder.orderValue()));
        assertEquals(1, advanceOrder.lines().size());
        OrderLine line = advanceOrder.lines().get(0);
        assertEquals(1, line.rowNumber());
        assertEquals("uuid-zam-1", line.uuid());
        assertEquals("Ordered widget", line.description());
        assertEquals("IDX-1", line.index());
        assertEquals("05901234567890", line.gtin());
        assertEquals("12.34.56", line.pkwiu());
        assertEquals("8471", line.cnCode());
        assertEquals("1122", line.pkobCode());
        assertEquals("szt.", line.unitOfMeasure());
        assertEquals(0, new BigDecimal("2").compareTo(line.quantity()));
        assertEquals(0, new BigDecimal("50.00").compareTo(line.netUnitPrice()));
        assertEquals(0, new BigDecimal("100.00").compareTo(line.netAmount()));
        assertEquals(0, new BigDecimal("23.00").compareTo(line.vatAmount()));
        assertEquals("23", line.vatRate());
        assertEquals(0, new BigDecimal("7.00").compareTo(line.valueAddedTaxRate()));
        assertEquals(Boolean.TRUE, line.annex15());
        assertEquals("GTU_01", line.gtuCode());
        assertEquals("WSTO_EE", line.procedureMarking());
        assertEquals(0, new BigDecimal("4.00").compareTo(line.exciseAmount()));
        assertEquals(Boolean.TRUE, line.correctionStateBefore());
    }

    @Test
    void fa2_advanceOrderDistinctSubset_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/ZAM/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(ORDER_VALUE).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var order = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Zamowienie();
                    order.setWartoscZamowienia(ORDER_VALUE);
                    var line = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Zamowienie.ZamowienieWiersz();
                    line.setNrWierszaZam(BigInteger.valueOf(2));
                    line.setP7Z("Ordered gadget");
                    line.setP8AZ("kg");
                    line.setP8BZ(new BigDecimal("3"));
                    line.setP11NettoZ(new BigDecimal("90.00"));
                    line.setP12Z("8");
                    line.setGTUZ(io.github.mgrtomaszzurawski.ksef.xml.fa2.TGTU.GTU_03);
                    line.setStanPrzedZ((byte) 1);
                    order.getZamowienieWiersz().add(line);
                    faktura.getFa().setZamowienie(order);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        AdvanceOrder advanceOrder = Fa2InvoiceDocument.from(xml).advanceOrder();

        assertEquals(0, ORDER_VALUE.compareTo(advanceOrder.orderValue()));
        assertEquals(1, advanceOrder.lines().size());
        OrderLine line = advanceOrder.lines().get(0);
        assertEquals(2, line.rowNumber());
        assertEquals("Ordered gadget", line.description());
        assertEquals("kg", line.unitOfMeasure());
        assertEquals(0, new BigDecimal("3").compareTo(line.quantity()));
        assertEquals(0, new BigDecimal("90.00").compareTo(line.netAmount()));
        assertEquals("8", line.vatRate());
        assertEquals("GTU_03", line.gtuCode());
        assertEquals(Boolean.TRUE, line.correctionStateBefore());
        assertNull(line.uuid());
        assertNull(line.annex15());
        assertNull(line.procedureMarking());
    }
}
