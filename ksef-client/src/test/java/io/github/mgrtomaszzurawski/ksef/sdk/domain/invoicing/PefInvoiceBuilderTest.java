/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PefInvoiceBuilderTest {

    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal VAT = new BigDecimal("23");
    private static final String UNIT_CODE = "C62";

    @Test
    void build_whenInvoiceNumberMissing_throwsNpe() {
        assertThrows(NullPointerException.class,
                () -> PefInvoice.builder()
                        .issueDate(LocalDate.of(2026, 5, 9))
                        .supplier(party("1111111111", "Acme"))
                        .customer(party("9876543210", "Customer"))
                        .addLine(line())
                        .payableAmount(AMOUNT)
                        .build());
    }

    @Test
    void build_whenSupplierMissing_throwsNpe() {
        assertThrows(NullPointerException.class,
                () -> PefInvoice.builder()
                        .invoiceNumber("PEF/0001")
                        .issueDate(LocalDate.of(2026, 5, 9))
                        .customer(party("9876543210", "Customer"))
                        .addLine(line())
                        .payableAmount(AMOUNT)
                        .build());
    }

    @Test
    void build_whenHappyPath_producesUblInvoiceWithSinglePartyTaxScheme() {
        PefInvoice invoice = PefInvoice.builder()
                .invoiceNumber("PEF/0001")
                .issueDate(LocalDate.of(2026, 5, 9))
                .supplier(party("1111111111", "Acme"))
                .customer(party("9876543210", "Customer"))
                .addLine(line())
                .payableAmount(AMOUNT)
                .build();

        assertTrue(invoice.lines().size() == 1);
        assertEquals(1, invoice.unsafeJaxbView().getAccountingSupplierParty()
                .getParty().getPartyTaxScheme().size());
    }

    private static PefParty party(String taxId, String name) {
        return new PefParty(taxId, null, name, taxId,
                new PefAddress("Test 1", "Warsaw", "00-001", "PL"));
    }

    private static PefInvoiceLine line() {
        return new PefInvoiceLine("1", new BigDecimal("1"), UNIT_CODE,
                AMOUNT, "Goods", VAT);
    }
}
