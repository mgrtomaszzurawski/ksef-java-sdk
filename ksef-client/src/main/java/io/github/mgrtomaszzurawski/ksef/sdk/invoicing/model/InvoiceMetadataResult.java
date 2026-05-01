/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;
import io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model.InvoiceMetadata;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;

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

    public static InvoiceMetadataResult from(QueryInvoicesMetadataResponseRaw raw) {
        List<InvoiceMetadata> mapped = raw.getInvoices() != null
                ? raw.getInvoices().stream().map(InvoiceMetadata::from).toList()
                : List.of();
        return new InvoiceMetadataResult(
                Boolean.TRUE.equals(raw.getHasMore()),
                Boolean.TRUE.equals(raw.getIsTruncated()),
                raw.getPermanentStorageHwmDate(),
                mapped);
    }
}
