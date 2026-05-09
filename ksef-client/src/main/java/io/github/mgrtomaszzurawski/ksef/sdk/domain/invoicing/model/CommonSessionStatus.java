/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Status of a KSeF session — mirrors OpenAPI {@code CommonSessionStatus}
 * (string enum on the wire, used as the {@code statuses} filter parameter
 * on {@code GET /sessions}).
 *
 * <p>Wire-level representation is the enum constant name verbatim (e.g.
 * {@code "InProgress"}), not the numeric status code returned by
 * {@code GET /sessions/{ref}}.
 *
 * @since 1.0.0
 */
public enum CommonSessionStatus {
    /** Session active. */
    InProgress,
    /**
     * Session processed successfully. Processing completed without errors;
     * individual invoices in the session may still have been rejected.
     */
    Succeeded,
    /**
     * Session not processed due to errors. Errors during session open or
     * close prevented successful processing.
     */
    Failed,
    /**
     * Session cancelled. Either the batch session upload time was exceeded,
     * or no invoices were submitted in an interactive session.
     */
    Cancelled
}
