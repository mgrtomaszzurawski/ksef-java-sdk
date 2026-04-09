/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;

import java.time.OffsetDateTime;

/**
 * Status of an invoice export job.
 *
 * @param status current export status
 * @param completedDate when the export completed (null if still processing)
 * @param packageExpirationDate when the download package expires
 * @param invoicePackage the downloadable package (null if not ready)
 */
public record InvoiceExportStatus(
        StatusInfo status,
        OffsetDateTime completedDate,
        OffsetDateTime packageExpirationDate,
        InvoicePackage invoicePackage) {

    public static InvoiceExportStatus from(InvoiceExportStatusResponseRaw raw) {
        return new InvoiceExportStatus(
                StatusInfo.from(raw.getStatus()),
                raw.getCompletedDate(),
                raw.getPackageExpirationDate(),
                InvoicePackage.from(raw.getPackage()));
    }
}
