/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.examples;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.PefInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PEF(3) Peppol/UBL invoice authoring snippets — Polish public-procurement
 * invoices submitted through the KSeF PEF channel.
 */
public final class PefInvoiceExamples {

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
    private static final String UNIT_HOUR = "HUR";
    private static final BigDecimal SERVICE_AMOUNT = new BigDecimal("1500.00");
    private static final BigDecimal SERVICE_PAYABLE = new BigDecimal("1845.00");
    private static final BigDecimal GOODS_AMOUNT = new BigDecimal("250.00");
    private static final BigDecimal GOODS_PAYABLE = new BigDecimal("307.50");
    private static final BigDecimal SUBSCRIPTION_AMOUNT = new BigDecimal("99.00");
    private static final BigDecimal SUBSCRIPTION_PAYABLE = new BigDecimal("121.77");
    private static final BigDecimal VAT_23 = new BigDecimal("23");
    private static final BigDecimal QUANTITY_ONE = new BigDecimal("1");

    private PefInvoiceExamples() {
    }

    /** Single-line public-procurement service invoice. */
    public static PefInvoice serviceInvoice() {
        return PefInvoice.builder()
                .invoiceNumber("PEF/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 9))
                .currencyCode(CURRENCY_PLN)
                .supplier(supplier())
                .customer(customer())
                .addLine(new PefInvoiceLine("1", QUANTITY_ONE, UNIT_HOUR,
                        SERVICE_AMOUNT, "Architectural review", VAT_23))
                .payableAmount(SERVICE_PAYABLE)
                .build();
    }

    /** Goods sale invoice (single line). */
    public static PefInvoice goodsInvoice() {
        return PefInvoice.builder()
                .invoiceNumber("PEF/2026/0002")
                .issueDate(LocalDate.of(2026, 5, 9))
                .currencyCode(CURRENCY_PLN)
                .supplier(supplier())
                .customer(customer())
                .addLine(new PefInvoiceLine("1", QUANTITY_ONE, UNIT_PIECE,
                        GOODS_AMOUNT, "Office supplies bundle", VAT_23))
                .payableAmount(GOODS_PAYABLE)
                .build();
    }

    /** Subscription invoice (recurring service line). */
    public static PefInvoice subscriptionInvoice() {
        return PefInvoice.builder()
                .invoiceNumber("PEF/2026/0003")
                .issueDate(LocalDate.of(2026, 5, 9))
                .currencyCode(CURRENCY_PLN)
                .supplier(supplier())
                .customer(customer())
                .addLine(new PefInvoiceLine("1", QUANTITY_ONE, UNIT_PIECE,
                        SUBSCRIPTION_AMOUNT, "Monthly SaaS subscription", VAT_23))
                .payableAmount(SUBSCRIPTION_PAYABLE)
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
