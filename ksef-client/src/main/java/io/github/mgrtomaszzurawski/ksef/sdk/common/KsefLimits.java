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
 * <p>Codex 2026-05-05 round-3 Performance review IMPORTANT — every
 * full-pagination helper ({@code queryAll*}, {@code listAll},
 * {@code getAllInvoices}, {@code queryAllSessions}) now caps the
 * returned list at {@link #DEFAULT_QUERY_RESULT_LIMIT} unless the
 * caller passes an explicit larger {@code maxResults}.
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

    private KsefLimits() { }
}
