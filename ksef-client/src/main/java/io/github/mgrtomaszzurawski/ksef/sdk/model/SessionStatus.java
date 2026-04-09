/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SessionStatusResponseRaw;

import java.time.OffsetDateTime;

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
 */
public record SessionStatus(
        StatusInfo status,
        OffsetDateTime dateCreated,
        OffsetDateTime dateUpdated,
        OffsetDateTime validUntil,
        UpoInfo upo,
        Integer invoiceCount,
        Integer successfulInvoiceCount,
        Integer failedInvoiceCount) {

    public static SessionStatus from(SessionStatusResponseRaw raw) {
        return new SessionStatus(
                StatusInfo.from(raw.getStatus()),
                raw.getDateCreated(),
                raw.getDateUpdated(),
                raw.getValidUntil(),
                UpoInfo.from(raw.getUpo()),
                raw.getInvoiceCount(),
                raw.getSuccessfulInvoiceCount(),
                raw.getFailedInvoiceCount());
    }
}
