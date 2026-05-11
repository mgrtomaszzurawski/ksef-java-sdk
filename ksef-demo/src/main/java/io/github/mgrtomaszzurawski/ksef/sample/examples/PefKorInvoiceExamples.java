/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.examples;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefKorInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PEF_KOR(3) UBL credit-note authoring snippets — corrections of
 * earlier PEF(3) invoices.
 */
public final class PefKorInvoiceExamples {

    private static final String SUPPLIER_NIP = "1111111111";
    private static final String CUSTOMER_NIP = "9876543210";
    private static final String SUPPLIER_NAME = "Acme sp. z o.o.";
    private static final String CUSTOMER_NAME = "Public Buyer sp. z o.o.";
    private static final String STREET_NAME = "Marszalkowska 10";
    private static final String CITY_NAME = "Warszawa";
    private static final String POSTAL_ZONE = "00-001";
    private static final String COUNTRY_CODE_PL = "PL";
    private static final String CURRENCY_PLN = "PLN";
    private static final String UNIT_PIECE = "C62";
    private static final BigDecimal CORRECTION_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal QUANTITY_REFUND = new BigDecimal("100.00");
    private static final BigDecimal CORRECTION_PAYABLE = new BigDecimal("123.00");
    private static final BigDecimal PARTIAL_REFUND_AMOUNT = new BigDecimal("50.00");
    private static final BigDecimal PARTIAL_REFUND_PAYABLE = new BigDecimal("61.50");
    private static final BigDecimal QUANTITY_PRICE_FIX_AMOUNT = new BigDecimal("25.00");
    private static final BigDecimal QUANTITY_PRICE_FIX_PAYABLE = new BigDecimal("30.75");
    private static final BigDecimal VAT_23 = new BigDecimal("23");
    private static final BigDecimal QUANTITY_ONE = new BigDecimal("1");

    private PefKorInvoiceExamples() {
    }

    /** Full refund correction note — entire original invoice cancelled. */
    public static PefKorInvoice fullRefundCorrection() {
        return PefKorInvoice.builder()
                .creditNoteNumber("PEFKOR/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 9))
                .currencyCode(CURRENCY_PLN)
                .supplier(supplier())
                .customer(customer())
                .addLine(new PefInvoiceLine("1", QUANTITY_ONE, UNIT_PIECE,
                        CORRECTION_AMOUNT, "Refund of original invoice", VAT_23))
                .payableAmount(CORRECTION_PAYABLE)
                .originalInvoiceNumber("PEF/2025/0099")
                .build();
    }

    /** Partial refund — adjust the original invoice by a partial amount. */
    public static PefKorInvoice partialRefundCorrection() {
        return PefKorInvoice.builder()
                .creditNoteNumber("PEFKOR/2026/0002")
                .issueDate(LocalDate.of(2026, 5, 9))
                .currencyCode(CURRENCY_PLN)
                .supplier(supplier())
                .customer(customer())
                .addLine(new PefInvoiceLine("1", QUANTITY_ONE, UNIT_PIECE,
                        PARTIAL_REFUND_AMOUNT, "Partial refund — discount applied", VAT_23))
                .payableAmount(PARTIAL_REFUND_PAYABLE)
                .originalInvoiceNumber("PEF/2025/0100")
                .build();
    }

    /** Quantity / price fix — small correction to a previous invoice line. */
    public static PefKorInvoice quantityPriceFix() {
        return PefKorInvoice.builder()
                .creditNoteNumber("PEFKOR/2026/0003")
                .issueDate(LocalDate.of(2026, 5, 9))
                .currencyCode(CURRENCY_PLN)
                .supplier(supplier())
                .customer(customer())
                .addLine(new PefInvoiceLine("1", QUANTITY_ONE, UNIT_PIECE,
                        QUANTITY_PRICE_FIX_AMOUNT, "Quantity correction", VAT_23))
                .payableAmount(QUANTITY_PRICE_FIX_PAYABLE)
                .originalInvoiceNumber("PEF/2025/0101")
                .build();
    }

    private static PefParty supplier() {
        return new PefParty(SUPPLIER_NIP, null, SUPPLIER_NAME, SUPPLIER_NIP, address());
    }

    private static PefParty customer() {
        return new PefParty(CUSTOMER_NIP, null, CUSTOMER_NAME, CUSTOMER_NIP, address());
    }

    private static PefAddress address() {
        return new PefAddress(STREET_NAME, CITY_NAME, POSTAL_ZONE, COUNTRY_CODE_PL);
    }
}
