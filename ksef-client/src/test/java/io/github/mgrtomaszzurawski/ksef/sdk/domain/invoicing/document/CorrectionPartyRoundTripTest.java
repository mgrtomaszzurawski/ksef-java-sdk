/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AuthorizedParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.CorrectionBuyer;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.CorrectionSeller;
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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Round-trip guard for the correction and authorised-party sections:
 * {@code Fa/Podmiot1K} → {@link CorrectionSeller}, {@code Fa/Podmiot2K} →
 * {@link CorrectionBuyer}, and {@code Faktura/PodmiotUpowazniony} →
 * {@link AuthorizedParty}. Set through the JAXB escape hatch (the builder
 * does not surface them), XSD-validated, and read back.
 *
 * <p>FA(3) exercises the NIP buyer-identity branch and a taxpayer prefix;
 * FA(2) exercises the other-tax-id buyer branch and an absent prefix, so
 * both hand-duplicated extractors are covered.
 */
class CorrectionPartyRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final LocalDate CORRECTED_DATE = LocalDate.of(2026, 4, 1);
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

    @Test
    void fa3_correctionAndAuthorizedParties_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/KP/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    // Podmiot1K/Podmiot2K live in the correction sequence, which the
                    // XSD only admits after a DaneFaKorygowanej reference on a KOR invoice.
                    faktura.getFa().setRodzajFaktury(io.github.mgrtomaszzurawski.ksef.xml.fa3.TRodzajFaktury.KOR);
                    var corrected = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.DaneFaKorygowanej();
                    corrected.setDataWystFaKorygowanej(greg(CORRECTED_DATE));
                    corrected.setNrFaKorygowanej("FA/2026/ORIG/1");
                    corrected.setNrKSeFN((byte) 1);
                    faktura.getFa().getDaneFaKorygowanej().add(corrected);

                    var seller = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Podmiot1K();
                    seller.setPrefiksPodatnika(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodyKrajowUE.fromValue("PL"));
                    var sellerId = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot1();
                    sellerId.setNIP("1111111111");
                    sellerId.setNazwa("Old Seller sp. z o.o.");
                    seller.setDaneIdentyfikacyjne(sellerId);
                    seller.setAdres(addressFa3("PL", "ul. Stara 1", "00-001 Warszawa"));
                    faktura.getFa().setPodmiot1K(seller);

                    var corrBuyer = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Podmiot2K();
                    var buyerId = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot2();
                    buyerId.setNIP("9876543210");
                    buyerId.setNazwa("Old Buyer sp. z o.o.");
                    corrBuyer.setDaneIdentyfikacyjne(buyerId);
                    corrBuyer.setAdres(addressFa3("PL", "ul. Klienta 2", "00-002 Krakow"));
                    corrBuyer.setIDNabywcy("BUYER-CORR-1");
                    faktura.getFa().getPodmiot2K().add(corrBuyer);

                    var agent = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.PodmiotUpowazniony();
                    agent.setNrEORI("PL999888777666555");
                    var agentId = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot1();
                    agentId.setNIP("5555555555");
                    agentId.setNazwa("Agent sp. z o.o.");
                    agent.setDaneIdentyfikacyjne(agentId);
                    agent.setAdres(addressFa3("PL", "ul. Agencyjna 3", "00-003 Gdansk"));
                    agent.setAdresKoresp(addressFa3("PL", "skr. 7", null));
                    var contact = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.PodmiotUpowazniony.DaneKontaktowe();
                    contact.setEmailPU("agent@example.com");
                    contact.setTelefonPU("+48222333444");
                    agent.getDaneKontaktowe().add(contact);
                    agent.setRolaPU(BigInteger.valueOf(1));
                    faktura.setPodmiotUpowazniony(agent);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        CorrectionSeller correctionSeller = document.correctionSeller();
        assertEquals("1111111111", correctionSeller.nip());
        assertEquals("Old Seller sp. z o.o.", correctionSeller.name());
        assertEquals("PL", correctionSeller.taxpayerPrefix());
        assertEquals("PL", correctionSeller.address().countryCode());
        assertEquals("ul. Stara 1", correctionSeller.address().addressLine1());

        assertEquals(1, document.correctionBuyers().size());
        CorrectionBuyer correctionBuyer = document.correctionBuyers().get(0);
        assertEquals("9876543210", correctionBuyer.identity().nip());
        assertEquals("Old Buyer sp. z o.o.", correctionBuyer.identity().name());
        assertEquals("ul. Klienta 2", correctionBuyer.address().addressLine1());
        assertEquals("BUYER-CORR-1", correctionBuyer.buyerId());

        AuthorizedParty agent = document.authorizedParty();
        assertEquals("5555555555", agent.nip());
        assertEquals("Agent sp. z o.o.", agent.name());
        assertEquals("PL999888777666555", agent.eori());
        assertEquals("ul. Agencyjna 3", agent.address().addressLine1());
        assertEquals("skr. 7", agent.correspondenceAddress().addressLine1());
        assertEquals(1, agent.contacts().size());
        assertEquals("agent@example.com", agent.contacts().get(0).email());
        assertEquals("+48222333444", agent.contacts().get(0).phone());
        assertEquals(1, agent.role());
    }

    @Test
    void fa2_correctionAndAuthorizedParties_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/KP/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    // Podmiot1K/Podmiot2K live in the correction sequence, which the
                    // XSD only admits after a DaneFaKorygowanej reference on a KOR invoice.
                    faktura.getFa().setRodzajFaktury(io.github.mgrtomaszzurawski.ksef.xml.fa2.TRodzajFaktury.KOR);
                    var corrected = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.DaneFaKorygowanej();
                    corrected.setDataWystFaKorygowanej(greg(CORRECTED_DATE));
                    corrected.setNrFaKorygowanej("FA/2026/ORIG/1");
                    corrected.setNrKSeFN((byte) 1);
                    faktura.getFa().getDaneFaKorygowanej().add(corrected);

                    var seller = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Podmiot1K();
                    var sellerId = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot1();
                    sellerId.setNIP("1111111111");
                    sellerId.setNazwa("Old Seller sp. z o.o.");
                    seller.setDaneIdentyfikacyjne(sellerId);
                    seller.setAdres(addressFa2("PL", "ul. Stara 1", "00-001 Warszawa"));
                    faktura.getFa().setPodmiot1K(seller);

                    var corrBuyer = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Podmiot2K();
                    var buyerId = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot2();
                    buyerId.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodKraju.fromValue("DE"));
                    buyerId.setNrID("DE123456789");
                    buyerId.setNazwa("Old Foreign Buyer GmbH");
                    corrBuyer.setDaneIdentyfikacyjne(buyerId);
                    corrBuyer.setAdres(addressFa2("DE", "Kundenstrasse 2", "10115 Berlin"));
                    corrBuyer.setIDNabywcy("BUYER-CORR-2");
                    faktura.getFa().getPodmiot2K().add(corrBuyer);

                    var agent = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.PodmiotUpowazniony();
                    var agentId = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot1();
                    agentId.setNIP("5555555555");
                    agentId.setNazwa("Agent sp. z o.o.");
                    agent.setDaneIdentyfikacyjne(agentId);
                    agent.setAdres(addressFa2("PL", "ul. Agencyjna 3", "00-003 Gdansk"));
                    var contact = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.PodmiotUpowazniony.DaneKontaktowe();
                    contact.setEmailPU("agent@example.com");
                    agent.getDaneKontaktowe().add(contact);
                    agent.setRolaPU(BigInteger.valueOf(2));
                    faktura.setPodmiotUpowazniony(agent);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        Fa2InvoiceDocument document = Fa2InvoiceDocument.from(xml);

        CorrectionSeller correctionSeller = document.correctionSeller();
        assertEquals("1111111111", correctionSeller.nip());
        assertEquals("Old Seller sp. z o.o.", correctionSeller.name());
        assertNull(correctionSeller.taxpayerPrefix());
        assertEquals("ul. Stara 1", correctionSeller.address().addressLine1());

        assertEquals(1, document.correctionBuyers().size());
        CorrectionBuyer correctionBuyer = document.correctionBuyers().get(0);
        assertNull(correctionBuyer.identity().nip());
        assertEquals("DE", correctionBuyer.identity().taxIdCountryCode());
        assertEquals("DE123456789", correctionBuyer.identity().otherTaxId());
        assertEquals("Old Foreign Buyer GmbH", correctionBuyer.identity().name());
        assertEquals("DE", correctionBuyer.address().countryCode());
        assertEquals("BUYER-CORR-2", correctionBuyer.buyerId());

        AuthorizedParty agent = document.authorizedParty();
        assertEquals("5555555555", agent.nip());
        assertEquals("Agent sp. z o.o.", agent.name());
        assertNull(agent.eori());
        assertEquals("ul. Agencyjna 3", agent.address().addressLine1());
        assertNull(agent.correspondenceAddress());
        assertEquals(1, agent.contacts().size());
        assertEquals("agent@example.com", agent.contacts().get(0).email());
        assertNull(agent.contacts().get(0).phone());
        assertEquals(2, agent.role());
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3 addressFa3(
            String country, String line1, String line2) {
        var address = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3();
        address.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodKraju.fromValue(country));
        address.setAdresL1(line1);
        address.setAdresL2(line2);
        return address;
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa2.TAdresFa2 addressFa2(
            String country, String line1, String line2) {
        var address = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TAdresFa2();
        address.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodKraju.fromValue(country));
        address.setAdresL1(line1);
        address.setAdresL2(line2);
        return address;
    }
}
