/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decrypted, unzipped contents of an invoice export package.
 *
 * @param metadataJson raw {@code _metadata.json} bytes from the package, or
 *                     {@code null} when the export was {@code metadataOnly} and
 *                     no metadata file was emitted
 * @param invoiceXmls map of invoice file name (inside the ZIP) to invoice XML
 *                    bytes; never {@code null}, may be empty for empty exports
 */
public record ExportedInvoicePackage(byte[] metadataJson, Map<String, byte[]> invoiceXmls) {

    public ExportedInvoicePackage {
        metadataJson = metadataJson == null ? null : metadataJson.clone();
        invoiceXmls = invoiceXmls == null ? Map.of() : Map.copyOf(invoiceXmls);
    }

    @Override
    public byte[] metadataJson() {
        return metadataJson == null ? null : metadataJson.clone();
    }

    /**
     * Convenience accessor returning the invoice XML bytes for the given file
     * name, or {@code null} when the package does not contain that entry.
     */
    public byte[] invoiceXml(String fileName) {
        byte[] bytes = invoiceXmls.get(fileName);
        return bytes == null ? null : bytes.clone();
    }

    /**
     * List of invoice file names present in the package, in arbitrary order.
     */
    public List<String> invoiceFileNames() {
        return List.copyOf(invoiceXmls.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExportedInvoicePackage other)) {
            return false;
        }
        return java.util.Arrays.equals(metadataJson, other.metadataJson)
                && Objects.equals(invoiceXmls, other.invoiceXmls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceXmls, java.util.Arrays.hashCode(metadataJson));
    }

    @Override
    public String toString() {
        return "ExportedInvoicePackage[metadataJsonSize="
                + (metadataJson == null ? 0 : metadataJson.length)
                + ", invoiceCount=" + invoiceXmls.size() + "]";
    }
}
