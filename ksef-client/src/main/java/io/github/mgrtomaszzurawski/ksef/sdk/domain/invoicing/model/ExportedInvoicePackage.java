/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingMappers;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Decrypted, unzipped contents of an invoice export package.
 *
 * @param metadataJson raw {@code _metadata.json} bytes from the package, or
 *                     {@code null} when the export was {@code metadataOnly} and
 *                     no metadata file was emitted
 * @param invoiceXmls map of invoice file name (inside the ZIP) to invoice XML
 *                    bytes; never {@code null}, may be empty for empty exports
 *
 * @since 1.0.0
 */
public record ExportedInvoicePackage(byte @Nullable [] metadataJson, Map<String, byte[]> invoiceXmls) {

    private static final String METADATA_INVOICES_FIELD = "invoices";
    private static final String ERR_PARSE_METADATA = "Failed to parse _metadata.json from export package: ";
    private static final String ERR_READ_METADATA = "Failed to read _metadata.json from export package: ";
    /**
     * Pre-configured `ObjectMapper` shared across all {@link #invoiceMetadataList()}
     * calls — Jackson `ObjectMapper` is thread-safe once configured (canonical
     * "build once, reuse forever" pattern). Allocating a fresh mapper per call
     * was wasteful given module registration is non-trivial.
     */
    /**
     * Pre-configured `ObjectMapper` shared across all {@link #invoiceMetadataList()}
     * calls — Jackson `ObjectMapper` is thread-safe once configured (canonical
     * "build once, reuse forever" pattern). Uses Jackson 2.18 default
     * {@code StreamReadConstraints} (max depth 1000, max string 20 MB,
     * max number 1000) — the metadata bytes come from a SHA-256-verified
     * KSeF export over TLS, so the trust boundary is the upstream API
     * not arbitrary input.
     */
    private static final ObjectMapper METADATA_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ExportedInvoicePackage {
        metadataJson = metadataJson == null ? null : metadataJson.clone();
        invoiceXmls = invoiceXmls == null ? Map.of() : deepCopyByteMap(invoiceXmls);
    }

    @Override
    public byte @Nullable [] metadataJson() {
        return metadataJson == null ? null : metadataJson.clone();
    }

    @Override
    public Map<String, byte[]> invoiceXmls() {
        return deepCopyByteMap(invoiceXmls);
    }

    /**
     * Convenience accessor returning the invoice XML bytes for the given file
     * name, or {@code null} when the package does not contain that entry.
     */
    public byte @Nullable [] invoiceXml(String fileName) {
        byte[] bytes = invoiceXmls.get(fileName);
        return bytes == null ? null : bytes.clone();
    }

    private static Map<String, byte[]> deepCopyByteMap(Map<String, byte[]> source) {
        Map<String, byte[]> copy = new java.util.HashMap<>(source.size());
        for (Map.Entry<String, byte[]> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return Map.copyOf(copy);
    }

    /**
     * List of invoice file names present in the package, in arbitrary order.
     */
    public List<String> invoiceFileNames() {
        return List.copyOf(invoiceXmls.keySet());
    }

    /**
     * Parse {@code _metadata.json} into typed
     * {@link InvoiceMetadata} records. Per spec
     * {@code pobieranie-faktur/pobieranie-faktur.md}, the file contains
     * a top-level object with property {@code invoices} (an array of
     * {@code InvoiceMetadata} elements identical to the response of
     * {@code POST /invoices/query/metadata}).
     *
     * @return parsed list, or empty list when the export was
     *         {@code metadataOnly=false} and no metadata file was emitted
     *         (older API behaviour, before retention 27.10.2025)
     * @throws KsefException if the JSON is malformed
     *         ({@link com.fasterxml.jackson.core.JsonProcessingException}
     *         wrapped) or the metadata buffer cannot be read
     *         ({@link java.io.IOException} wrapped)
     *
     * @since 1.0.0
     */
    public List<InvoiceMetadata> invoiceMetadataList() {
        if (metadataJson == null || metadataJson.length == 0) {
            return List.of();
        }
        try {
            JsonNode root = METADATA_MAPPER.readTree(metadataJson);
            JsonNode invoicesNode = root.get(METADATA_INVOICES_FIELD);
            if (invoicesNode == null || !invoicesNode.isArray()) {
                return List.of();
            }
            InvoiceMetadataRaw[] rawArray = METADATA_MAPPER.treeToValue(invoicesNode, InvoiceMetadataRaw[].class);
            return Arrays.stream(rawArray)
                    .map(InvoicingMappers::toInvoiceMetadata)
                    .toList();
        } catch (JsonProcessingException jsonFailure) {
            throw new KsefException(ERR_PARSE_METADATA + jsonFailure.getMessage(), jsonFailure);
        } catch (IOException ioFailure) {
            throw new KsefException(ERR_READ_METADATA + ioFailure.getMessage(), ioFailure);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExportedInvoicePackage other)) {
            return false;
        }
        return Arrays.equals(metadataJson, other.metadataJson)
                && byteMapsEqual(invoiceXmls, other.invoiceXmls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(metadataJson), byteMapHashCode(invoiceXmls));
    }

    private static boolean byteMapsEqual(Map<String, byte[]> left, Map<String, byte[]> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (Map.Entry<String, byte[]> entry : left.entrySet()) {
            byte[] otherValue = right.get(entry.getKey());
            if (otherValue == null && !right.containsKey(entry.getKey())) {
                return false;
            }
            if (!Arrays.equals(entry.getValue(), otherValue)) {
                return false;
            }
        }
        return true;
    }

    private static int byteMapHashCode(Map<String, byte[]> source) {
        int result = 0;
        for (Map.Entry<String, byte[]> entry : source.entrySet()) {
            result += entry.getKey().hashCode() ^ Arrays.hashCode(entry.getValue());
        }
        return result;
    }

    @Override
    public String toString() {
        return "ExportedInvoicePackage[metadataJsonSize="
                + (metadataJson == null ? 0 : metadataJson.length)
                + ", invoiceCount=" + invoiceXmls.size() + "]";
    }
}
