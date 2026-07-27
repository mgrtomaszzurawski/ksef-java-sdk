/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ThirdParty;
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
 * Round-trip guard for the {@code Faktura/Podmiot3} third-party list,
 * surfaced as {@link ThirdParty} on both documents. Set through the JAXB
 * escape hatch (the builder does not surface it), XSD-validated, and read
 * back through {@code thirdParties()}.
 *
 * <p>The identity and role are XSD choices, split across the schemas to
 * exercise both hand-duplicated extractors: FA(3) uses the NIP identity
 * branch and a coded {@code Rola}; FA(2) uses the other-tax-id branch
 * ({@code KodKraju}+{@code NrID}) and the "other role" branch
 * ({@code RolaInna}+{@code OpisRoli}).
 */
class ThirdPartyRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final BigDecimal SHARE = new BigDecimal("25.00");

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
    void fa3_nipIdentityAndCodedRole_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/P3/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var party = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Podmiot3();
                    party.setIDNabywcy("BUYER-KEY-1");
                    party.setNrEORI("PL123456789012345");
                    var identity = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot3();
                    identity.setNIP("5555555555");
                    identity.setNazwa("Recipient sp. z o.o.");
                    party.setDaneIdentyfikacyjne(identity);
                    party.setAdres(addressFa3("PL", "ul. Odbiorcza 1", "03-000 Warszawa"));
                    party.setAdresKoresp(addressFa3("PL", "skr. poczt. 5", null));
                    var contact = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Podmiot3.DaneKontaktowe();
                    contact.setEmail("recipient@example.com");
                    contact.setTelefon("+48111222333");
                    party.getDaneKontaktowe().add(contact);
                    party.setRola(BigInteger.valueOf(2));
                    party.setUdzial(SHARE);
                    party.setNrKlienta("CUST-42");
                    faktura.getPodmiot3().add(party);
                    // Second third party exercises the coupled EU-VAT identity
                    // branch (KodUE + NrVatUE) that the first party's NIP branch
                    // leaves untested.
                    var euParty = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Podmiot3();
                    var euIdentity = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot3();
                    euIdentity.setKodUE(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodyKrajowUE.fromValue("AT"));
                    euIdentity.setNrVatUE("U12345678");
                    euIdentity.setNazwa("EU Recipient GmbH");
                    euParty.setDaneIdentyfikacyjne(euIdentity);
                    euParty.setRola(BigInteger.valueOf(3));
                    faktura.getPodmiot3().add(euParty);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        List<ThirdParty> parties = Fa3InvoiceDocument.from(xml).thirdParties();

        assertEquals(2, parties.size());
        ThirdParty party = parties.get(0);
        assertEquals("BUYER-KEY-1", party.buyerId());
        assertEquals("PL123456789012345", party.eori());
        assertEquals("5555555555", party.identity().nip());
        assertEquals("Recipient sp. z o.o.", party.identity().name());
        assertNull(party.identity().noTaxId());
        assertEquals("PL", party.address().countryCode());
        assertEquals("ul. Odbiorcza 1", party.address().addressLine1());
        assertEquals("03-000 Warszawa", party.address().addressLine2());
        assertEquals("skr. poczt. 5", party.correspondenceAddress().addressLine1());
        assertEquals(1, party.contacts().size());
        assertEquals("recipient@example.com", party.contacts().get(0).email());
        assertEquals("+48111222333", party.contacts().get(0).phone());
        assertEquals(2, party.role());
        assertNull(party.otherRole());
        assertNull(party.roleDescription());
        assertEquals(0, SHARE.compareTo(party.share()));
        assertEquals("CUST-42", party.customerNumber());

        ThirdParty euParty = parties.get(1);
        assertNull(euParty.identity().nip());
        assertEquals("AT", euParty.identity().euVatPrefix());
        assertEquals("U12345678", euParty.identity().euVatNumber());
        assertEquals("EU Recipient GmbH", euParty.identity().name());
        assertEquals(3, euParty.role());
    }

    @Test
    void fa2_otherIdentityAndOtherRole_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/P3/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var party = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Podmiot3();
                    party.setIDNabywcy("BUYER-KEY-2");
                    party.setNrEORI("AT987654321098765");
                    var identity = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot3();
                    identity.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodKraju.fromValue("DE"));
                    identity.setNrID("DETAX4711");
                    identity.setNazwa("Foreign Factor GmbH");
                    party.setDaneIdentyfikacyjne(identity);
                    // Address country (AT) deliberately differs from the identity's
                    // tax-id issuing country (DE) so a cross-wire would be caught.
                    party.setAdres(addressFa2("AT", "Faktorgasse 2", "1010 Wien"));
                    party.setAdresKoresp(addressFa2("AT", "Postfach 9", null));
                    var contact = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Podmiot3.DaneKontaktowe();
                    contact.setEmail("factor@example.com");
                    contact.setTelefon("+43111222333");
                    party.getDaneKontaktowe().add(contact);
                    party.setRolaInna((byte) 1);
                    party.setOpisRoli("Faktor");
                    party.setUdzial(new BigDecimal("10.00"));
                    party.setNrKlienta("CUST-77");
                    faktura.getPodmiot3().add(party);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        List<ThirdParty> parties = Fa2InvoiceDocument.from(xml).thirdParties();

        assertEquals(1, parties.size());
        ThirdParty party = parties.get(0);
        assertEquals("BUYER-KEY-2", party.buyerId());
        assertEquals("AT987654321098765", party.eori());
        assertNull(party.identity().nip());
        assertEquals("DE", party.identity().taxIdCountryCode());
        assertEquals("DETAX4711", party.identity().otherTaxId());
        assertEquals("Foreign Factor GmbH", party.identity().name());
        assertEquals("AT", party.address().countryCode());
        assertEquals("Faktorgasse 2", party.address().addressLine1());
        assertEquals("1010 Wien", party.address().addressLine2());
        assertEquals("AT", party.correspondenceAddress().countryCode());
        assertEquals("Postfach 9", party.correspondenceAddress().addressLine1());
        assertEquals(1, party.contacts().size());
        assertEquals("factor@example.com", party.contacts().get(0).email());
        assertEquals("+43111222333", party.contacts().get(0).phone());
        assertNull(party.role());
        assertEquals(Boolean.TRUE, party.otherRole());
        assertEquals("Faktor", party.roleDescription());
        assertEquals(0, new BigDecimal("10.00").compareTo(party.share()));
        assertEquals("CUST-77", party.customerNumber());
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
