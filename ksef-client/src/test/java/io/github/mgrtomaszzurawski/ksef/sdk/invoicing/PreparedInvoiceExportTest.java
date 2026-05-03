/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoicePackage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparedInvoiceExportTest {

    private static final String INVOICE_FILE_NAME = "invoice-1.xml";
    private static final String SAMPLE_INVOICE_XML = "<Faktura/>";
    private static final String METADATA_JSON = "{}";
    private static final byte[] BYTE_ONE = new byte[]{1};
    private static final byte[] BYTE_TWO = new byte[]{2};
    private static final int EXPECTED_INVOICE_COUNT = 2;

    @Test
    void exportedInvoicePackage_invoiceXml_returnsClonedBytes() {
        byte[] xml = SAMPLE_INVOICE_XML.getBytes(StandardCharsets.UTF_8);
        ExportedInvoicePackage pkg = new ExportedInvoicePackage(
                METADATA_JSON.getBytes(StandardCharsets.UTF_8),
                Map.of(INVOICE_FILE_NAME, xml));

        byte[] firstAccess = pkg.invoiceXml(INVOICE_FILE_NAME);
        byte[] secondAccess = pkg.invoiceXml(INVOICE_FILE_NAME);

        assertNotNull(firstAccess);
        assertNotNull(secondAccess);
        byte original = secondAccess[0];
        firstAccess[0] = (byte) (original + 1);
        assertNotEquals(firstAccess[0], secondAccess[0]);
    }

    @Test
    void exportedInvoicePackage_invoiceXmls_deepClonesEveryEntryValue() {
        byte[] mutableSource = SAMPLE_INVOICE_XML.getBytes(StandardCharsets.UTF_8);
        ExportedInvoicePackage pkg = new ExportedInvoicePackage(
                null, Map.of(INVOICE_FILE_NAME, mutableSource));

        byte[] entryFromBulkAccessor = pkg.invoiceXmls().get(INVOICE_FILE_NAME);
        byte original = entryFromBulkAccessor[0];
        entryFromBulkAccessor[0] = (byte) (original + 1);

        byte[] entryFromSubsequentAccess = pkg.invoiceXmls().get(INVOICE_FILE_NAME);
        assertEquals(original, entryFromSubsequentAccess[0]);
    }

    @Test
    void exportedInvoicePackage_invoiceFileNames_listsAllKeys() {
        ExportedInvoicePackage pkg = new ExportedInvoicePackage(
                null,
                Map.of("a.xml", BYTE_ONE, "b.xml", BYTE_TWO));

        assertEquals(EXPECTED_INVOICE_COUNT, pkg.invoiceFileNames().size());
        assertTrue(pkg.invoiceFileNames().contains("a.xml"));
        assertTrue(pkg.invoiceFileNames().contains("b.xml"));
    }
}
