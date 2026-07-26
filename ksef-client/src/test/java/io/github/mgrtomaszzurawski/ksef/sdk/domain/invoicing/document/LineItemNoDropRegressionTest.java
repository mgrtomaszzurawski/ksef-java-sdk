/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.testfixtures.Fa3InvoiceFixtures;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression guard for the silent line-item drop defect: a {@code FaWiersz}
 * carrying only a gross amount ({@code P_11A}) — legal under the FA(2)/FA(3)
 * XSD where {@code P_7}, {@code P_11} and {@code P_12} are all
 * {@code minOccurs="0"} — was dropped entirely by the read-side mapper,
 * silently losing an invoice line.
 *
 * <p>Each test round-trips a schema-valid invoice through JAXB, appends a
 * gross-only line to the {@code Fa} block, marshals it back, and parses it
 * through the document. Every {@code FaWiersz} must survive as exactly one
 * {@link InvoiceLineItem} with the absent fields exposed as {@code null}.
 */
class LineItemNoDropRegressionTest {

    private static final BigDecimal GROSS_ONLY_AMOUNT = new BigDecimal("50.00");
    private static final BigInteger SECOND_ROW = BigInteger.valueOf(2);

    @Test
    void fa3_whenLineHasGrossOnly_lineIsNotDroppedAndNetIsNull() throws Exception {
        io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura faktura =
                unmarshalFa3(Fa3InvoiceFixtures.minimalValid().xml());

        io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.FaWiersz grossOnly =
                new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.FaWiersz();
        grossOnly.setNrWierszaFa(SECOND_ROW);
        grossOnly.setP11A(GROSS_ONLY_AMOUNT);
        faktura.getFa().getFaWiersz().add(grossOnly);

        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(marshalFa3(faktura));

        List<InvoiceLineItem> lines = doc.lineItems();
        assertEquals(2, lines.size(), "gross-only line must not be dropped");
        InvoiceLineItem second = lineByRow(lines, 2);
        assertNull(second.netAmount(), "P_11 absent -> netAmount null");
        assertNull(second.vatRate(), "P_12 absent -> vatRate null");
        assertNull(second.description(), "P_7 absent -> description null");
        assertEquals(0, GROSS_ONLY_AMOUNT.compareTo(second.grossAmount()),
                "P_11A must round-trip on the gross-only line");
    }

    @Test
    void fa2_whenLineHasGrossOnly_lineIsNotDroppedAndNetIsNull() throws Exception {
        io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura faktura =
                unmarshalFa2(minimalFa2Xml());

        io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.FaWiersz grossOnly =
                new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.FaWiersz();
        grossOnly.setNrWierszaFa(SECOND_ROW);
        grossOnly.setP11A(GROSS_ONLY_AMOUNT);
        faktura.getFa().getFaWiersz().add(grossOnly);

        Fa2InvoiceDocument doc = Fa2InvoiceDocument.from(marshalFa2(faktura));

        List<InvoiceLineItem> lines = doc.lineItems();
        assertEquals(2, lines.size(), "gross-only line must not be dropped");
        InvoiceLineItem second = lineByRow(lines, 2);
        assertNull(second.netAmount(), "P_11 absent -> netAmount null");
        assertNull(second.vatRate(), "P_12 absent -> vatRate null");
        assertNull(second.description(), "P_7 absent -> description null");
        assertEquals(0, GROSS_ONLY_AMOUNT.compareTo(second.grossAmount()),
                "P_11A must round-trip on the gross-only line");
    }

    private static InvoiceLineItem lineByRow(List<InvoiceLineItem> lines, int rowNumber) {
        return lines.stream()
                .filter(line -> line.rowNumber() == rowNumber)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no line with rowNumber " + rowNumber));
    }

    private static byte[] minimalFa2Xml() {
        return Fa2Invoice.builder()
                .invoiceNumber("FA/2026/FIXTURE/0001")
                .issueDate(LocalDate.of(2026, 5, 11))
                .seller(new InvoiceParty("1111111111", "Acme", "00-001", "Warszawa", "Marszalkowska", "10", null))
                .buyer(new InvoiceParty("9876543210", "Customer", "00-002", "Krakow", null, "5", null))
                .totalGrossAmount(new BigDecimal("123.00"))
                .addLineItem(InvoiceLineItem.builder().rowNumber(1).description("Consulting")
                        .unitOfMeasure("szt.").quantity(BigDecimal.ONE)
                        .netUnitPrice(new BigDecimal("100.00")).netAmount(new BigDecimal("100.00"))
                        .vatRate("23").build())
                .build()
                .xml();
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura unmarshalFa3(byte[] xml) throws Exception {
        Unmarshaller unmarshaller =
                JAXBContext.newInstance(io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.class).createUnmarshaller();
        return (io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura)
                unmarshaller.unmarshal(new ByteArrayInputStream(xml));
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura unmarshalFa2(byte[] xml) throws Exception {
        Unmarshaller unmarshaller =
                JAXBContext.newInstance(io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.class).createUnmarshaller();
        return (io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura)
                unmarshaller.unmarshal(new ByteArrayInputStream(xml));
    }

    private static byte[] marshalFa3(io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura faktura) throws Exception {
        Marshaller marshaller =
                JAXBContext.newInstance(io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.class).createMarshaller();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(faktura, out);
        return out.toByteArray();
    }

    private static byte[] marshalFa2(io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura faktura) throws Exception {
        Marshaller marshaller =
                JAXBContext.newInstance(io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.class).createMarshaller();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(faktura, out);
        return out.toByteArray();
    }
}
