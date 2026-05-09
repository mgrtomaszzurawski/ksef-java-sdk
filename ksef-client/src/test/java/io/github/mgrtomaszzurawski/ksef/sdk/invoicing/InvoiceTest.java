/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Behavioural tests for {@link Invoice#fromXml(FormCode, byte[])} —
 * the minimal escape-hatch factory introduced in PR12a.
 *
 * <p>Pins:
 * <ul>
 *   <li>round-trip of {@code formCode} + {@code xml} bytes;</li>
 *   <li>defensive copy on input (mutation of the supplied array does
 *       not change {@link Invoice#xml()});</li>
 *   <li>defensive copy on accessor (each call returns an independent
 *       array);</li>
 *   <li>null-rejection for both arguments.</li>
 * </ul>
 */
class InvoiceTest {

    private static final byte[] SAMPLE_XML =
            "<Invoice><Number>FA/001</Number></Invoice>".getBytes(StandardCharsets.UTF_8);

    @Test
    void fromXml_roundTripsFormCodeAndXml() {
        Invoice invoice = Invoice.fromXml(FormCode.FA3, SAMPLE_XML);

        assertEquals(FormCode.FA3, invoice.formCode());
        assertArrayEquals(SAMPLE_XML, invoice.xml());
    }

    @Test
    void fromXml_defensivelyCopiesInputArray() {
        // given
        byte[] mutable = SAMPLE_XML.clone();
        Invoice invoice = Invoice.fromXml(FormCode.FA3, mutable);

        // when — caller mutates the array they passed in
        mutable[0] = 0;

        // then — the invoice's view is unchanged
        assertArrayEquals(SAMPLE_XML, invoice.xml(),
                "Invoice.fromXml must defensively copy the input array");
    }

    @Test
    void xml_returnsFreshArrayOnEachCall() {
        // given
        Invoice invoice = Invoice.fromXml(FormCode.FA3, SAMPLE_XML);

        // when
        byte[] firstAccess = invoice.xml();
        byte[] secondAccess = invoice.xml();

        // then — distinct arrays (defensive copy on accessor)
        assertNotSame(firstAccess, secondAccess,
                "Invoice.xml() must return a fresh array on every call");
        // Mutating one must not affect the other.
        firstAccess[0] = 0;
        assertArrayEquals(SAMPLE_XML, secondAccess,
                "Mutating one xml() result must not affect another");
    }

    @Test
    void fromXml_whenFormCodeNull_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> Invoice.fromXml(null, SAMPLE_XML));
    }

    @Test
    void fromXml_whenXmlNull_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> Invoice.fromXml(FormCode.FA3, null));
    }
}
