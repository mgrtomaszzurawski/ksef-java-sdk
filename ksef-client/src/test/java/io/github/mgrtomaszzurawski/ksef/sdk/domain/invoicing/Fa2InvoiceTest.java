/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class Fa2InvoiceTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String BUYER_NIP = "9876543210";
    private static final String INVOICE_NUMBER = "FA/2026/0001";
    private static final BigDecimal NET_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");
    private static final String VAT_RATE = "23";

    @Test
    void formCode_whenInvoiceBuilt_returnsFa2() {
        Fa2Invoice invoice = minimalInvoice();
        assertEquals(FormCode.FA2, invoice.formCode());
    }

    @Test
    void xml_whenInvoiceBuilt_roundTripsThroughJaxbUnchanged() throws Exception {
        Fa2Invoice invoice = minimalInvoice();
        JAXBContext context = JAXBContext.newInstance(Faktura.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Faktura parsed = (Faktura) unmarshaller.unmarshal(new ByteArrayInputStream(invoice.xml()));
        assertNotNull(parsed);
        assertEquals(SELLER_NIP, parsed.getPodmiot1().getDaneIdentyfikacyjne().getNIP());
        assertEquals(INVOICE_NUMBER, parsed.getFa().getP2());
    }

    @Test
    void xml_whenCalledTwice_returnsDefensiveCopies() {
        Fa2Invoice invoice = minimalInvoice();
        byte[] firstCopy = invoice.xml();
        byte[] secondCopy = invoice.xml();
        assertNotSame(firstCopy, secondCopy);
        assertEquals(firstCopy.length, secondCopy.length);
    }

    private static Fa2Invoice minimalInvoice() {
        return Fa2Invoice.builder()
                .invoiceNumber(INVOICE_NUMBER)
                .issueDate(LocalDate.of(2026, 5, 9))
                .seller(new InvoiceParty(SELLER_NIP, "Acme", "00-001", "Warszawa", "Marszalkowska", "10", null))
                .buyer(new InvoiceParty(BUYER_NIP, "Customer", "00-002", "Krakow", null, "5", null))
                .totalGrossAmount(GROSS_AMOUNT)
                .addLineItem(new InvoiceLineItem(1, "Consulting", "szt.",
                        new BigDecimal("1"), NET_AMOUNT, NET_AMOUNT, VAT_RATE))
                .build();
    }
}
