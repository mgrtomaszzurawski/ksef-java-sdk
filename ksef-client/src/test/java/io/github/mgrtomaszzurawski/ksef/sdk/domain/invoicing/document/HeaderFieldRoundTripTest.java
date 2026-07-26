/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateBucket;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateSum;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TRodzajFaktury;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip guard for the {@code Fa} header scalar surface added to the
 * FA(2)/FA(3) invoice overlay: VAT-summary fields ({@code P_13_6_x},
 * {@code P_14_x_W}) folded into {@code vatBreakdown}, the flat header
 * scalars ({@code P_1M}, {@code KursWalutyZ}, and the correction-context
 * {@code P_15ZK} / {@code KursWalutyZK} / {@code NrFaKorygowany}) and the
 * single-choice markers ({@code FP}, {@code TP}, {@code ZwrotAkcyzy}).
 * Each was ABSENT from the typed overlay until this change.
 *
 * <p>The single-choice tests also guard against the {@code etd:TWybor1}
 * write regression: those markers permit only value {@code 1}, so a
 * {@code false} must leave the element absent rather than emit an
 * XSD-invalid {@code 2}.
 */
class HeaderFieldRoundTripTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String BUYER_NIP = "9876543210";
    private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);

    private static final String ISSUE_LOCALITY = "Gdansk";
    private static final BigDecimal TAX_EXCHANGE_RATE = new BigDecimal("4.3210");
    private static final BigDecimal GROSS_BEFORE_CORRECTION = new BigDecimal("100.00");
    private static final BigDecimal RATE_BEFORE_CORRECTION = new BigDecimal("4.1234");
    private static final String CORRECTED_NUMBER_REPLACEMENT = "FA/2026/FIX/0009";
    private static final String ORIGINAL_KSEF_NUMBER = "1111111111-20260501-AABBCC000000-11";

    private static InvoiceParty seller() {
        return new InvoiceParty(SELLER_NIP, "Acme sp. z o.o.", "00-001", "Warszawa", "Marszalkowska", "10", null);
    }

    private static InvoiceParty buyer() {
        return new InvoiceParty(BUYER_NIP, "Customer sp. z o.o.", "00-002", "Krakow", null, "5", null);
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
    void fa3_headerScalars_surviveWriteThenRead() {
        // given / when
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/HDR/0001")
                .issueDate(ISSUE_DATE)
                .issueLocality(ISSUE_LOCALITY)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(plainLine())
                .taxExchangeRate(TAX_EXCHANGE_RATE)
                .issuedToReceipt(true)
                .relatedParty(true)
                .exciseDutyRefund(true)
                .build()
                .xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        // then
        assertHeaderRoundTrip(document.issueLocality(), document.taxExchangeRate(),
                document.issuedToReceipt(), document.relatedParty(), document.exciseDutyRefund());
    }

    @Test
    void fa2_headerScalars_surviveWriteThenRead() {
        // given / when
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/HDR/0002")
                .issueDate(ISSUE_DATE)
                .issueLocality(ISSUE_LOCALITY)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(plainLine())
                .taxExchangeRate(TAX_EXCHANGE_RATE)
                .issuedToReceipt(true)
                .relatedParty(true)
                .exciseDutyRefund(true)
                .build()
                .xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        Fa2InvoiceDocument document = Fa2InvoiceDocument.from(xml);

        // then
        assertHeaderRoundTrip(document.issueLocality(), document.taxExchangeRate(),
                document.issuedToReceipt(), document.relatedParty(), document.exciseDutyRefund());
    }

    private static void assertHeaderRoundTrip(String issueLocality, BigDecimal taxRate,
            Boolean issuedToReceipt, Boolean relatedParty, Boolean exciseDutyRefund) {
        assertEquals(ISSUE_LOCALITY, issueLocality);
        assertEquals(0, TAX_EXCHANGE_RATE.compareTo(taxRate));
        assertTrue(issuedToReceipt);
        assertTrue(relatedParty);
        assertTrue(exciseDutyRefund);
    }

    @Test
    void fa3_singleChoiceMarkersFalse_produceValidXmlAndReadNull() {
        // given / when — false must NOT emit an XSD-invalid "2" into a TWybor1
        // element (the B.T1 regression); the marker element is simply absent.
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/HDR/0003")
                .issueDate(ISSUE_DATE)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(InvoiceLineItem.builder().rowNumber(1).description("Consulting")
                        .unitOfMeasure("szt.").quantity(BigDecimal.ONE).netUnitPrice(new BigDecimal("100.00"))
                        .netAmount(new BigDecimal("100.00")).vatRate("23")
                        .annex15(false).correctionStateBefore(false).build())
                .issuedToReceipt(false)
                .relatedParty(false)
                .exciseDutyRefund(false)
                .build()
                .xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        // then — an absent single-choice marker reads back as null, not false
        assertNull(document.issuedToReceipt());
        assertNull(document.relatedParty());
        assertNull(document.exciseDutyRefund());
        assertNull(document.lineItems().get(0).annex15());
        assertNull(document.lineItems().get(0).correctionStateBefore());
    }

    @Test
    void fa3_correctionContextScalars_readBack() {
        // given / when — P_15ZK / KursWalutyZK / NrFaKorygowany live in the
        // correction-only XSD sub-sequence, so they are written via the JAXB
        // escape hatch on a KOR invoice (which carries DaneFaKorygowanej).
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/KOR/0005")
                .issueDate(ISSUE_DATE)
                .seller(seller())
                .buyer(buyer())
                .rodzajFaktury(TRodzajFaktury.KOR)
                .correctionReference(new InvoiceCorrectionReference(
                        "FA/2026/ORIG/0001", LocalDate.of(2026, 5, 1), ORIGINAL_KSEF_NUMBER))
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var fa = faktura.getFa();
                    fa.setNrFaKorygowany(CORRECTED_NUMBER_REPLACEMENT);
                    fa.setP15ZK(GROSS_BEFORE_CORRECTION);
                    fa.setKursWalutyZK(RATE_BEFORE_CORRECTION);
                })
                .build()
                .xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        // then
        assertEquals(CORRECTED_NUMBER_REPLACEMENT, document.correctedInvoiceNumberReplacement());
        assertEquals(0, GROSS_BEFORE_CORRECTION.compareTo(document.grossTotalBeforeCorrection()));
        assertEquals(0, RATE_BEFORE_CORRECTION.compareTo(document.taxExchangeRateBeforeCorrection()));
    }

    @Test
    void fa3_correctionReference_emitsChoiceMarkerForBothKsefAndNonKsef() {
        // given / when — DaneFaKorygowanej needs a choice marker: NrKSeF=1 for a
        // KSeF-issued original, NrKSeFN=1 for one issued outside KSeF. Both must
        // pass XSD validation (the builder previously emitted neither marker).
        byte[] withKsef = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/KOR/0006").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).rodzajFaktury(TRodzajFaktury.KOR)
                .correctionReference(new InvoiceCorrectionReference(
                        "FA/2026/ORIG/0002", LocalDate.of(2026, 5, 1), ORIGINAL_KSEF_NUMBER))
                .totalGrossAmount(GROSS_AMOUNT).addLineItem(plainLine())
                .build().xml();
        byte[] withoutKsef = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/KOR/0007").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).rodzajFaktury(TRodzajFaktury.KOR)
                .correctionReference(new InvoiceCorrectionReference(
                        "FA/2026/ORIG/0003", LocalDate.of(2026, 5, 1), null))
                .totalGrossAmount(GROSS_AMOUNT).addLineItem(plainLine())
                .build().xml();

        // then
        assertNoXsdErrors(withKsef, FormCode.FA3);
        assertNoXsdErrors(withoutKsef, FormCode.FA3);
        assertEquals(ORIGINAL_KSEF_NUMBER,
                Fa3InvoiceDocument.from(withKsef).correctedInvoices().get(0).originalKsefNumber());
    }

    @Test
    void fa3_vatSummaryFields_foldIntoBreakdown() {
        // given / when — populate the zero-rate net buckets and the PLN-converted
        // VAT via the JAXB escape hatch, then read the invoice back.
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/HDR/0004")
                .issueDate(ISSUE_DATE)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(plainLine())
                .customizeJaxb(faktura -> {
                    var fa = faktura.getFa();
                    fa.setP131(new BigDecimal("100.00"));
                    fa.setP141(new BigDecimal("23.00"));
                    fa.setP141W(new BigDecimal("99.36"));
                    fa.setP1361(new BigDecimal("10.00"));
                    fa.setP1362(new BigDecimal("20.00"));
                    fa.setP1363(new BigDecimal("30.00"));
                })
                .build()
                .xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        List<VatRateSum> breakdown = Fa3InvoiceDocument.from(xml).vatBreakdown();

        // then
        VatRateSum standard = bucket(breakdown, VatRateBucket.STANDARD);
        assertEquals(0, new BigDecimal("23.00").compareTo(standard.vatAmount()));
        assertEquals(0, new BigDecimal("99.36").compareTo(standard.vatAmountConvertedToPln()));
        assertEquals(0, new BigDecimal("10.00").compareTo(bucket(breakdown, VatRateBucket.ZERO_RATE_DOMESTIC).netAmount()));
        assertEquals(0, new BigDecimal("20.00").compareTo(bucket(breakdown, VatRateBucket.ZERO_RATE_INTRA_EU).netAmount()));
        assertEquals(0, new BigDecimal("30.00").compareTo(bucket(breakdown, VatRateBucket.ZERO_RATE_EXPORT).netAmount()));
        assertNull(bucket(breakdown, VatRateBucket.ZERO_RATE_DOMESTIC).vatAmount());
    }

    private static VatRateSum bucket(List<VatRateSum> breakdown, VatRateBucket wanted) {
        Optional<VatRateSum> found = breakdown.stream().filter(sum -> sum.bucket() == wanted).findFirst();
        assertTrue(found.isPresent(), "expected bucket " + wanted + " in breakdown " + breakdown);
        return found.get();
    }
}
