/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * One decrypted invoice produced by {@code Invoices.syncAsStream(...)}.
 * Carries the {@link KsefNumber}, the metadata record returned by the
 * server's incremental sync endpoint, the decrypted XML bytes, and an
 * optional file path when the caller's
 * {@link IncrementalSyncPlan} requested file-output mode.
 *
 * <p>The {@code xml} bytes are defensive-copied on construction and on
 * every accessor call. {@code xmlPath} is empty when the consumer
 * requested in-memory mode (no spilling to disk).
 *
 * @param ksefNumber server-assigned unique invoice number
 * @param metadata invoice metadata record returned by the sync query
 * @param xml decrypted invoice XML bytes (bit-exact)
 * @param xmlPath on-disk location when the plan requested file-output;
 *     empty otherwise
 *
 * @since 1.0.0
 */
public record DecryptedInvoice(
        KsefNumber ksefNumber,
        InvoiceMetadata metadata,
        byte[] xml,
        Optional<Path> xmlPath) {

    private static final String ERR_NULL_KSEF = "ksefNumber must not be null";
    private static final String ERR_NULL_METADATA = "metadata must not be null";
    private static final String ERR_NULL_XML = "xml must not be null";
    private static final String ERR_NULL_PATH = "xmlPath must not be null";

    public DecryptedInvoice {
        Objects.requireNonNull(ksefNumber, ERR_NULL_KSEF);
        Objects.requireNonNull(metadata, ERR_NULL_METADATA);
        Objects.requireNonNull(xml, ERR_NULL_XML);
        Objects.requireNonNull(xmlPath, ERR_NULL_PATH);
        xml = xml.clone();
    }

    @Override
    public byte[] xml() {
        return xml.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DecryptedInvoice that)) {
            return false;
        }
        return Objects.equals(ksefNumber, that.ksefNumber)
                && Objects.equals(metadata, that.metadata)
                && Arrays.equals(xml, that.xml)
                && Objects.equals(xmlPath, that.xmlPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ksefNumber, metadata, Arrays.hashCode(xml), xmlPath);
    }

    @Override
    public String toString() {
        return "DecryptedInvoice[ksefNumber=" + ksefNumber.value()
                + ", xml=byte[" + xml.length + "]"
                + ", xmlPath=" + xmlPath.map(Path::toString).orElse("<in-memory>") + "]";
    }
}
