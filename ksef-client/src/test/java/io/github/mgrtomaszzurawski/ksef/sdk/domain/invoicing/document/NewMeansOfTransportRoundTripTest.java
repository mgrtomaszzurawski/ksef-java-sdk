/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.NewMeansOfTransport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.NewTransportItem;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip guard for the {@code Fa/Adnotacje/NoweSrodkiTransportu} node
 * (intra-Community supply of new means of transport, art. 42 ust. 5):
 * the {@code P_22} / {@code P_42_5} / {@code P_22N} flags and the
 * {@code NowySrodekTransportu} item list ({@link NewMeansOfTransport} /
 * {@link NewTransportItem}). Set through the JAXB escape hatch, XSD-validated,
 * and read back.
 *
 * <p>FA(3) exercises the positive branch with a land vehicle and an aircraft
 * item ({@code P_42_5} = 1); FA(2) exercises a distinct positive branch with a
 * vessel item ({@code P_42_5} = 2) — so both hand-duplicated read paths are
 * guarded with distinct values and all three vehicle-category arms of the
 * item choice are covered. A third case drives the negative {@code P_22N} arm.
 */
class NewMeansOfTransportRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final LocalDate ADMISSION_DATE = LocalDate.of(2026, 3, 15);
    private static final LocalDate ADMISSION_DATE_2 = LocalDate.of(2026, 2, 1);
    private static final byte YES = 1;
    private static final byte NO = 2;
    private static final DatatypeFactory DTF = datatypeFactory();

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

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu
            fa3LandItem() {
        var item = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura
                .Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu();
        item.setP22A(greg(ADMISSION_DATE));
        item.setPNrWierszaNST(BigInteger.ONE);
        item.setP22BMK("Tesla");
        item.setP22BMD("Model 3");
        item.setP22BK("Red");
        item.setP22BNR("WZ12345");
        item.setP22BRP("2026");
        item.setP22B("150");
        item.setP22B1("VIN123ABC");
        item.setP22BT("Osobowy");
        return item;
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu
            fa3AircraftItem() {
        var item = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura
                .Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu();
        item.setP22A(greg(ADMISSION_DATE_2));
        item.setPNrWierszaNST(BigInteger.TWO);
        item.setP22BMK("Cessna");
        item.setP22D("500");
        item.setP22D1("FN-999");
        return item;
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu
            fa2VesselItem() {
        var item = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura
                .Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu();
        item.setP22A(greg(ADMISSION_DATE));
        item.setPNrWierszaNST(BigInteger.valueOf(3));
        item.setP22BMK("Bavaria");
        item.setP22BMD("C42");
        item.setP22C("1200");
        item.setP22C1("HULL-77");
        return item;
    }

    @Test
    void fa3_landAndAircraftItems_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/NST/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var node = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura
                            .Fa.Adnotacje.NoweSrodkiTransportu();
                    node.setP22(YES);
                    node.setP425(YES);
                    node.getNowySrodekTransportu().add(fa3LandItem());
                    node.getNowySrodekTransportu().add(fa3AircraftItem());
                    faktura.getFa().getAdnotacje().setNoweSrodkiTransportu(node);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        NewMeansOfTransport nst = document.newMeansOfTransport();
        assertEquals(Boolean.TRUE, nst.intraCommunitySupply());
        assertEquals(Boolean.TRUE, nst.article42Paragraph5());
        assertNull(nst.noIntraCommunitySupply());
        assertEquals(2, nst.items().size());

        NewTransportItem land = nst.items().get(0);
        assertEquals(ADMISSION_DATE, land.admissionDate());
        assertEquals(1, land.invoiceLineNumber());
        assertEquals("Tesla", land.make());
        assertEquals("Model 3", land.model());
        assertEquals("Red", land.color());
        assertEquals("WZ12345", land.registrationNumber());
        assertEquals("2026", land.productionYear());
        assertEquals("150", land.mileage());
        assertEquals("VIN123ABC", land.vin());
        assertEquals("Osobowy", land.vehicleType());
        assertNull(land.vesselWorkingHours());
        assertNull(land.aircraftWorkingHours());

        NewTransportItem aircraft = nst.items().get(1);
        assertEquals(ADMISSION_DATE_2, aircraft.admissionDate());
        assertEquals(2, aircraft.invoiceLineNumber());
        assertEquals("Cessna", aircraft.make());
        assertEquals("500", aircraft.aircraftWorkingHours());
        assertEquals("FN-999", aircraft.factoryNumber());
        assertNull(aircraft.mileage());
        assertNull(aircraft.vin());
    }

    @Test
    void fa2_vesselItemDistinctFlags_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/NST/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var node = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura
                            .Fa.Adnotacje.NoweSrodkiTransportu();
                    node.setP22(YES);
                    node.setP425(NO);
                    node.getNowySrodekTransportu().add(fa2VesselItem());
                    faktura.getFa().getAdnotacje().setNoweSrodkiTransportu(node);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        Fa2InvoiceDocument document = Fa2InvoiceDocument.from(xml);

        NewMeansOfTransport nst = document.newMeansOfTransport();
        assertEquals(Boolean.TRUE, nst.intraCommunitySupply());
        assertEquals(Boolean.FALSE, nst.article42Paragraph5());
        assertNull(nst.noIntraCommunitySupply());
        assertEquals(1, nst.items().size());

        NewTransportItem vessel = nst.items().get(0);
        assertEquals(ADMISSION_DATE, vessel.admissionDate());
        assertEquals(3, vessel.invoiceLineNumber());
        assertEquals("Bavaria", vessel.make());
        assertEquals("C42", vessel.model());
        assertEquals("1200", vessel.vesselWorkingHours());
        assertEquals("HULL-77", vessel.hullNumber());
        assertNull(vessel.mileage());
        assertNull(vessel.aircraftWorkingHours());
    }

    @Test
    void fa3_negativeMarkerBranch_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/NST/0003").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var node = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura
                            .Fa.Adnotacje.NoweSrodkiTransportu();
                    node.setP22N(YES);
                    faktura.getFa().getAdnotacje().setNoweSrodkiTransportu(node);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        NewMeansOfTransport nst = document.newMeansOfTransport();
        assertTrue(nst.noIntraCommunitySupply());
        assertNull(nst.intraCommunitySupply());
        assertNull(nst.article42Paragraph5());
        assertTrue(nst.items().isEmpty());
    }
}
