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
 * <p>Every full-pagination helper ({@code queryAll*}, {@code listAll},
 * {@code queryAllSessions}) caps the returned list at
 * {@link #DEFAULT_QUERY_RESULT_LIMIT} as a safety against unbounded
 * heap growth on large subjects. The cap is reached silently — the
 * caller receives the truncated list and cannot detect drop versus
 * exhaustive walk by inspecting the result alone.
 *
 * <p>Currently only
 * {@code InvoiceClient.queryAllMetadata(query, int maxResults)} accepts
 * a caller-supplied override. For other helpers, the recommended
 * pattern when an exhaustive walk is required is to call the paged
 * variant ({@code query(...)} / {@code list(...)}) directly with an
 * explicit {@code pageOffset} loop.
 *
 * @since 1.0.0
 */
public final class KsefLimits {

    /**
     * Default ceiling on records returned by full-pagination helpers
     * when the caller does not supply an explicit {@code maxResults}.
     * Mirrors the spec single-session invoice cap (REQ-SESS-41) so the
     * default is generous enough for 99 % of consumer use cases while
     * preventing accidental whole-database pulls.
     */
    public static final int DEFAULT_QUERY_RESULT_LIMIT = 10_000;

    /** KSeF spec maximum size for one invoice (incl. attachments). */
    public static final int MAX_INVOICE_BYTES = 3 * 1024 * 1024;

    /** KSeF spec maximum size for one batch upload part (pre-encryption). */
    public static final long MAX_BATCH_PART_BYTES = 100L * 1024 * 1024;

    /** REQ-SESS-41 — KSeF caps a single session at 10 000 invoices. */
    public static final int MAX_SESSION_INVOICES = 10_000;

    /** KSeF spec maximum number of parts per batch upload (per {@code weryfikacja-faktury.md}). */
    public static final int MAX_BATCH_PARTS = 50;

    /** KSeF spec maximum aggregate batch payload size, pre-encryption (5 GiB per OpenAPI {@code BatchFile.fileSize}). */
    public static final long MAX_BATCH_TOTAL_BYTES = 5L * 1024L * 1024L * 1024L;

    private KsefLimits() { }
}
