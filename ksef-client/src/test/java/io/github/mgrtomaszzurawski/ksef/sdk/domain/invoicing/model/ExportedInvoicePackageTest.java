/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins {@link ExportedInvoicePackage} defensive copies on construction
 * and on the {@code invoiceXmls()} accessor — Sonar S2384 mutable
 * byte[] in a record needs explicit deep-copy on both paths.
 */
class ExportedInvoicePackageTest {

    private static final byte[] META_JSON = "{\"invoices\":[]}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INVOICE_A = "<FakturaA/>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INVOICE_B = "<FakturaB/>".getBytes(StandardCharsets.UTF_8);

    @Test
    void compactCtor_deepCopiesInvoiceXmlsMap() {
        Map<String, byte[]> source = new HashMap<>();
        source.put("a.xml", INVOICE_A.clone());

        ExportedInvoicePackage exportPackage = new ExportedInvoicePackage(META_JSON, source);

        // Mutate both the source map and the byte[] entry — the snapshot must stay intact.
        source.put("b.xml", INVOICE_B);
        Map<String, byte[]> view = exportPackage.invoiceXmls();
        assertEquals(1, view.size(), "Map entries must be snapshotted on construction.");
        assertArrayEquals(INVOICE_A, view.get("a.xml"));
    }

    @Test
    void invoiceXmls_returnsFreshDeepCopyOnEveryCall() {
        Map<String, byte[]> source = new HashMap<>();
        source.put("a.xml", INVOICE_A.clone());
        ExportedInvoicePackage exportPackage = new ExportedInvoicePackage(META_JSON, source);

        Map<String, byte[]> first = exportPackage.invoiceXmls();
        Map<String, byte[]> second = exportPackage.invoiceXmls();

        assertNotSame(first, second, "Each accessor call returns a fresh map.");
        assertNotSame(first.get("a.xml"), second.get("a.xml"), "Each entry is a fresh byte[] clone.");

        first.get("a.xml")[0] = (byte) 0xFF;
        assertEquals('<', (char) exportPackage.invoiceXmls().get("a.xml")[0],
                "Mutating returned bytes must not affect subsequent reads.");
    }

    @Test
    void invoiceXml_byFilenameReturnsCloneAndNullForMissing() {
        Map<String, byte[]> source = new HashMap<>();
        source.put("a.xml", INVOICE_A.clone());
        ExportedInvoicePackage exportPackage = new ExportedInvoicePackage(META_JSON, source);

        byte[] invoiceAXml = exportPackage.invoiceXml("a.xml");
        assertArrayEquals(INVOICE_A, invoiceAXml);
        assertNotSame(source.get("a.xml"), invoiceAXml);

        assertNull(exportPackage.invoiceXml("missing.xml"));
    }

    @Test
    void compactCtor_nullInvoiceXmlsDefaultsToEmptyMap() {
        ExportedInvoicePackage exportPackage = new ExportedInvoicePackage(META_JSON, null);
        assertEquals(0, exportPackage.invoiceXmls().size());
    }

    @Test
    void metadataJson_returnsFreshCopyEachCall() {
        ExportedInvoicePackage exportPackage = new ExportedInvoicePackage(META_JSON, Map.of());
        byte[] first = exportPackage.metadataJson();
        byte[] second = exportPackage.metadataJson();
        assertNotSame(first, second);
        assertArrayEquals(META_JSON, first);
    }

    @Test
    void metadataJson_returnsNullWhenAbsentOnConstruction() {
        ExportedInvoicePackage exportPackage = new ExportedInvoicePackage(null, Map.of());
        assertNull(exportPackage.metadataJson());
    }
}
