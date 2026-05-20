/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the read-side fallback wrapper: null guards, defensive byte[] copy
 * on construction and accessor, structural equality on {@code formCode}
 * + {@code xml}, and {@code toString} surface required for diagnostics.
 */
class UnrecognizedInvoiceDocumentTest {

    private static final FormCode UNKNOWN_CODE =
            FormCode.custom("UNK-1", "0-1", "Unknown");
    private static final byte[] XML_BYTES =
            "<Unknown xmlns=\"urn:test\"/>".getBytes(StandardCharsets.UTF_8);

    @Test
    void constructor_whenFormCodeNull_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> new UnrecognizedInvoiceDocument(null, XML_BYTES));
    }

    @Test
    void constructor_whenXmlNull_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> new UnrecognizedInvoiceDocument(UNKNOWN_CODE, null));
    }

    @Test
    void constructor_defensiveCopiesXmlBytes() {
        byte[] source = XML_BYTES.clone();
        UnrecognizedInvoiceDocument document = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, source);
        source[0] = (byte) 0xFF;
        assertEquals('<', (char) document.xml()[0],
                "Constructor must clone the xml bytes — mutating the source array must not affect the document.");
    }

    @Test
    void xml_returnsFreshCopyEachCall() {
        UnrecognizedInvoiceDocument document = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, XML_BYTES);
        byte[] first = document.xml();
        byte[] second = document.xml();
        assertNotSame(first, second, "xml() must return a defensive copy each call.");
        assertArrayEquals(first, second);
        first[0] = (byte) 0xFF;
        assertEquals('<', (char) document.xml()[0],
                "Mutating the returned array must not affect subsequent xml() calls.");
    }

    @Test
    void equalsAndHashCode_areStructuralOverFormCodeAndBytes() {
        UnrecognizedInvoiceDocument first = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, XML_BYTES);
        UnrecognizedInvoiceDocument second = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, XML_BYTES.clone());
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equals_isFalseWhenFormCodeDiffers() {
        UnrecognizedInvoiceDocument first = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, XML_BYTES);
        UnrecognizedInvoiceDocument second = new UnrecognizedInvoiceDocument(
                FormCode.custom("UNK-2", "0-1", "OtherUnknown"), XML_BYTES);
        assertNotEquals(first, second);
    }

    @Test
    void equals_isFalseWhenXmlBytesDiffer() {
        UnrecognizedInvoiceDocument first = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, XML_BYTES);
        byte[] mutated = XML_BYTES.clone();
        mutated[0] = (byte) 0xFE;
        UnrecognizedInvoiceDocument second = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, mutated);
        assertNotEquals(first, second);
    }

    @Test
    void equals_isFalseAgainstUnrelatedType() {
        UnrecognizedInvoiceDocument document = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, XML_BYTES);
        assertNotEquals("not-an-invoice-document", document);
    }

    @Test
    void toString_carriesFormCodeAndByteLength() {
        UnrecognizedInvoiceDocument document = new UnrecognizedInvoiceDocument(UNKNOWN_CODE, XML_BYTES);
        String rendered = document.toString();
        assertTrue(rendered.contains(UNKNOWN_CODE.toString()),
                () -> "toString should reference the form code: " + rendered);
        assertTrue(rendered.contains("byte[" + XML_BYTES.length + "]"),
                () -> "toString should expose the xml byte length: " + rendered);
    }
}
