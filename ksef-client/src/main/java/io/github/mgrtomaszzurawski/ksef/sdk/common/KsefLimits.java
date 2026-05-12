/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

/**
 * Spec-defined and SDK-applied limits surfaced as public constants so
 * consumers can reason about page sizes, payload caps, and operation
 * budgets without hard-coding values.
 *
 * <p>Pagination is exposed via lazy {@link java.util.stream.Stream}
 * paginators ({@code archive().streamByMetadata}, {@code streamPersons},
 * {@code streamCertificates}, {@code streamTokens},
 * {@code sessions().stream}, ...). The SDK never imposes an upper bound
 * on stream length — caller controls memory by piping through
 * {@code .limit(N)} or {@code .toList()} as appropriate. Equivalent
 * in spirit to AWS SDK V2 paginators.
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
