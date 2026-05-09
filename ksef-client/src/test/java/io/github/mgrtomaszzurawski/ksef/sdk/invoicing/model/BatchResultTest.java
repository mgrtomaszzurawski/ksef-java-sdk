/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FailedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoEntry;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the count invariants {@link BatchResult} enforces in its compact
 * constructor: caller-supplied counts must match the embedded list sizes,
 * and {@code totalCount} must equal {@code successfulCount + failedCount}.
 *
 * <p>These guards exist so that downstream consumers reading individual
 * count accessors cannot silently observe a state that disagrees with
 * the cleared/failed lists they iterate.
 */
class BatchResultTest {

    private static final String REF = "20260509-SE-1234";
    private static final byte[] UPO_BYTES = new byte[] { 1, 2, 3 };
    private static final OffsetDateTime STARTED = OffsetDateTime.parse("2026-05-09T10:00:00Z");
    private static final OffsetDateTime COMPLETED = OffsetDateTime.parse("2026-05-09T10:05:00Z");

    @Test
    void constructor_whenAllInvariantsHold_succeeds() {
        UpoEntry cleared = new UpoEntry("inv-1", UPO_BYTES);
        FailedInvoice failed = new FailedInvoice("inv-2", "rejected", List.of("detail"));

        BatchResult result = new BatchResult(
                REF, List.of(cleared), List.of(failed), 2, 1, 1, STARTED, COMPLETED);

        assertEquals(1, result.successfulCount());
        assertEquals(1, result.failedCount());
        assertEquals(2, result.totalCount());
    }

    @Test
    void constructor_whenSuccessfulCountMismatchesClearedSize_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new BatchResult(REF, List.of(new UpoEntry("inv-1", UPO_BYTES)),
                        List.of(), 1, 0, 0, STARTED, COMPLETED));

        assertTrue(ex.getMessage().contains("successfulCount"));
    }

    @Test
    void constructor_whenFailedCountMismatchesFailedSize_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new BatchResult(REF, List.of(),
                        List.of(new FailedInvoice("inv-1", "err", List.of())),
                        1, 0, 0, STARTED, COMPLETED));

        assertTrue(ex.getMessage().contains("failedCount"));
    }

    @Test
    void constructor_whenTotalCountMismatchesSum_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new BatchResult(REF, List.of(), List.of(), 5, 0, 0, STARTED, COMPLETED));

        assertTrue(ex.getMessage().contains("totalCount"));
    }

    @Test
    void constructor_copiesClearedAndFailedDefensively() {
        java.util.List<UpoEntry> mutableCleared = new java.util.ArrayList<>();
        mutableCleared.add(new UpoEntry("inv-1", UPO_BYTES));
        java.util.List<FailedInvoice> mutableFailed = new java.util.ArrayList<>();
        mutableFailed.add(new FailedInvoice("inv-2", "err", List.of()));

        BatchResult result = new BatchResult(
                REF, mutableCleared, mutableFailed, 2, 1, 1, STARTED, COMPLETED);

        assertNotSame(mutableCleared, result.cleared());
        assertNotSame(mutableFailed, result.failed());
    }
}
