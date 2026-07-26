/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip guard for the {@code FaWiersz} scalar surface added to
 * {@link InvoiceLineItem}. Each of these accessors was ABSENT from the typed
 * overlay (the SDK never read them) until this change; the test builds an
 * invoice that carries them, marshals it through the SDK write path, reads it
 * back through the document reader, and asserts every value survives both
 * directions — the read/write parity the coverage doctrine requires.
 */
class LineItemFieldRoundTripTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String BUYER_NIP = "9876543210";
    private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 11);

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 5, 10);
    private static final String UUID_VALUE = "12345678-1234-1234-1234-123456789abc";
    private static final String INDEX_VALUE = "IDX-001";
    private static final String CN_CODE = "8471";
    private static final String PKOB_CODE = "1122";
    private static final BigDecimal GROSS_UNIT_PRICE = new BigDecimal("123.00");
    private static final BigDecimal DISCOUNT_AMOUNT = new BigDecimal("5.00");
    private static final BigDecimal EXCISE_AMOUNT = new BigDecimal("2.50");
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("4.3210");
    private static final BigDecimal VALUE_ADDED_TAX_RATE = new BigDecimal("23");
    private static final String GTU_CODE = "GTU_01";
    private static final String PROCEDURE_MARKING = "WSTO_EE";

    private static InvoiceLineItem richLine() {
        return InvoiceLineItem.builder()
                .rowNumber(1)
                .description("Consulting")
                .unitOfMeasure("szt.")
                .quantity(BigDecimal.ONE)
                .netUnitPrice(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("100.00"))
                .vatRate("23")
                .deliveryDate(DELIVERY_DATE)
                .uuid(UUID_VALUE)
                .index(INDEX_VALUE)
                .cnCode(CN_CODE)
                .pkobCode(PKOB_CODE)
                .grossUnitPrice(GROSS_UNIT_PRICE)
                .discountAmount(DISCOUNT_AMOUNT)
                .exciseAmount(EXCISE_AMOUNT)
                .exchangeRate(EXCHANGE_RATE)
                .valueAddedTaxRate(VALUE_ADDED_TAX_RATE)
                .annex15(true)
                .correctionStateBefore(true)
                .gtuCode(GTU_CODE)
                .procedureMarking(PROCEDURE_MARKING)
                .build();
    }

    private static void assertRoundTrip(InvoiceLineItem line) {
        assertEquals(DELIVERY_DATE, line.deliveryDate());
        assertEquals(UUID_VALUE, line.uuid());
        assertEquals(INDEX_VALUE, line.index());
        assertEquals(CN_CODE, line.cnCode());
        assertEquals(PKOB_CODE, line.pkobCode());
        assertEquals(0, GROSS_UNIT_PRICE.compareTo(line.grossUnitPrice()));
        assertEquals(0, DISCOUNT_AMOUNT.compareTo(line.discountAmount()));
        assertEquals(0, EXCISE_AMOUNT.compareTo(line.exciseAmount()));
        assertEquals(0, EXCHANGE_RATE.compareTo(line.exchangeRate()));
        assertEquals(0, VALUE_ADDED_TAX_RATE.compareTo(line.valueAddedTaxRate()));
        assertTrue(line.annex15());
        assertTrue(line.correctionStateBefore());
        assertEquals(GTU_CODE, line.gtuCode());
        assertEquals(PROCEDURE_MARKING, line.procedureMarking());
    }

    @Test
    void fa3_lineItemScalars_surviveWriteThenRead() {
        // given / when — build an FA(3) invoice carrying every FaWiersz scalar,
        // marshal it through the SDK write path, then read it back.
        byte[] xml = Fa3Invoice.builder()
                .invoiceNumber("FA/2026/RT/0001")
                .issueDate(ISSUE_DATE)
                .issueLocality("Warszawa")
                .seller(new InvoiceParty(SELLER_NIP, "Acme sp. z o.o.", "00-001",
                        "Warszawa", "Marszalkowska", "10", null))
                .buyer(new InvoiceParty(BUYER_NIP, "Customer sp. z o.o.", "00-002",
                        "Krakow", null, "5", null))
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(richLine())
                .build()
                .xml();

        Fa3InvoiceDocument document = Fa3InvoiceDocument.from(xml);

        // then
        assertEquals(1, document.lineItems().size());
        assertRoundTrip(document.lineItems().get(0));
    }

    @Test
    void fa2_lineItemScalars_surviveWriteThenRead() {
        // given / when — same round trip through the FA(2) write + read path.
        byte[] xml = Fa2Invoice.builder()
                .invoiceNumber("FA/2026/RT/0002")
                .issueDate(ISSUE_DATE)
                .seller(new InvoiceParty(SELLER_NIP, "Acme sp. z o.o.", "00-001",
                        "Warszawa", "Marszalkowska", "10", null))
                .buyer(new InvoiceParty(BUYER_NIP, "Customer sp. z o.o.", "00-002",
                        "Krakow", null, "5", null))
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(richLine())
                .build()
                .xml();

        Fa2InvoiceDocument document = Fa2InvoiceDocument.from(xml);

        // then
        assertEquals(1, document.lineItems().size());
        assertRoundTrip(document.lineItems().get(0));
    }
}
