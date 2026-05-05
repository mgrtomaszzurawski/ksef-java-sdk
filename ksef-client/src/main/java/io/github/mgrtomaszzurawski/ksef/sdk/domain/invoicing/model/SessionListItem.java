/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One row from the {@code GET /sessions} listing — a session reference,
 * its current status, and aggregate counts.
 *
 * <p>Mirrors OpenAPI {@code SessionsQueryResponseItem}.
 *
 * @param referenceNumber the session reference number
 * @param status terminal/in-flight status (StatusInfo carries code + description)
 * @param dateCreated when the session opened
 * @param dateUpdated last status update
 * @param validUntil session expiration deadline
 * @param totalInvoiceCount total invoices submitted in the session
 * @param successfulInvoiceCount how many of those succeeded
 * @param failedInvoiceCount how many failed
 *
 * @since 1.0.0
 */
public record SessionListItem(
        String referenceNumber,
        @Nullable StatusInfo status,
        @Nullable OffsetDateTime dateCreated,
        @Nullable OffsetDateTime dateUpdated,
        @Nullable OffsetDateTime validUntil,
        @Nullable Integer totalInvoiceCount,
        @Nullable Integer successfulInvoiceCount,
        @Nullable Integer failedInvoiceCount) {
}
