/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AdvanceInvoiceReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartialAdvance;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.TaxpayerStatus;
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
 * Round-trip guard for the party-completeness and advance-invoice fields:
 * {@code Podmiot1}/{@code Podmiot2} scalars (NrEORI, PrefiksPodatnika,
 * StatusInfoPodatnika, IDNabywcy, NrKlienta) + AdresKoresp, and the
 * {@code Fa/ZaliczkaCzesciowa} / {@code Fa/FakturaZaliczkowa} advance sections.
 * Set through the JAXB escape hatch, XSD-validated, and read back.
 *
 * <p>Both hand-duplicated read paths are exercised with distinct values. FA(3)
 * drives the {@code FakturaZaliczkowa} out-of-KSeF branch (NrKSeFZN + number)
 * and a partial advance with an exchange rate; FA(2) drives the in-KSeF branch
 * (NrKSeFFaZaliczkowej) and a partial advance without an exchange rate.
 */
class PartyAndAdvanceRoundTripTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final LocalDate ADVANCE_DATE_FA3 = LocalDate.of(2026, 3, 1);
    private static final LocalDate ADVANCE_DATE_FA2 = LocalDate.of(2026, 2, 2);
    private static final String KSEF_NUMBER = "5265877635-20250826-0100001AF629-AF";
    private static final byte YES = 1;
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
    void fa3_partyScalarsAndAdvances_surviveWriteThenRead() {
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/PARTY/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var podmiot1 = faktura.getPodmiot1();
                    podmiot1.setNrEORI("PL1234567");
                    podmiot1.setPrefiksPodatnika(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodyKrajowUE.PL);
                    podmiot1.setStatusInfoPodatnika(BigInteger.ONE);
                    var sellerKoresp = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Podmiot1.AdresKoresp();
                    sellerKoresp.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodKraju.PL);
                    sellerKoresp.setAdresL1("ul. Korespondencyjna 1, Sprzedawca");
                    podmiot1.setAdresKoresp(sellerKoresp);

                    var podmiot2 = faktura.getPodmiot2();
                    podmiot2.setIDNabywcy("BUYER-ID-1");
                    podmiot2.setNrEORI("DE7654321");
                    podmiot2.setNrKlienta("CLIENT-9");
                    var buyerKoresp = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3();
                    buyerKoresp.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodKraju.PL);
                    buyerKoresp.setAdresL1("ul. Korespondencyjna 9, Nabywca");
                    podmiot2.setAdresKoresp(buyerKoresp);

                    var partial = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.ZaliczkaCzesciowa();
                    partial.setP6Z(greg(ADVANCE_DATE_FA3));
                    partial.setP15Z(new BigDecimal("100.00"));
                    partial.setKursWalutyZW(new BigDecimal("4.5000"));
                    faktura.getFa().getZaliczkaCzesciowa().add(partial);

                    var advance = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.FakturaZaliczkowa();
                    advance.setNrKSeFZN(YES);
                    advance.setNrFaZaliczkowej("ADV/2026/1");
                    faktura.getFa().getFakturaZaliczkowa().add(advance);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        assertEquals("PL1234567", document.sellerEori());
        assertEquals("PL", document.sellerTaxpayerPrefix());
        assertEquals(TaxpayerStatus.LIQUIDATION, document.sellerTaxpayerStatus());
        assertEquals("ul. Korespondencyjna 1, Sprzedawca", document.sellerCorrespondenceAddress().addressLine1());
        assertEquals("PL", document.sellerCorrespondenceAddress().countryCode());
        assertEquals("BUYER-ID-1", document.buyerId());
        assertEquals("DE7654321", document.buyerEori());
        assertEquals("CLIENT-9", document.buyerClientNumber());
        assertEquals("ul. Korespondencyjna 9, Nabywca", document.buyerCorrespondenceAddress().addressLine1());
        assertEquals("PL", document.buyerCorrespondenceAddress().countryCode());

        assertEquals(1, document.partialAdvances().size());
        PartialAdvance partial = document.partialAdvances().get(0);
        assertEquals(new BigDecimal("100.00"), partial.amount());
        assertEquals(ADVANCE_DATE_FA3, partial.receiptDate());
        assertEquals(new BigDecimal("4.5000"), partial.exchangeRate());

        assertEquals(1, document.advanceInvoiceReferences().size());
        AdvanceInvoiceReference advance = document.advanceInvoiceReferences().get(0);
        assertEquals("ADV/2026/1", advance.invoiceNumber());
        assertEquals(Boolean.TRUE, advance.withoutKsefNumber());
        assertNull(advance.ksefNumber());
    }

    @Test
    void fa2_partyScalarsAndAdvancesDistinct_surviveWriteThenRead() {
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/PARTY/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00")).addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var podmiot1 = faktura.getPodmiot1();
                    podmiot1.setNrEORI("PL7777777");
                    podmiot1.setPrefiksPodatnika(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodyKrajowUE.PL);
                    podmiot1.setStatusInfoPodatnika(BigInteger.ONE);
                    var sellerKoresp = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Podmiot1.AdresKoresp();
                    sellerKoresp.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodKraju.PL);
                    sellerKoresp.setAdresL1("ul. FA2 Koresp 7, Sprzedawca");
                    podmiot1.setAdresKoresp(sellerKoresp);

                    var podmiot2 = faktura.getPodmiot2();
                    podmiot2.setIDNabywcy("BUYER-ID-2");
                    podmiot2.setNrEORI("FR1111111");
                    podmiot2.setNrKlienta("CLIENT-2");
                    var buyerKoresp = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TAdresFa2();
                    buyerKoresp.setKodKraju(io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodKraju.PL);
                    buyerKoresp.setAdresL1("ul. FA2 Koresp 2, Nabywca");
                    podmiot2.setAdresKoresp(buyerKoresp);

                    var partial = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.ZaliczkaCzesciowa();
                    partial.setP6Z(greg(ADVANCE_DATE_FA2));
                    partial.setP15Z(new BigDecimal("200.00"));
                    faktura.getFa().getZaliczkaCzesciowa().add(partial);

                    var advance = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.FakturaZaliczkowa();
                    advance.setNrKSeFFaZaliczkowej(KSEF_NUMBER);
                    faktura.getFa().getFakturaZaliczkowa().add(advance);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        Fa2InvoiceDocument document = Fa2InvoiceDocument.from(xml);

        assertEquals("PL7777777", document.sellerEori());
        assertEquals("PL", document.sellerTaxpayerPrefix());
        assertEquals(TaxpayerStatus.LIQUIDATION, document.sellerTaxpayerStatus());
        assertEquals("ul. FA2 Koresp 7, Sprzedawca", document.sellerCorrespondenceAddress().addressLine1());
        assertEquals("PL", document.sellerCorrespondenceAddress().countryCode());
        assertEquals("BUYER-ID-2", document.buyerId());
        assertEquals("FR1111111", document.buyerEori());
        assertEquals("CLIENT-2", document.buyerClientNumber());
        assertEquals("ul. FA2 Koresp 2, Nabywca", document.buyerCorrespondenceAddress().addressLine1());
        assertEquals("PL", document.buyerCorrespondenceAddress().countryCode());

        assertEquals(1, document.partialAdvances().size());
        PartialAdvance partial = document.partialAdvances().get(0);
        assertEquals(new BigDecimal("200.00"), partial.amount());
        assertEquals(ADVANCE_DATE_FA2, partial.receiptDate());
        assertNull(partial.exchangeRate());

        assertEquals(1, document.advanceInvoiceReferences().size());
        AdvanceInvoiceReference advance = document.advanceInvoiceReferences().get(0);
        assertEquals(KSEF_NUMBER, advance.ksefNumber());
        assertNull(advance.invoiceNumber());
        assertNull(advance.withoutKsefNumber());
    }
}
