/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;

import io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class Fa3InvoiceTest {

    private static final String SELLER_NIP = "1111111111";
    private static final String BUYER_NIP = "9876543210";
    private static final String INVOICE_NUMBER = "FA/2026/0001";

    @Test
    void formCode_whenInvoiceBuilt_returnsFa3() {
        // given
        Fa3Invoice invoice = InvoiceFixtures.minimalFa3Invoice();

        // when / then
        assertEquals(FormCode.FA3, invoice.formCode());
    }

    @Test
    void xml_whenInvoiceBuilt_roundTripsThroughJaxbUnchanged() throws Exception {
        // given
        Fa3Invoice invoice = InvoiceFixtures.minimalFa3Invoice();

        // when
        JAXBContext context = JAXBContext.newInstance(Faktura.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Faktura parsed = (Faktura) unmarshaller.unmarshal(new ByteArrayInputStream(invoice.xml()));

        // then
        assertNotNull(parsed);
        assertEquals(SELLER_NIP, parsed.getPodmiot1().getDaneIdentyfikacyjne().getNIP());
        assertEquals(BUYER_NIP, parsed.getPodmiot2().getDaneIdentyfikacyjne().getNIP());
        assertEquals(INVOICE_NUMBER, parsed.getFa().getP2());
    }

    @Test
    void xml_whenCalledTwice_returnsDefensiveCopies() {
        // given
        Fa3Invoice invoice = InvoiceFixtures.minimalFa3Invoice();

        // when
        byte[] firstCopy = invoice.xml();
        byte[] secondCopy = invoice.xml();

        // then
        assertNotSame(firstCopy, secondCopy);
        assertEquals(firstCopy.length, secondCopy.length);
    }

    static final class InvoiceFixtures {

        private static final BigDecimal NET_AMOUNT = new BigDecimal("100.00");
        private static final BigDecimal GROSS_AMOUNT = new BigDecimal("123.00");
        private static final String VAT_RATE = "23";
        private static final String DESCRIPTION = "Consulting";

        private InvoiceFixtures() {
        }

        static Fa3Invoice minimalFa3Invoice() {
            return Fa3Invoice.builder()
                    .invoiceNumber(INVOICE_NUMBER)
                    .issueDate(LocalDate.of(2026, 5, 9))
                    .seller(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty(
                            SELLER_NIP, "Acme sp. z o.o.", "00-001", "Warszawa", "Marszalkowska", "10", null))
                    .buyer(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty(
                            BUYER_NIP, "Customer sp. z o.o.", "00-002", "Krakow", null, "5", null))
                    .totalGrossAmount(GROSS_AMOUNT)
                    .addLineItem(new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem(
                            1, DESCRIPTION, "szt.", new BigDecimal("1"),
                            NET_AMOUNT, NET_AMOUNT, VAT_RATE))
                    .build();
        }
    }
}
