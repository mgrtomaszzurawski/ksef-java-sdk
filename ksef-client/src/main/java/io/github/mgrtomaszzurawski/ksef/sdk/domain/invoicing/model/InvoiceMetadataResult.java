/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
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
 */
public record InvoiceMetadataResult(
        boolean hasMore,
        boolean isTruncated,
        OffsetDateTime permanentStorageHwmDate,
        List<InvoiceMetadata> invoices) {

}
