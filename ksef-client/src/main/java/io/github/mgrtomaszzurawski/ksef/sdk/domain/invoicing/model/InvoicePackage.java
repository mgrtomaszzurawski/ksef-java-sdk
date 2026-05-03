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

}
