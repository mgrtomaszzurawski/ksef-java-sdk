/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
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
 *
 * @since 1.0.0
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
        @Nullable SortOrder sortOrder) {

    public InvoiceQueryRequest {
        Objects.requireNonNull(subjectType, "subjectType");
        Objects.requireNonNull(dateType, "dateType");
        Objects.requireNonNull(dateFrom, "dateFrom");
        currencyCodes = currencyCodes == null ? null : List.copyOf(currencyCodes);
        invoiceTypes = invoiceTypes == null ? null : List.copyOf(invoiceTypes);
    }
}
