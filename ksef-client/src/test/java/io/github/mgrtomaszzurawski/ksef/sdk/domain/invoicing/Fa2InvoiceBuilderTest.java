/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TRodzajFaktury;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Fa2InvoiceBuilderTest {

    private static final String SELLER_NIP = "1234567890";
    private static final String BUYER_NIP = "9876543210";
    private static final String INVOICE_NUMBER = "FA/2026/0001";
    private static final BigDecimal NET_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");

    @Test
    void build_whenInvoiceNumberMissing_throwsNpe() {
        assertThrows(NullPointerException.class,
                () -> Fa2Invoice.builder()
                        .issueDate(LocalDate.of(2026, 5, 9))
                        .seller(seller())
                        .buyer(buyer())
                        .totalGrossAmount(GROSS_AMOUNT)
                        .addLineItem(line())
                        .build());
    }

    @Test
    void build_whenHappyPath_producesFaWierszEntry() {
        Fa2Invoice invoice = Fa2Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 9))
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(line())
                .build();
        assertTrue(invoice.lineItems().size() == 1);
    }

    @Test
    void build_whenCorrectionTypeWithoutReference_throwsIllegalState() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Fa2Invoice.builder()
                        .invoiceNumber(INVOICE_NUMBER)
                        .issueDate(LocalDate.of(2026, 5, 9))
                        .seller(seller())
                        .buyer(buyer())
                        .rodzajFaktury(TRodzajFaktury.KOR)
                        .totalGrossAmount(GROSS_AMOUNT)
                        .addLineItem(line())
                        .build());
        assertTrue(ex.getMessage().contains("correction"));
    }

    private static InvoiceParty seller() {
        return new InvoiceParty(SELLER_NIP, "Acme", "00-001", "Warszawa", "Marszalkowska", "10", null);
    }

    private static InvoiceParty buyer() {
        return new InvoiceParty(BUYER_NIP, "Customer", "00-002", "Krakow", null, "5", null);
    }

    private static InvoiceLineItem line() {
        return new InvoiceLineItem(1, "Service", "szt.",
                new BigDecimal("1"), NET_AMOUNT, NET_AMOUNT, "23");
    }
}
