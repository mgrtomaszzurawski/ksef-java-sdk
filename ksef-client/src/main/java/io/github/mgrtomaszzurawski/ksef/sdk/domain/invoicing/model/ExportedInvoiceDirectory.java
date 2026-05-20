/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * File-backed result of
 * {@code PreparedInvoiceExport.downloadAndDecryptTo(status, outputDir)}.
 *
 * <p>Each invoice XML and the optional {@code _metadata.json} are written
 * to the supplied output directory. This handle returns the resulting
 * paths instead of the byte arrays — useful for export packages too
 * large to hold in heap.
 *
 * @param outputDirectory directory where contents were written
 * @param metadataJson path to {@code _metadata.json}, or {@code null} if
 *     the export had no metadata file
 * @param invoiceXmls map from invoice file name (inside the ZIP) to its
 *     on-disk path
 *
 * @since 0.1.0
 */
public record ExportedInvoiceDirectory(
        Path outputDirectory,
        @Nullable Path metadataJson,
        Map<String, Path> invoiceXmls) {

    private static final String ERR_NULL_DIR = "outputDirectory must not be null";
    private static final String ERR_NULL_INVOICES = "invoiceXmls must not be null";

    public ExportedInvoiceDirectory {
        Objects.requireNonNull(outputDirectory, ERR_NULL_DIR);
        Objects.requireNonNull(invoiceXmls, ERR_NULL_INVOICES);
        invoiceXmls = Map.copyOf(invoiceXmls);
    }

    /**
     * List of invoice file names present, in arbitrary order.
     */
    public List<String> invoiceFileNames() {
        return List.copyOf(invoiceXmls.keySet());
    }

    /**
     * On-disk path for the named invoice XML, or {@code null} if absent.
     */
    public @Nullable Path invoiceXml(String fileName) {
        return invoiceXmls.get(fileName);
    }
}
