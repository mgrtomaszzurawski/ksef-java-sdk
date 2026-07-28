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
import java.util.function.Consumer;
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
 * <p>The read mappers are hand-duplicated per schema, so both forms exercise
 * every vehicle-category arm of the item choice with DISTINCT values: a land
 * vehicle (mileage + one of VIN / body / chassis / frame + type), a vessel
 * ({@code P_22C} + hull) and an aircraft ({@code P_22D} + factory number).
 * The three adjacent same-typed land identifiers ({@code P_22B2}/{@code P_22B3}/
 * {@code P_22B4}) each get a dedicated item so a swap among them is caught.
 * {@code P_42_5} is set to 1 (FA(3)) and 2 (FA(2)) so a hard-coded mapping
 * would fail. A third case drives the negative {@code P_22N} arm.
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
            fa3Item(int line, Consumer<io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura
                    .Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu> spec) {
        var item = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura
                .Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu();
        item.setP22A(greg(ADMISSION_DATE));
        item.setPNrWierszaNST(BigInteger.valueOf(line));
        spec.accept(item);
        return item;
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu
            fa2Item(int line, Consumer<io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura
                    .Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu> spec) {
        var item = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura
                .Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu();
        item.setP22A(greg(ADMISSION_DATE));
        item.setPNrWierszaNST(BigInteger.valueOf(line));
        spec.accept(item);
        return item;
    }

    @Test
    void fa3_allVehicleArmsAndLandIdentifiers_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/NST/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var node = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura
                            .Fa.Adnotacje.NoweSrodkiTransportu();
                    node.setP22(YES);
                    node.setP425(YES);
                    node.getNowySrodekTransportu().add(fa3Item(1, it -> {
                        it.setP22BMK("Tesla");
                        it.setP22BMD("Model 3");
                        it.setP22BK("Red");
                        it.setP22BNR("WZ12345");
                        it.setP22BRP("2026");
                        it.setP22B("150");
                        it.setP22B1("VIN123ABC");
                        it.setP22BT("Osobowy");
                    }));
                    node.getNowySrodekTransportu().add(fa3Item(2, it -> {
                        it.setP22B("160");
                        it.setP22B2("BODY-11");
                    }));
                    node.getNowySrodekTransportu().add(fa3Item(3, it -> {
                        it.setP22B("170");
                        it.setP22B3("CHASSIS-22");
                    }));
                    node.getNowySrodekTransportu().add(fa3Item(4, it -> {
                        it.setP22B("180");
                        it.setP22B4("FRAME-33");
                    }));
                    node.getNowySrodekTransportu().add(fa3Item(5, it -> {
                        it.setP22C("1200");
                        it.setP22C1("HULL-77");
                    }));
                    node.getNowySrodekTransportu().add(fa3Item(6, it -> {
                        it.setP22A(greg(ADMISSION_DATE_2));
                        it.setP22BMK("Cessna");
                        it.setP22D("500");
                        it.setP22D1("FN-999");
                    }));
                    faktura.getFa().getAdnotacje().setNoweSrodkiTransportu(node);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        NewMeansOfTransport nst = Fa3InvoiceDocument.from(xml).newMeansOfTransport();

        assertEquals(Boolean.TRUE, nst.intraCommunitySupply());
        assertEquals(Boolean.TRUE, nst.article42Paragraph5());
        assertNull(nst.noIntraCommunitySupply());
        assertEquals(6, nst.items().size());

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
        assertNull(land.bodyNumber());
        assertNull(land.chassisNumber());
        assertNull(land.frameNumber());
        assertNull(land.vesselWorkingHours());
        assertNull(land.aircraftWorkingHours());

        assertLandIdentifier(nst.items().get(1), "160", null, "BODY-11", null, null);
        assertLandIdentifier(nst.items().get(2), "170", null, null, "CHASSIS-22", null);
        assertLandIdentifier(nst.items().get(3), "180", null, null, null, "FRAME-33");

        NewTransportItem vessel = nst.items().get(4);
        assertEquals("1200", vessel.vesselWorkingHours());
        assertEquals("HULL-77", vessel.hullNumber());
        assertNull(vessel.mileage());
        assertNull(vessel.aircraftWorkingHours());

        NewTransportItem aircraft = nst.items().get(5);
        assertEquals(ADMISSION_DATE_2, aircraft.admissionDate());
        assertEquals("Cessna", aircraft.make());
        assertEquals("500", aircraft.aircraftWorkingHours());
        assertEquals("FN-999", aircraft.factoryNumber());
        assertNull(aircraft.mileage());
        assertNull(aircraft.vesselWorkingHours());
    }

    @Test
    void fa2_allVehicleArmsDistinctFlags_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/NST/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var node = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura
                            .Fa.Adnotacje.NoweSrodkiTransportu();
                    node.setP22(YES);
                    node.setP425(NO);
                    node.getNowySrodekTransportu().add(fa2Item(11, it -> {
                        it.setP22BMK("Volvo");
                        it.setP22BMD("FH16");
                        it.setP22BK("Blue");
                        it.setP22BNR("KR99999");
                        it.setP22BRP("2025");
                        it.setP22B("250");
                        it.setP22B1("VINFA2XYZ");
                        it.setP22BT("Ciezarowy");
                    }));
                    node.getNowySrodekTransportu().add(fa2Item(12, it -> {
                        it.setP22C("1300");
                        it.setP22C1("HULL-88");
                    }));
                    node.getNowySrodekTransportu().add(fa2Item(13, it -> {
                        it.setP22D("600");
                        it.setP22D1("FN-222");
                        it.setP22BMK("Boeing");
                    }));
                    faktura.getFa().getAdnotacje().setNoweSrodkiTransportu(node);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        NewMeansOfTransport nst = Fa2InvoiceDocument.from(xml).newMeansOfTransport();

        assertEquals(Boolean.TRUE, nst.intraCommunitySupply());
        assertEquals(Boolean.FALSE, nst.article42Paragraph5());
        assertNull(nst.noIntraCommunitySupply());
        assertEquals(3, nst.items().size());

        NewTransportItem land = nst.items().get(0);
        assertEquals(11, land.invoiceLineNumber());
        assertEquals("Volvo", land.make());
        assertEquals("FH16", land.model());
        assertEquals("Blue", land.color());
        assertEquals("KR99999", land.registrationNumber());
        assertEquals("2025", land.productionYear());
        assertEquals("250", land.mileage());
        assertEquals("VINFA2XYZ", land.vin());
        assertEquals("Ciezarowy", land.vehicleType());
        assertNull(land.vesselWorkingHours());
        assertNull(land.aircraftWorkingHours());

        NewTransportItem vessel = nst.items().get(1);
        assertEquals("1300", vessel.vesselWorkingHours());
        assertEquals("HULL-88", vessel.hullNumber());
        assertNull(vessel.mileage());
        assertNull(vessel.aircraftWorkingHours());

        NewTransportItem aircraft = nst.items().get(2);
        assertEquals("Boeing", aircraft.make());
        assertEquals("600", aircraft.aircraftWorkingHours());
        assertEquals("FN-222", aircraft.factoryNumber());
        assertNull(aircraft.mileage());
        assertNull(aircraft.vesselWorkingHours());
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
        NewMeansOfTransport nst = Fa3InvoiceDocument.from(xml).newMeansOfTransport();

        assertTrue(nst.noIntraCommunitySupply());
        assertNull(nst.intraCommunitySupply());
        assertNull(nst.article42Paragraph5());
        assertTrue(nst.items().isEmpty());
    }

    private static void assertLandIdentifier(NewTransportItem item, String mileage, String vin,
                                             String bodyNumber, String chassisNumber, String frameNumber) {
        assertEquals(mileage, item.mileage());
        assertEquals(vin, item.vin());
        assertEquals(bodyNumber, item.bodyNumber());
        assertEquals(chassisNumber, item.chassisNumber());
        assertEquals(frameNumber, item.frameNumber());
    }
}
