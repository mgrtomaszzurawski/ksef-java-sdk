/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Invoice export package with download parts.
 *
 * @param invoiceCount number of invoices in the package
 * @param size total package size in bytes
 * @param parts downloadable package parts
 * @param isTruncated whether the package was truncated
 * @param lastIssueDate last issue date in the package
 * @param lastInvoicingDate last invoicing date in the package
 * @param lastPermanentStorageDate last permanent storage date
 * @param permanentStorageHwmDate high-water-mark date for permanent storage
 *
 * @since 1.0.0
 */
public record InvoicePackage(
        Long invoiceCount,
        Long size,
        List<InvoicePackagePart> parts,
        Boolean isTruncated,
        LocalDate lastIssueDate,
        OffsetDateTime lastInvoicingDate,
        OffsetDateTime lastPermanentStorageDate,
        OffsetDateTime permanentStorageHwmDate) {

    /**
     * Continuation point for incremental sync. Per spec
     * ({@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md:258-264}):
     *
     * <ul>
     *   <li>truncated package ({@code isTruncated == true}) — return
     *       {@link #lastPermanentStorageDate};</li>
     *   <li>untruncated package — return
     *       {@link #permanentStorageHwmDate}.</li>
     * </ul>
     *
     * @return next-window cursor, or {@code null} if no continuation is
     *     possible (both candidate fields are null)
     *
     * Spec citation: REQ-HWM-002, REQ-HWM-003.
     */
    public OffsetDateTime continuationCursor() {
        if (Boolean.TRUE.equals(isTruncated) && lastPermanentStorageDate != null) {
            return lastPermanentStorageDate;
        }
        return permanentStorageHwmDate;
    }
}
