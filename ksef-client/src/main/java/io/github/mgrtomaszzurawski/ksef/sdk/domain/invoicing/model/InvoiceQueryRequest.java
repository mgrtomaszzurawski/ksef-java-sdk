/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for invoice metadata queries (returned by
 * {@code InvoiceQueryBuilder.build()}).
 *
 * @param restrictToPermanentStorageHwm when {@code true}, the server caps
 *     the export's {@code dateRange.to} at the current PermanentStorage HWM;
 *     mandated by the incremental-retrieval spec
 *     ({@code pobieranie-faktur/przyrostowe-pobieranie-faktur.md}). Set by
 *     the SDK's sync orchestrator on every export it opens; consumers do not
 *     normally need to set this directly. Default {@code false}.
 * @param sortOrder optional sort order for the
 *     {@code POST /invoices/query/metadata} call. Maps to the spec's
 *     {@code sortOrder} query parameter; default (when {@code null}) is
 *     {@link SortOrder#ASC} per spec.
 * @param pageOffset optional page offset (0-based) for explicit page
 *     navigation via {@code InvoiceArchive.queryByMetadata}. {@code null}
 *     defers to the server default (0 = first page).
 *     {@code InvoiceArchive.streamByMetadata} <strong>ignores</strong>
 *     this field — the paginator always starts at page 0.
 * @param pageSize optional page size for both
 *     {@code queryByMetadata} and {@code streamByMetadata}. Server bounds
 *     (per OpenAPI): min 10, max 250; {@code null} defers to the SDK
 *     default (250 — the largest page the server accepts, chosen so
 *     stream consumers cover ground per round-trip).
 *
 * @since 0.1.0
 */
public record InvoiceQueryRequest(
        InvoiceQuerySubjectType subjectType,
        InvoiceQueryDateType dateType,
        OffsetDateTime dateFrom,
        @Nullable OffsetDateTime dateTo,
        @Nullable KsefNumber ksefNumber,
        @Nullable String invoiceNumber,
        @Nullable String sellerNip,
        @Nullable InvoicingMode invoicingMode,
        @Nullable Boolean selfInvoicing,
        @Nullable Boolean hasAttachment,
        boolean restrictToPermanentStorageHwm,
        @Nullable InvoiceQueryAmount amount,
        @Nullable InvoiceQueryBuyerIdentifier buyerIdentifier,
        @Nullable List<String> currencyCodes,
        @Nullable InvoiceFormType formType,
        @Nullable List<InvoiceType> invoiceTypes,
        @Nullable SortOrder sortOrder,
        @Nullable Integer pageOffset,
        @Nullable Integer pageSize) {

    /** Server-side page-size bounds for {@code POST /invoices/query/metadata}. */
    private static final int MIN_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 250;
    private static final String ERR_NEGATIVE_OFFSET = "pageOffset must not be negative (got %d)";
    private static final String ERR_PAGE_SIZE_BOUNDS =
            "pageSize must be in [" + MIN_PAGE_SIZE + ", " + MAX_PAGE_SIZE + "] (got %d)";

    public InvoiceQueryRequest {
        Objects.requireNonNull(subjectType, "subjectType");
        Objects.requireNonNull(dateType, "dateType");
        Objects.requireNonNull(dateFrom, "dateFrom");
        currencyCodes = currencyCodes == null ? null : List.copyOf(currencyCodes);
        invoiceTypes = invoiceTypes == null ? null : List.copyOf(invoiceTypes);
        if (pageOffset != null && pageOffset < 0) {
            throw new IllegalArgumentException(String.format(ERR_NEGATIVE_OFFSET, pageOffset));
        }
        if (pageSize != null && (pageSize < MIN_PAGE_SIZE || pageSize > MAX_PAGE_SIZE)) {
            throw new IllegalArgumentException(String.format(ERR_PAGE_SIZE_BOUNDS, pageSize));
        }
    }
}
