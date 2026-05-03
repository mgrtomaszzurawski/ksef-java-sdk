/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import java.nio.file.Path;

/**
 * Consumer-implemented sink that receives each invoice during
 * {@link InvoiceSyncClient#sync}.
 *
 * <p>Called once per invoice processed in the current sync run. The
 * {@code xmlPath} points to the on-disk decrypted invoice XML, or
 * {@code null} when the SDK could not match an XML file in the export
 * package to the metadata entry (the file naming convention used by
 * KSeF inside the export ZIP is not strictly specified, so the
 * orchestrator does best-effort matching by KSeF number, ordinal, and
 * common patterns; null indicates none matched).
 *
 * <p>The sync orchestrator commits the matching {@link CheckpointStore}
 * entry only AFTER {@code accept(...)} has been invoked for every
 * invoice in the current export package, so an exception thrown here
 * aborts checkpoint advancement for the current window.
 *
 * <p>Implementations need not be thread-safe — the orchestrator calls
 * the sink from a single thread.
 */
@FunctionalInterface
public interface InvoiceSink {

    /**
     * Called once per invoice in the sync run.
     *
     * @param ksefNumber the validated KSeF number (length, format, CRC-8 checked) for this invoice
     * @param metadata the metadata returned by KSeF for this invoice
     * @param xmlPath on-disk path to the decrypted invoice XML, or
     *     {@code null} when the SDK could not match an XML file to this
     *     metadata entry
     */
    void accept(KsefNumber ksefNumber, InvoiceMetadata metadata, Path xmlPath);
}
