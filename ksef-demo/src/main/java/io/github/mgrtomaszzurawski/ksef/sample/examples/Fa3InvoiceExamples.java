/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.examples;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Fa3Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodWaluty;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TRodzajFaktury;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FA(3) invoice authoring snippets — common business cases shown
 * end-to-end with the {@link Fa3Invoice#builder()} fluent API.
 *
 * <p>Each method is a static, side-effect-free constructor returning
 * an in-memory {@link Fa3Invoice}. Adapt fixture data to your own
 * tenant before sending.
 */
public final class Fa3InvoiceExamples {

    private static final String SELLER_NIP = "1111111111";
    private static final String SELLER_NAME = "Acme sp. z o.o.";
    private static final String SELLER_POSTAL = "00-001";
    private static final String SELLER_CITY = "Warszawa";
    private static final String SELLER_STREET = "Marszalkowska";
    private static final String SELLER_HOUSE = "10";
    private static final String BUYER_NIP = "9876543210";
    private static final String BUYER_NAME = "Customer sp. z o.o.";
    private static final String BUYER_POSTAL = "00-002";
    private static final String BUYER_CITY = "Krakow";
    private static final String BUYER_HOUSE = "5";
    private static final BigDecimal CONSULTING_NET = new BigDecimal("1000.00");
    private static final BigDecimal CONSULTING_GROSS = new BigDecimal("1230.00");
    private static final BigDecimal GOODS_NET = new BigDecimal("250.00");
    private static final BigDecimal GOODS_GROSS = new BigDecimal("307.50");
    private static final BigDecimal CORRECTION_GROSS = new BigDecimal("-100.00");
    private static final BigDecimal QUANTITY_ONE = new BigDecimal("1");
    private static final String VAT_23 = "23";
    private static final String UNIT_PIECE = "szt.";
    private static final String UNIT_HOUR = "h";
    private static final int FIRST_LINE = 1;

    private Fa3InvoiceExamples() {
    }

    /**
     * Single-line standard sale invoice — service rendered, paid in PLN.
     */
    public static Fa3Invoice singleLineService() {
        return Fa3Invoice.builder()
                .invoiceNumber("FA/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 9))
                .issueLocality(SELLER_CITY)
                .currency(TKodWaluty.PLN)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(CONSULTING_GROSS)
                .addLineItem(new InvoiceLineItem(FIRST_LINE, "Consulting services", null, null, UNIT_HOUR, QUANTITY_ONE, CONSULTING_NET, CONSULTING_NET, VAT_23, null, null))
                .build();
    }

    /**
     * Multi-line goods sale invoice — three product line items.
     */
    public static Fa3Invoice multiLineGoodsSale() {
        return Fa3Invoice.builder()
                .invoiceNumber("FA/2026/0002")
                .issueDate(LocalDate.of(2026, 5, 9))
                .issueLocality(SELLER_CITY)
                .currency(TKodWaluty.PLN)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GOODS_GROSS)
                .addLineItem(new InvoiceLineItem(FIRST_LINE, "Mug", null, null, UNIT_PIECE, QUANTITY_ONE, GOODS_NET, GOODS_NET, VAT_23, null, null))
                .build();
    }

    /**
     * Correction invoice ({@code RodzajFaktury=KOR}) referencing the
     * original invoice being corrected.
     */
    public static Fa3Invoice correctionInvoice() {
        return Fa3Invoice.builder()
                .invoiceNumber("FA/KOR/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 9))
                .issueLocality(SELLER_CITY)
                .currency(TKodWaluty.PLN)
                .seller(seller())
                .buyer(buyer())
                .rodzajFaktury(TRodzajFaktury.KOR)
                .correctionReference(new InvoiceCorrectionReference(
                        "FA/2025/0099", LocalDate.of(2025, 12, 1)))
                .totalGrossAmount(CORRECTION_GROSS)
                .addLineItem(new InvoiceLineItem(FIRST_LINE, "Pricing correction", null, null, UNIT_PIECE, QUANTITY_ONE, CORRECTION_GROSS, CORRECTION_GROSS, VAT_23, null, null))
                .build();
    }

    private static InvoiceParty seller() {
        return new InvoiceParty(SELLER_NIP, SELLER_NAME, SELLER_POSTAL, SELLER_CITY,
                SELLER_STREET, SELLER_HOUSE, null);
    }

    private static InvoiceParty buyer() {
        return new InvoiceParty(BUYER_NIP, BUYER_NAME, BUYER_POSTAL, BUYER_CITY,
                null, BUYER_HOUSE, null);
    }
}
