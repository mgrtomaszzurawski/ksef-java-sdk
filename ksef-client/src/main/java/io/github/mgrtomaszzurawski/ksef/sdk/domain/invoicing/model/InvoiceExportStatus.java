/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.core.StatusInfo;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Status of an invoice export job.
 *
 * @param status current export status
 * @param completedDate when the export completed (null if still processing)
 * @param packageExpirationDate when the download package expires
 * @param invoicePackage the downloadable package (null if not ready)
 *
 * @since 1.0.0
 */
public record InvoiceExportStatus(
        @Nullable StatusInfo status,
        @Nullable OffsetDateTime completedDate,
        @Nullable OffsetDateTime packageExpirationDate,
        @Nullable InvoicePackage invoicePackage) {

}
