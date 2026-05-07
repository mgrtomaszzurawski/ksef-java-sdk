/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Status of a KSeF session (online or batch).
 *
 * @param status current session status
 * @param dateCreated when the session was created
 * @param dateUpdated when the session was last updated
 * @param validUntil session expiration timestamp (null if session is closed)
 * @param upo UPO download information (null if not yet available)
 * @param invoiceCount total invoices submitted
 * @param successfulInvoiceCount successfully processed invoices
 * @param failedInvoiceCount failed invoices
 *
 * @since 1.0.0
 */
public record SessionStatus(
        StatusInfo status,
        OffsetDateTime dateCreated,
        OffsetDateTime dateUpdated,
        @Nullable OffsetDateTime validUntil,
        @Nullable UpoInfo upo,
        @Nullable Integer invoiceCount,
        @Nullable Integer successfulInvoiceCount,
        @Nullable Integer failedInvoiceCount) {

}
