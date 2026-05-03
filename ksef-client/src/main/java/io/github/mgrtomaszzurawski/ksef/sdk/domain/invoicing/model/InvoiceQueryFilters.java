/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for invoice metadata queries (returned by
 * {@code InvoiceQueryBuilder.build()}).
 */
public record InvoiceQueryFilters(
        InvoiceQuerySubjectType subjectType,
        InvoiceQueryDateType dateType,
        OffsetDateTime dateFrom,
        @Nullable OffsetDateTime dateTo,
        @Nullable String ksefNumber,
        @Nullable String invoiceNumber,
        @Nullable String sellerNip,
        @Nullable InvoicingMode invoicingMode,
        @Nullable Boolean selfInvoicing,
        @Nullable Boolean hasAttachment) {

    public InvoiceQueryFilters {
        Objects.requireNonNull(subjectType, "subjectType");
        Objects.requireNonNull(dateType, "dateType");
        Objects.requireNonNull(dateFrom, "dateFrom");
    }
}
