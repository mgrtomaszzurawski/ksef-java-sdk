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
 * <p>Wire-level representation is mixed-case (e.g. {@code "InProgress"}).
 * Java naming convention requires {@code UPPER_SNAKE_CASE} enum constants;
 * use {@link #wireValue()} to obtain the spec-defined string when emitting
 * to the API.
 *
 * @since 1.0.0
 */
public enum CommonSessionStatus {
    /** Session active. */
    IN_PROGRESS("InProgress"),
    /**
     * Session processed successfully. Processing completed without errors;
     * individual invoices in the session may still have been rejected.
     */
    SUCCEEDED("Succeeded"),
    /**
     * Session not processed due to errors. Errors during session open or
     * close prevented successful processing.
     */
    FAILED("Failed"),
    /**
     * Session cancelled. Either the batch session upload time was exceeded,
     * or no invoices were submitted in an interactive session.
     */
    CANCELLED("Cancelled");

    private final String wireValue;

    CommonSessionStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the spec-defined wire-level string value (e.g. {@code "InProgress"}),
     * suitable for emitting to KSeF as a query parameter or JSON body field.
     */
    public String wireValue() {
        return wireValue;
    }
}
