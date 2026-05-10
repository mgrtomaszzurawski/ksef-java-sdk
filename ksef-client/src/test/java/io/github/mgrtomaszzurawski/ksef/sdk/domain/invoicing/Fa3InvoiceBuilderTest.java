/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator.ValidationIssue;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TRodzajFaktury;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Fa3InvoiceBuilderTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String BUYER_NIP = "9876543210";
    private static final String INVOICE_NUMBER = "FA/2026/0001";
    private static final BigDecimal NET_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");
    private static final String VAT_RATE = "23";

    @Test
    void build_whenIssueDateMissing_throwsNpe() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> Fa3Invoice.builder()
                        .invoiceNumber(INVOICE_NUMBER)
                        .seller(seller())
                        .buyer(buyer())
                        .totalGrossAmount(GROSS_AMOUNT)
                        .addLineItem(lineItem())
                        .build());
    }

    @Test
    void build_whenSellerMissing_throwsNpe() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> Fa3Invoice.builder()
                        .invoiceNumber(INVOICE_NUMBER)
                        .issueDate(LocalDate.of(2026, 5, 9))
                        .buyer(buyer())
                        .totalGrossAmount(GROSS_AMOUNT)
                        .addLineItem(lineItem())
                        .build());
    }

    @Test
    void build_whenHappyPath_producesXmlSurvivingXsdValidation() {
        // given
        Fa3Invoice invoice = Fa3Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 9))
                .issueLocality("Warszawa")
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(lineItem())
                .build();

        // when
        List<ValidationIssue> issues = KsefXmlValidator.validate(invoice.xml(), FormCode.FA3);

        // then — no FATAL/ERROR severity issues; warnings (if any) are tolerated
        assertFalse(issues.stream().anyMatch(issue -> issue.severity() == Severity.ERROR
                        || issue.severity() == Severity.FATAL),
                "Generated XML must survive XSD validation: " + issues);
    }

    @Test
    void build_whenCorrectionTypeWithoutReference_throwsIllegalState() {
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Fa3Invoice.builder()
                        .invoiceNumber(INVOICE_NUMBER)
                        .issueDate(LocalDate.of(2026, 5, 9))
                        .seller(seller())
                        .buyer(buyer())
                        .rodzajFaktury(TRodzajFaktury.KOR)
                        .totalGrossAmount(GROSS_AMOUNT)
                        .addLineItem(lineItem())
                        .build());
        assertTrue(ex.getMessage().contains("correction"),
                "Diagnostic must mention correction-invoice rule: " + ex.getMessage());
    }

    @Test
    void build_whenCorrectionTypeWithReference_succeeds() {
        // given / when
        Fa3Invoice invoice = Fa3Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 9))
                .seller(seller())
                .buyer(buyer())
                .rodzajFaktury(TRodzajFaktury.KOR)
                .correctionReference(new InvoiceCorrectionReference(
                        "FA/2025/0099", LocalDate.of(2025, 12, 1)))
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(lineItem())
                .build();

        // then
        assertTrue(invoice.xml().length > 0);
    }

    private static InvoiceParty seller() {
        return new InvoiceParty(SELLER_NIP, "Acme sp. z o.o.", "00-001",
                "Warszawa", "Marszalkowska", "10", null);
    }

    private static InvoiceParty buyer() {
        return new InvoiceParty(BUYER_NIP, "Customer sp. z o.o.", "00-002",
                "Krakow", null, "5", null);
    }

    private static InvoiceLineItem lineItem() {
        return new InvoiceLineItem(1, "Consulting", "szt.",
                new BigDecimal("1"), NET_AMOUNT, NET_AMOUNT, VAT_RATE);
    }
}
