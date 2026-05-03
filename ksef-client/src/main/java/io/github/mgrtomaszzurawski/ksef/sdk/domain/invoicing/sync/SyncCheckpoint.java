/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Per-subject-type checkpoint for incremental invoice sync.
 *
 * <p>Holds the cursor (last processed timestamp) used to resume the next
 * sync run from where the previous one left off.
 *
 * <p>Spec citation: REQ-HWM-001/002/003 in
 * {@code context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md}.
 *
 * @param cursor last successfully processed permanent-storage timestamp
 *     (advances by {@link InvoicePackage#continuationCursor()} on each
 *     window)
 * @param lastTruncated whether the last window was truncated
 */
public record SyncCheckpoint(OffsetDateTime cursor, boolean lastTruncated) {

    private static final String ERR_NULL_CURSOR = "cursor must not be null";

    public SyncCheckpoint {
        Objects.requireNonNull(cursor, ERR_NULL_CURSOR);
    }
}
