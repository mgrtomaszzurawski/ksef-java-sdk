/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.examples;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa2Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TKodWaluty;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TRodzajFaktury;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FA(2) invoice authoring snippets — TEST-environment-only schema.
 *
 * <p>FA(2) is accepted only on the KSeF TEST environment. Each
 * snippet returns an in-memory {@link Fa2Invoice} constructed via
 * the typed builder.
 */
public final class Fa2InvoiceExamples {

    private static final String SELLER_NIP = "1111111111";
    private static final String SELLER_NAME = "Legacy Vendor sp. z o.o.";
    private static final String SELLER_POSTAL = "00-001";
    private static final String SELLER_CITY = "Warszawa";
    private static final String SELLER_STREET = "Marszalkowska";
    private static final String SELLER_HOUSE = "10";
    private static final String BUYER_NIP = "9876543210";
    private static final String BUYER_NAME = "Legacy Customer sp. z o.o.";
    private static final String BUYER_POSTAL = "00-002";
    private static final String BUYER_CITY = "Krakow";
    private static final String BUYER_HOUSE = "5";
    private static final BigDecimal SERVICE_NET = new BigDecimal("500.00");
    private static final BigDecimal SERVICE_GROSS = new BigDecimal("615.00");
    private static final BigDecimal GOODS_NET = new BigDecimal("100.00");
    private static final BigDecimal GOODS_GROSS = new BigDecimal("123.00");
    private static final BigDecimal CORRECTION_GROSS = new BigDecimal("-50.00");
    private static final BigDecimal QUANTITY_ONE = new BigDecimal("1");
    private static final String VAT_23 = "23";
    private static final String UNIT_PIECE = "szt.";
    private static final int FIRST_LINE = 1;

    private Fa2InvoiceExamples() {
    }

    /** Single-line legacy service invoice. */
    public static Fa2Invoice singleLineService() {
        return Fa2Invoice.builder()
                .invoiceNumber("FA2/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 9))
                .issueLocality(SELLER_CITY)
                .currency(TKodWaluty.PLN)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(SERVICE_GROSS)
                .addLineItem(new InvoiceLineItem(FIRST_LINE, "Legacy maintenance",
                        UNIT_PIECE, QUANTITY_ONE, SERVICE_NET, SERVICE_NET, VAT_23))
                .build();
    }

    /** Goods sale invoice. */
    public static Fa2Invoice goodsSale() {
        return Fa2Invoice.builder()
                .invoiceNumber("FA2/2026/0002")
                .issueDate(LocalDate.of(2026, 5, 9))
                .issueLocality(SELLER_CITY)
                .currency(TKodWaluty.PLN)
                .seller(seller())
                .buyer(buyer())
                .totalGrossAmount(GOODS_GROSS)
                .addLineItem(new InvoiceLineItem(FIRST_LINE, "Mug",
                        UNIT_PIECE, QUANTITY_ONE, GOODS_NET, GOODS_NET, VAT_23))
                .build();
    }

    /** Correction invoice referencing the original document. */
    public static Fa2Invoice correctionInvoice() {
        return Fa2Invoice.builder()
                .invoiceNumber("FA2/KOR/2026/0001")
                .issueDate(LocalDate.of(2026, 5, 9))
                .issueLocality(SELLER_CITY)
                .currency(TKodWaluty.PLN)
                .seller(seller())
                .buyer(buyer())
                .rodzajFaktury(TRodzajFaktury.KOR)
                .correctionReference(new InvoiceCorrectionReference(
                        "FA2/2025/0099", LocalDate.of(2025, 12, 1)))
                .totalGrossAmount(CORRECTION_GROSS)
                .addLineItem(new InvoiceLineItem(FIRST_LINE, "Pricing correction",
                        UNIT_PIECE, QUANTITY_ONE, CORRECTION_GROSS, CORRECTION_GROSS, VAT_23))
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
