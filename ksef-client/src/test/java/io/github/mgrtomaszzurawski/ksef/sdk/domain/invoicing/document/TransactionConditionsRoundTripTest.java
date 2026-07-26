/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.TransactionConditions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.Transport;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Round-trip guard for the {@code Fa/WarunkiTransakcji} tree, surfaced as
 * the {@link TransactionConditions} nested record on both documents. The
 * block is set through the JAXB escape hatch (the builder does not
 * surface it), XSD-validated, and read back through
 * {@code transactionConditions()}.
 *
 * <p>Transport carries two XSD choices (means of transport, load type).
 * FA(3) exercises the coded branches ({@code RodzajTransportu},
 * {@code OpisLadunku}) with a NIP-identified carrier; FA(2) exercises the
 * "other" branches ({@code TransportInny} / {@code LadunekInny}) with a
 * carrier that has no tax identifier ({@code BrakID}).
 */
class TransactionConditionsRoundTripTest {

    private static final DatatypeFactory DTF = datatypeFactory();
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final LocalDate AGREEMENT_DATE = LocalDate.of(2026, 3, 1);
    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 3, 2);
    private static final OffsetDateTime TRANSPORT_START = OffsetDateTime.of(2026, 5, 10, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TRANSPORT_END = OffsetDateTime.of(2026, 5, 10, 16, 30, 0, 0, ZoneOffset.UTC);
    private static final BigDecimal CONTRACT_RATE = new BigDecimal("4.3500");

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

    private static XMLGregorianCalendar gregDateTime(OffsetDateTime moment) {
        return DTF.newXMLGregorianCalendar(moment.getYear(), moment.getMonthValue(), moment.getDayOfMonth(),
                moment.getHour(), moment.getMinute(), moment.getSecond(), 0, 0);
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

    private static void assertCommonConditions(TransactionConditions conditions) {
        assertNotNull(conditions);
        assertEquals(1, conditions.agreements().size());
        assertEquals(AGREEMENT_DATE, conditions.agreements().get(0).date());
        assertEquals("UM/2026/1", conditions.agreements().get(0).number());
        assertEquals(1, conditions.orders().size());
        assertEquals(ORDER_DATE, conditions.orders().get(0).date());
        assertEquals("ZAM/2026/1", conditions.orders().get(0).number());
        assertEquals(List.of("BATCH-A", "BATCH-B"), conditions.productBatchNumbers());
        assertEquals("DAP Warszawa", conditions.deliveryTerms());
        assertEquals(0, CONTRACT_RATE.compareTo(conditions.contractExchangeRate()));
        assertEquals("EUR", conditions.contractCurrency());
        assertEquals(Boolean.TRUE, conditions.intermediary());
        assertEquals(1, conditions.transports().size());
    }

    @Test
    void fa3_codedTransportAndNipCarrier_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/WT/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var warunki = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.WarunkiTransakcji();
                    var umowa = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.WarunkiTransakcji.Umowy();
                    umowa.setDataUmowy(greg(AGREEMENT_DATE));
                    umowa.setNrUmowy("UM/2026/1");
                    warunki.getUmowy().add(umowa);
                    var zamowienie = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.WarunkiTransakcji.Zamowienia();
                    zamowienie.setDataZamowienia(greg(ORDER_DATE));
                    zamowienie.setNrZamowienia("ZAM/2026/1");
                    warunki.getZamowienia().add(zamowienie);
                    warunki.getNrPartiiTowaru().add("BATCH-A");
                    warunki.getNrPartiiTowaru().add("BATCH-B");
                    warunki.setWarunkiDostawy("DAP Warszawa");
                    warunki.setKursUmowny(CONTRACT_RATE);
                    warunki.setWalutaUmowna(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodWaluty.EUR);
                    var transport = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.WarunkiTransakcji.Transport();
                    transport.setRodzajTransportu(BigInteger.valueOf(3));
                    var carrier =
                            new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.WarunkiTransakcji.Transport.Przewoznik();
                    var identity = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot2();
                    identity.setNIP("5555555555");
                    identity.setNazwa("Fast Logistics sp. z o.o.");
                    carrier.setDaneIdentyfikacyjne(identity);
                    carrier.setAdresPrzewoznika(addressFa3("PL", "ul. Spedycyjna 1", "02-000 Warszawa", "0000000000017"));
                    transport.setPrzewoznik(carrier);
                    transport.setNrZleceniaTransportu("TRZ/2026/1");
                    transport.setOpisLadunku(BigInteger.valueOf(2));
                    transport.setJednostkaOpakowania("paleta");
                    transport.setDataGodzRozpTransportu(gregDateTime(TRANSPORT_START));
                    transport.setDataGodzZakTransportu(gregDateTime(TRANSPORT_END));
                    transport.setWysylkaZ(addressFa3("PL", "Magazyn 1", null, null));
                    transport.getWysylkaPrzez().add(addressFa3("PL", "Hub Lodz", null, null));
                    transport.setWysylkaDo(addressFa3("DE", "Zielort 5", "10115 Berlin", null));
                    warunki.getTransport().add(transport);
                    warunki.setPodmiotPosredniczacy((byte) 1);
                    faktura.getFa().setWarunkiTransakcji(warunki);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        TransactionConditions conditions = Fa3InvoiceDocument.from(xml).transactionConditions();

        assertCommonConditions(conditions);
        Transport transport = conditions.transports().get(0);
        assertEquals(3, transport.transportType());
        assertNull(transport.otherTransport());
        assertNull(transport.otherTransportDescription());
        assertEquals(2, transport.loadType());
        assertNull(transport.otherLoad());
        assertEquals("TRZ/2026/1", transport.transportOrderNumber());
        assertEquals("paleta", transport.packagingUnit());
        assertEquals(TRANSPORT_START, transport.transportStart());
        assertEquals(TRANSPORT_END, transport.transportEnd());
        assertNotNull(transport.carrier());
        assertEquals("5555555555", transport.carrier().identity().nip());
        assertEquals("Fast Logistics sp. z o.o.", transport.carrier().identity().name());
        assertEquals("PL", transport.carrier().address().countryCode());
        assertEquals("ul. Spedycyjna 1", transport.carrier().address().addressLine1());
        assertEquals("0000000000017", transport.carrier().address().gln());
        assertEquals("PL", transport.shipFrom().countryCode());
        assertEquals(1, transport.shipVia().size());
        assertEquals("Hub Lodz", transport.shipVia().get(0).addressLine1());
        assertEquals("DE", transport.shipTo().countryCode());
        assertEquals("10115 Berlin", transport.shipTo().addressLine2());
    }

    @Test
    void fa2_otherTransportAndNoIdCarrier_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/WT/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var warunki = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.WarunkiTransakcji();
                    var umowa = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.WarunkiTransakcji.Umowy();
                    umowa.setDataUmowy(greg(AGREEMENT_DATE));
                    umowa.setNrUmowy("UM/2026/1");
                    warunki.getUmowy().add(umowa);
                    var zamowienie = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.WarunkiTransakcji.Zamowienia();
                    zamowienie.setDataZamowienia(greg(ORDER_DATE));
                    zamowienie.setNrZamowienia("ZAM/2026/1");
                    warunki.getZamowienia().add(zamowienie);
                    warunki.getNrPartiiTowaru().add("BATCH-A");
                    warunki.getNrPartiiTowaru().add("BATCH-B");
                    warunki.setWarunkiDostawy("DAP Warszawa");
                    warunki.setKursUmowny(CONTRACT_RATE);
                    warunki.setWalutaUmowna(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodWaluty.EUR);
                    var transport = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.WarunkiTransakcji.Transport();
                    transport.setTransportInny((byte) 1);
                    transport.setOpisInnegoTransportu("dron dostawczy");
                    var carrier =
                            new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.WarunkiTransakcji.Transport.Przewoznik();
                    var identity = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot2();
                    identity.setBrakID((byte) 1);
                    identity.setNazwa("Anonimowy Przewoznik");
                    carrier.setDaneIdentyfikacyjne(identity);
                    carrier.setAdresPrzewoznika(addressFa2("PL", "ul. Spedycyjna 1", "60-001 Poznan", "0000000000024"));
                    transport.setPrzewoznik(carrier);
                    transport.setNrZleceniaTransportu("TRZ/2026/1");
                    transport.setLadunekInny((byte) 1);
                    transport.setOpisInnegoLadunku("ladunek mieszany");
                    transport.setJednostkaOpakowania("paleta");
                    transport.setWysylkaZ(addressFa2("PL", "Magazyn 1", null, null));
                    transport.getWysylkaPrzez().add(addressFa2("PL", "Hub Lodz", null, null));
                    transport.setWysylkaDo(addressFa2("DE", "Zielort 5", "10115 Berlin", null));
                    warunki.getTransport().add(transport);
                    warunki.setPodmiotPosredniczacy((byte) 1);
                    faktura.getFa().setWarunkiTransakcji(warunki);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        TransactionConditions conditions = Fa2InvoiceDocument.from(xml).transactionConditions();

        assertCommonConditions(conditions);
        Transport transport = conditions.transports().get(0);
        assertNull(transport.transportType());
        assertEquals(Boolean.TRUE, transport.otherTransport());
        assertEquals("dron dostawczy", transport.otherTransportDescription());
        assertNull(transport.loadType());
        assertEquals(Boolean.TRUE, transport.otherLoad());
        assertEquals("ladunek mieszany", transport.otherLoadDescription());
        assertEquals("TRZ/2026/1", transport.transportOrderNumber());
        assertEquals("paleta", transport.packagingUnit());
        assertNotNull(transport.carrier());
        assertNull(transport.carrier().identity().nip());
        assertEquals(Boolean.TRUE, transport.carrier().identity().noTaxId());
        assertEquals("Anonimowy Przewoznik", transport.carrier().identity().name());
        // The FA(2) address mapping is hand-duplicated from the FA(3) one, so
        // assert every TAdres leaf here too (line1, line2, gln) to catch a
        // swapped getAdresL2/getGLN in the FA(2) copy.
        assertEquals("ul. Spedycyjna 1", transport.carrier().address().addressLine1());
        assertEquals("60-001 Poznan", transport.carrier().address().addressLine2());
        assertEquals("0000000000024", transport.carrier().address().gln());
        assertEquals("PL", transport.shipFrom().countryCode());
        assertEquals("Magazyn 1", transport.shipFrom().addressLine1());
        assertEquals(1, transport.shipVia().size());
        assertEquals("Hub Lodz", transport.shipVia().get(0).addressLine1());
        assertEquals("DE", transport.shipTo().countryCode());
        assertEquals("10115 Berlin", transport.shipTo().addressLine2());
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3 addressFa3(
            String country, String line1, String line2, String gln) {
        var address = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3();
        address.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodKraju.fromValue(country));
        address.setAdresL1(line1);
        address.setAdresL2(line2);
        address.setGLN(gln);
        return address;
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa2.TAdresFa2 addressFa2(
            String country, String line1, String line2, String gln) {
        var address = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TAdresFa2();
        address.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodKraju.fromValue(country));
        address.setAdresL1(line1);
        address.setAdresL2(line2);
        address.setGLN(gln);
        return address;
    }
}
