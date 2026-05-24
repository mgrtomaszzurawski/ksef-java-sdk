/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.testfixtures;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Fa3Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Test fixtures for FA(3) invoices that survive XSD validation.
 *
 * <p>Used by WireMock tests so the XSD-validator gate inside
 * {@code OnlineSession.sendInvoice} / {@code Invoices.submitBatch}
 * actually runs against a real schema-conformant payload — replaces
 * the prior {@code FormCode.custom("FA (TEST)", ...)} fixtures that
 * silently bypassed the validator path (F-5 follow-up).
 */
public final class Fa3InvoiceFixtures {

    private static final String SELLER_NIP = "1111111111";
    private static final String BUYER_NIP = "9876543210";
    private static final String INVOICE_NUMBER = "FA/2026/FIXTURE/0001";
    private static final BigDecimal NET_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");
    private static final String VAT_RATE = "23";
    private static final String UNIT = "szt.";
    private static final String DESCRIPTION = "Consulting";
    private static final String LOCALITY = "Warszawa";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);

    private Fa3InvoiceFixtures() {
    }

    /**
     * Minimal FA(3) invoice that passes XSD validation. Single line item,
     * one seller, one buyer, no corrections, no payment block.
     */
    public static Fa3Invoice minimalValid() {
        return Fa3Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(ISSUE_DATE)
                .issueLocality(LOCALITY)
                .seller(new InvoiceParty(SELLER_NIP, "Acme sp. z o.o.", "00-001",
                        LOCALITY, "Marszalkowska", "10", null))
                .buyer(new InvoiceParty(BUYER_NIP, "Customer sp. z o.o.", "00-002",
                        "Krakow", null, "5", null))
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(new InvoiceLineItem(1, DESCRIPTION, null, null, UNIT, BigDecimal.ONE, NET_AMOUNT, NET_AMOUNT, VAT_RATE, null, null))
                .build();
    }
}
