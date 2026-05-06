/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Result of querying invoice metadata.
 *
 * @param hasMore whether more results are available
 * @param isTruncated whether the result set was truncated
 * @param permanentStorageHwmDate high-water-mark date for permanent storage
 * @param invoices list of invoice metadata
 *
 * @since 1.0.0
 */
public record InvoiceMetadataResult(
        boolean hasMore,
        boolean isTruncated,
        OffsetDateTime permanentStorageHwmDate,
        List<InvoiceMetadata> invoices) {

}
