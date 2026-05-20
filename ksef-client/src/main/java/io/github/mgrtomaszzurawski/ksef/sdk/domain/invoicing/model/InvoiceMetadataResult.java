/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;



import java.time.OffsetDateTime;
import java.util.List;

/**
 * Single page of invoice metadata returned by
 * {@code InvoiceArchive.queryByMetadata(...)}.
 *
 * <p><strong>Paging flow.</strong> Two flags drive navigation:
 *
 * <ul>
 *   <li>{@link #hasMore} — {@code true} when at least one more page
 *       exists past the current {@code pageOffset}. Increment
 *       {@code pageOffset} and re-issue the query to fetch it. When
 *       {@code false}, this is the terminal page.</li>
 *   <li>{@link #isTruncated} — {@code true} when the filter set has
 *       hit KSeF's hard <strong>10 000-record cap</strong> per
 *       {@code (subjectType, dateRange, sortOrder)} tuple. Further
 *       {@code pageOffset} increments past this point will NOT return
 *       additional results. Workaround per spec
 *       ({@code POST /invoices/query/metadata} description):
 *       advance {@code dateRange.from} (or {@code dateRange.to},
 *       depending on sort direction) past the last record's date,
 *       reset {@code pageOffset = 0}, and re-issue. The
 *       {@code streamByMetadata} paginator handles this transition
 *       automatically.</li>
 * </ul>
 *
 * <p>{@link #permanentStorageHwmDate} is populated only when
 * {@code dateType = PermanentStorage} on the query. It is the
 * server-stable timestamp below which the result set is guaranteed
 * not to gain further records — used by the SDK's
 * {@code InvoiceSync} orchestrator as the high-water-mark for
 * incremental walks. For {@code Issue} / {@code Invoicing} date
 * filters this field is {@code null} per spec.
 *
 * @param hasMore {@code true} when more pages exist past the current
 *     {@code pageOffset}
 * @param isTruncated {@code true} when the filter set hit the
 *     server's 10 000-record cap (see flow above)
 * @param permanentStorageHwmDate stability watermark for
 *     {@code PermanentStorage}-date queries (non-null only when
 *     {@code dateType = PermanentStorage}); guarantees that no
 *     further records will appear in this result set below this
 *     timestamp
 * @param invoices the metadata records on this page (non-null,
 *     possibly empty)
 *
 * @since 0.1.0
 */
public record InvoiceMetadataResult(
        boolean hasMore,
        boolean isTruncated,
        OffsetDateTime permanentStorageHwmDate,
        List<InvoiceMetadata> invoices) {

}
