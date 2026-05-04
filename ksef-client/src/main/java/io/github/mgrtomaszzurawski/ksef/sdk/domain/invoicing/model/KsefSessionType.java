/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * KSeF session type — mirrors OpenAPI {@code SessionType}.
 *
 * <p>Used by the {@code GET /sessions} listing filter (Codex round-9
 * manual-validation A.2.4).
 */
public enum KsefSessionType {
    /** Interactive (one-by-one) invoice submission. */
    ONLINE,
    /** Bulk (ZIP-of-invoices) submission. */
    BATCH;
}
