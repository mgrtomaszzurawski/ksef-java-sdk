/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AttachmentBlock;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AttachmentTable;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceAttachment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Round-trip guard for the {@code Faktura/Zalacznik} attachment tree,
 * surfaced as {@link InvoiceAttachment} on the FA(3) document. Set
 * through the JAXB escape hatch (the builder does not surface it),
 * XSD-validated, and read back through {@code attachment()}.
 *
 * <p>{@code Zalacznik} is an FA(3)-only node — the FA(2) schema has no
 * attachment — so this guard is FA(3) only.
 */
class AttachmentRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);

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
    void fa3_attachmentBlockWithTable_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/ZAL/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var attachment = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik();
                    var block = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych();
                    block.setZNaglowek("Delivery breakdown");
                    var meta = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.MetaDane();
                    meta.setZKlucz("source");
                    meta.setZWartosc("warehouse-A");
                    block.getMetaDane().add(meta);
                    var text = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tekst();
                    text.getAkapit().add("First paragraph.");
                    text.getAkapit().add("Second paragraph.");
                    block.setTekst(text);
                    var table = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela();
                    var tMeta =
                            new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.TMetaDane();
                    tMeta.setTKlucz("currency");
                    tMeta.setTWartosc("PLN");
                    table.getTMetaDane().add(tMeta);
                    table.setOpis("Line totals");
                    var header =
                            new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.TNaglowek();
                    header.getKol().add(column("Item", "txt"));
                    header.getKol().add(column("Amount", "dec"));
                    table.setTNaglowek(header);
                    table.getWiersz().add(row("Widget", "10.00"));
                    table.getWiersz().add(row("Gadget", "20.00"));
                    var sum = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.Suma();
                    sum.getSKom().add("Total");
                    sum.getSKom().add("30.00");
                    table.setSuma(sum);
                    block.getTabela().add(table);
                    attachment.getBlokDanych().add(block);
                    faktura.setZalacznik(attachment);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        InvoiceAttachment attachment = Fa3InvoiceDocument.from(xml).attachment();

        assertEquals(1, attachment.blocks().size());
        AttachmentBlock block = attachment.blocks().get(0);
        assertEquals("Delivery breakdown", block.header());
        assertEquals(1, block.metadata().size());
        assertEquals("source", block.metadata().get(0).key());
        assertEquals("warehouse-A", block.metadata().get(0).value());
        assertEquals(List.of("First paragraph.", "Second paragraph."), block.paragraphs());
        assertEquals(1, block.tables().size());

        AttachmentTable table = block.tables().get(0);
        assertEquals(1, table.metadata().size());
        assertEquals("currency", table.metadata().get(0).key());
        assertEquals("PLN", table.metadata().get(0).value());
        assertEquals("Line totals", table.description());
        assertEquals(2, table.columns().size());
        assertEquals("Item", table.columns().get(0).label());
        assertEquals("txt", table.columns().get(0).type());
        assertEquals("Amount", table.columns().get(1).label());
        assertEquals("dec", table.columns().get(1).type());
        assertEquals(2, table.rows().size());
        assertEquals(List.of("Widget", "10.00"), table.rows().get(0).cells());
        assertEquals(List.of("Gadget", "20.00"), table.rows().get(1).cells());
        assertEquals(List.of("Total", "30.00"), table.totals());
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.TNaglowek.Kol column(
            String label, String type) {
        var kol = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.TNaglowek.Kol();
        var content =
                new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.TNaglowek.Kol.NKom();
        content.setValue(label);
        kol.setNKom(content);
        kol.setTyp(type);
        return kol;
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.Wiersz row(
            String... cells) {
        var wiersz = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Zalacznik.BlokDanych.Tabela.Wiersz();
        for (String cell : cells) {
            wiersz.getWKom().add(cell);
        }
        return wiersz;
    }
}
