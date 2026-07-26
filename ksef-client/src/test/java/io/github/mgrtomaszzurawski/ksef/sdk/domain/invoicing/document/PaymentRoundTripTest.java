/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BankAccount;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePayment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartialPayment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PaymentTerm;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator.ValidationIssue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip guard for the {@code Fa/Platnosc} tree, surfaced as the
 * {@link InvoicePayment} nested record on both documents. The builder
 * writes the common payment fields (method code, due date); the richer
 * fields are set through the JAXB escape hatch, XSD-validated, and read
 * back through {@code payment()}.
 *
 * <p>{@code Platnosc} opens with an XSD choice: EITHER paid-in-full
 * ({@code Zaplacono} + {@code DataZaplaty}) OR paid-in-parts
 * ({@code ZnacznikZaplatyCzesciowej} + {@code ZaplataCzesciowa}), never
 * both — so FA(3) exercises the paid branch and FA(2) the partial branch.
 * FA(2) also differs in that {@code TerminOpis} is free text,
 * {@code ZnacznikZaplatyCzesciowej} is {@code TWybor1} (value 1 only), and
 * there is no {@code IPKSeF}/{@code LinkDoPlatnosci}.
 */
class PaymentRoundTripTest {

    private static final DatatypeFactory DTF = datatypeFactory();
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 6, 11);
    private static final LocalDate PAID_DATE = LocalDate.of(2026, 5, 12);
    private static final String METHOD_TRANSFER = "6";
    private static final String IBAN = "PL61109010140000071219812874";
    private static final String SWIFT = "WBKPPLPP";
    private static final String IP_KSEF = "1234567890abc"; // [0-9]{3}[a-zA-Z0-9]{10}
    private static final String PAY_LINK = "https://pay.example/p?IPKSeF=" + IP_KSEF;
    private static final byte MARKER_YES = 1;

    private static DatatypeFactory datatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static XMLGregorianCalendar greg(LocalDate date) {
        return DTF.newXMLGregorianCalendarDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED);
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

    /** Fields outside the paid/partial choice, set by both tests. */
    private static void assertCommonPayment(InvoicePayment payment) {
        assertEquals(METHOD_TRANSFER, payment.methodCode());
        assertNull(payment.otherForm());
        assertEquals("Zaplata do 7 dni", payment.earlyPaymentDiscount().conditions());
        assertEquals("2%", payment.earlyPaymentDiscount().amount());
        assertEquals(1, payment.bankAccounts().size());
        BankAccount account = payment.bankAccounts().get(0);
        assertEquals(IBAN, account.accountNumber());
        assertEquals(SWIFT, account.swift());
        assertEquals(1, account.ownAccountType());
        assertEquals("Bank X", account.bankName());
        assertEquals(1, payment.terms().size());
        assertEquals(DUE_DATE, payment.terms().get(0).dueDate());
    }

    @Test
    void fa3_paidBranch_surviveWriteThenRead() {
        // given / when — paid-in-full branch (Zaplacono + DataZaplaty) plus the
        // FA(3)-only IPKSeF/LinkDoPlatnosci and the structured TerminOpis.
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/PAY/0001").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00"))
                .addLineItem(plainLine())
                .paymentDueDate(DUE_DATE).paymentMethodCode(METHOD_TRANSFER)
                .customizeJaxb(faktura -> {
                    var p = faktura.getFa().getPlatnosc();
                    p.setZaplacono(MARKER_YES);
                    p.setDataZaplaty(greg(PAID_DATE));
                    p.setLinkDoPlatnosci(PAY_LINK);
                    p.setIPKSeF(IP_KSEF);
                    var sk = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Platnosc.Skonto();
                    sk.setWarunkiSkonta("Zaplata do 7 dni");
                    sk.setWysokoscSkonta("2%");
                    p.setSkonto(sk);
                    var rb = new io.github.mgrtomaszzurawski.ksef.xml.fa3.TRachunekBankowy();
                    rb.setNrRB(IBAN);
                    rb.setSWIFT(SWIFT);
                    rb.setRachunekWlasnyBanku(BigInteger.ONE);
                    rb.setNazwaBanku("Bank X");
                    p.getRachunekBankowy().add(rb);
                    var opis = new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.Platnosc
                            .TerminPlatnosci.TerminOpis();
                    opis.setIlosc(BigInteger.valueOf(14));
                    opis.setJednostka("dni");
                    opis.setZdarzeniePoczatkowe("od daty dostawy");
                    p.getTerminPlatnosci().get(0).setTerminOpis(opis);
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA3);
        InvoicePayment payment = Fa3InvoiceDocument.from(xml).payment();

        // then
        assertCommonPayment(payment);
        assertTrue(payment.paid());
        assertEquals(PAID_DATE, payment.paymentDate());
        assertNull(payment.partialPaymentStatus());
        assertTrue(payment.partialPayments().isEmpty());
        assertEquals(PAY_LINK, payment.paymentLink());
        assertEquals(IP_KSEF, payment.ipKsef());
        PaymentTerm term = payment.terms().get(0);
        assertEquals(14, term.quantity());
        assertEquals("dni", term.unit());
        assertEquals("od daty dostawy", term.startEvent());
        assertNull(term.description());
    }

    @Test
    void fa2_partialBranch_surviveWriteThenRead() {
        // given / when — partial-payment branch (ZnacznikZaplatyCzesciowej +
        // ZaplataCzesciowa); FA(2) has free-text TerminOpis, TWybor1 marker,
        // and a form-less ZaplataCzesciowa.
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/PAY/0002").issueDate(ISSUE_DATE)
                .seller(seller()).buyer(buyer()).totalGrossAmount(new BigDecimal("123.00"))
                .addLineItem(plainLine())
                .paymentDueDate(DUE_DATE).paymentMethodCode(METHOD_TRANSFER)
                .customizeJaxb(faktura -> {
                    var p = faktura.getFa().getPlatnosc();
                    p.setZnacznikZaplatyCzesciowej(MARKER_YES);
                    var zc = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Platnosc.ZaplataCzesciowa();
                    zc.setKwotaZaplatyCzesciowej(new BigDecimal("50.00"));
                    zc.setDataZaplatyCzesciowej(greg(PAID_DATE));
                    p.getZaplataCzesciowa().add(zc);
                    var sk = new io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura.Fa.Platnosc.Skonto();
                    sk.setWarunkiSkonta("Zaplata do 7 dni");
                    sk.setWysokoscSkonta("2%");
                    p.setSkonto(sk);
                    var rb = new io.github.mgrtomaszzurawski.ksef.xml.fa2.TRachunekBankowy();
                    rb.setNrRB(IBAN);
                    rb.setSWIFT(SWIFT);
                    rb.setRachunekWlasnyBanku(BigInteger.ONE);
                    rb.setNazwaBanku("Bank X");
                    p.getRachunekBankowy().add(rb);
                    p.getTerminPlatnosci().get(0).setTerminOpis("14 dni od daty dostawy");
                })
                .build().xml();

        assertNoXsdErrors(xml, FormCode.FA2);
        InvoicePayment payment = Fa2InvoiceDocument.from(xml).payment();

        // then
        assertCommonPayment(payment);
        assertNull(payment.paid());
        assertNull(payment.paymentDate());
        assertEquals(1, payment.partialPaymentStatus());
        assertEquals(1, payment.partialPayments().size());
        PartialPayment installment = payment.partialPayments().get(0);
        assertEquals(0, new BigDecimal("50.00").compareTo(installment.amount()));
        assertEquals(PAID_DATE, installment.date());
        assertNull(installment.methodCode());
        assertNull(payment.paymentLink());
        assertNull(payment.ipKsef());
        PaymentTerm term = payment.terms().get(0);
        assertEquals("14 dni od daty dostawy", term.description());
        assertNull(term.quantity());
    }
}
