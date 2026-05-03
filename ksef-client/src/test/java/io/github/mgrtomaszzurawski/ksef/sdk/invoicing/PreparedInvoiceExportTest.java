/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoicePackage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreparedInvoiceExportTest {

    @Test
    void prepareExportConstructor_whenAesKeyNull_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> new PreparedInvoiceExport(null, null, "ref", null, new byte[16]));
    }

    @Test
    void exportedInvoicePackage_invoiceXml_returnsClonedBytes() {
        byte[] xml = "<Faktura/>".getBytes(StandardCharsets.UTF_8);
        ExportedInvoicePackage pkg = new ExportedInvoicePackage(
                "{}".getBytes(StandardCharsets.UTF_8),
                java.util.Map.of("invoice-1.xml", xml));

        byte[] firstAccess = pkg.invoiceXml("invoice-1.xml");
        byte[] secondAccess = pkg.invoiceXml("invoice-1.xml");

        assertNotNull(firstAccess);
        assertNotNull(secondAccess);
        // returned arrays must be independent copies — mutating one cannot leak to the other
        firstAccess[0] = 0;
        org.junit.jupiter.api.Assertions.assertNotEquals(firstAccess[0], secondAccess[0]);
    }

    @Test
    void exportedInvoicePackage_invoiceFileNames_listsKeys() {
        ExportedInvoicePackage pkg = new ExportedInvoicePackage(
                null,
                java.util.Map.of(
                        "a.xml", new byte[]{1},
                        "b.xml", new byte[]{2}));

        org.junit.jupiter.api.Assertions.assertEquals(2, pkg.invoiceFileNames().size());
        org.junit.jupiter.api.Assertions.assertTrue(pkg.invoiceFileNames().contains("a.xml"));
    }

    @Test
    void invoiceQueryBuilder_canBeUsedAsPrepareExportFilter() {
        // smoke check that the builder type is reachable from the export workflow caller side
        InvoiceQueryBuilder query = InvoiceQueryBuilder.seller()
                .invoicingDateFrom(java.time.OffsetDateTime.now().minusDays(1));
        assertNotNull(query);
    }
}
