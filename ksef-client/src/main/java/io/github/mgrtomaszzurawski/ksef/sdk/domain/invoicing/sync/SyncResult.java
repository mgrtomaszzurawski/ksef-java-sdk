/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import java.util.Map;
import java.util.Objects;

/**
 * Outcome of {@link InvoiceSyncClient#sync}.
 *
 * @param processedCounts number of invoices successfully accepted by
 *     the {@link InvoiceSink} per subject type
 * @param finalCheckpoints the last checkpoint persisted per subject
 *     type (matches what {@link CheckpointStore} now holds)
 */
public record SyncResult(
        Map<InvoiceQuerySubjectType, Long> processedCounts,
        Map<InvoiceQuerySubjectType, SyncCheckpoint> finalCheckpoints) {

    public SyncResult {
        Objects.requireNonNull(processedCounts, "processedCounts must not be null");
        Objects.requireNonNull(finalCheckpoints, "finalCheckpoints must not be null");
        processedCounts = Map.copyOf(processedCounts);
        finalCheckpoints = Map.copyOf(finalCheckpoints);
    }

    public long totalProcessed() {
        return processedCounts.values().stream().mapToLong(Long::longValue).sum();
    }
}
