/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

/**
 * Spec-defined limits enforced internally by the batch and session
 * pipelines. NOT part of the public API — consumers learn about these
 * constraints through validation errors and the Javadoc on the relevant
 * builders/records, not by importing this class.
 *
 * @since 1.0.0
 */
public final class KsefLimits {

    /**
     * KSeF spec maximum size for one invoice including attachments — 3 MiB
     * ({@code 3 * 1024 * 1024 = 3_145_728} bytes). The spec text says "3 MB"
     * but the server enforces the binary value; using 3 × 10⁶ here would
     * leave 145 KiB of headroom unused.
     */
    public static final int MAX_INVOICE_BYTES = 3 * 1024 * 1024;

    /**
     * KSeF spec maximum size for one batch upload part (pre-encryption) —
     * 100 MiB ({@code 100 * 1024 * 1024} bytes). See
     * {@link #MAX_INVOICE_BYTES} for the MB / MiB distinction.
     */
    public static final long MAX_BATCH_PART_BYTES = 100L * 1024 * 1024;

    /** REQ-SESS-41 — KSeF caps a single session at 10 000 invoices. */
    public static final int MAX_SESSION_INVOICES = 10_000;

    /** KSeF spec maximum number of parts per batch upload (per {@code weryfikacja-faktury.md}). */
    public static final int MAX_BATCH_PARTS = 50;

    /** KSeF spec maximum aggregate batch payload size, pre-encryption (5 GiB per OpenAPI {@code BatchFile.fileSize}). */
    public static final long MAX_BATCH_TOTAL_BYTES = 5L * 1024L * 1024L * 1024L;

    private KsefLimits() { }
}
