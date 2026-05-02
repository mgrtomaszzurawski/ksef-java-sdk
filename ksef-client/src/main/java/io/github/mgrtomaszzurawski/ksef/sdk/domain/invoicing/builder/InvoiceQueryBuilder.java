/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryDateRangeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryDateTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQuerySubjectTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicingModeRaw;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Builder for invoice metadata query filters.
 * <p>
 * Required fields: subjectType, dateType, dateFrom.
 * Server enforces: dateRange max 3 months.
 * <p>
 * Usage:
 * <pre>{@code
 * InvoiceQueryFiltersRaw filters = InvoiceQueryBuilder.seller()
 *     .invoicingDateFrom(OffsetDateTime.now().minusDays(30))
 *     .build();
 * }</pre>
 */
public final class InvoiceQueryBuilder {

    private static final int MAX_DATE_RANGE_MONTHS = 3;
    private static final String ERR_DATE_FROM_REQUIRED = "dateFrom is required";
    private static final String ERR_DATE_RANGE_EXCEEDED = "dateRange must not exceed 3 months";

    private final InvoiceQuerySubjectTypeRaw subjectType;
    private InvoiceQueryDateTypeRaw dateType = InvoiceQueryDateTypeRaw.INVOICING;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;
    private String ksefNumber;
    private String invoiceNumber;
    private String sellerNip;
    private InvoicingModeRaw invoicingMode;
    private Boolean selfInvoicing;
    private Boolean hasAttachment;

    private InvoiceQueryBuilder(InvoiceQuerySubjectTypeRaw subjectType) {
        this.subjectType = subjectType;
    }

    /**
     * Query invoices where the authenticated subject is the seller (Subject1).
     */
    public static InvoiceQueryBuilder seller() {
        return new InvoiceQueryBuilder(InvoiceQuerySubjectTypeRaw.SUBJECT1);
    }

    /**
     * Query invoices where the authenticated subject is the buyer (Subject2).
     */
    public static InvoiceQueryBuilder buyer() {
        return new InvoiceQueryBuilder(InvoiceQuerySubjectTypeRaw.SUBJECT2);
    }

    /**
     * Query invoices where the authenticated subject is a third party (Subject3).
     */
    public static InvoiceQueryBuilder thirdParty() {
        return new InvoiceQueryBuilder(InvoiceQuerySubjectTypeRaw.SUBJECT3);
    }

    /**
     * Filter by invoicing date from (required). Date is truncated to seconds
     * for KSeF compatibility.
     */
    public InvoiceQueryBuilder invoicingDateFrom(OffsetDateTime from) {
        this.dateType = InvoiceQueryDateTypeRaw.INVOICING;
        this.dateFrom = from;
        return this;
    }

    /**
     * Filter by permanent storage date from (required). Date is truncated to seconds
     * for KSeF compatibility.
     */
    public InvoiceQueryBuilder permanentStorageDateFrom(OffsetDateTime from) {
        this.dateType = InvoiceQueryDateTypeRaw.PERMANENT_STORAGE;
        this.dateFrom = from;
        return this;
    }

    /**
     * Filter by issue date from (required). Date is truncated to seconds
     * for KSeF compatibility.
     */
    public InvoiceQueryBuilder issueDateFrom(OffsetDateTime from) {
        this.dateType = InvoiceQueryDateTypeRaw.ISSUE;
        this.dateFrom = from;
        return this;
    }

    /**
     * Optional upper bound for the date range.
     */
    public InvoiceQueryBuilder dateTo(OffsetDateTime endDate) {
        this.dateTo = endDate;
        return this;
    }

    /**
     * Filter by KSeF invoice number.
     */
    public InvoiceQueryBuilder ksefNumber(String ksefNumber) {
        this.ksefNumber = ksefNumber;
        return this;
    }

    /**
     * Filter by invoice number from the XML content.
     */
    public InvoiceQueryBuilder invoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
        return this;
    }

    /**
     * Filter by seller NIP.
     */
    public InvoiceQueryBuilder sellerNip(String nip) {
        this.sellerNip = nip;
        return this;
    }

    /**
     * Filter by invoicing mode (online/offline).
     */
    public InvoiceQueryBuilder onlineOnly() {
        this.invoicingMode = InvoicingModeRaw.ONLINE;
        return this;
    }

    /**
     * Filter by invoicing mode (online/offline).
     */
    public InvoiceQueryBuilder offlineOnly() {
        this.invoicingMode = InvoicingModeRaw.OFFLINE;
        return this;
    }

    /**
     * Filter self-invoicing only.
     */
    public InvoiceQueryBuilder selfInvoicing(boolean selfInvoicing) {
        this.selfInvoicing = selfInvoicing;
        return this;
    }

    /**
     * Filter invoices with attachments.
     */
    public InvoiceQueryBuilder hasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
        return this;
    }

    /**
     * Build the query filters. Validates required fields and date range constraints.
     *
     * @return the filters ready to pass to {@code InvoiceClient.queryMetadata()}
     * @throws IllegalStateException if required fields are missing or constraints violated
     */
    public InvoiceQueryFiltersRaw build() {
        Objects.requireNonNull(dateFrom, ERR_DATE_FROM_REQUIRED);

        if (dateTo != null && dateFrom.plusMonths(MAX_DATE_RANGE_MONTHS).isBefore(dateTo)) {
            throw new IllegalStateException(ERR_DATE_RANGE_EXCEEDED);
        }

        OffsetDateTime truncatedFrom = dateFrom.withNano(0);
        InvoiceQueryDateRangeRaw dateRange = new InvoiceQueryDateRangeRaw()
                .dateType(dateType)
                .from(truncatedFrom);
        if (dateTo != null) {
            dateRange.to(dateTo.withNano(0));
        }

        InvoiceQueryFiltersRaw filters = new InvoiceQueryFiltersRaw()
                .subjectType(subjectType)
                .dateRange(dateRange);

        if (ksefNumber != null) {
            filters.ksefNumber(ksefNumber);
        }
        if (invoiceNumber != null) {
            filters.invoiceNumber(invoiceNumber);
        }
        if (sellerNip != null) {
            filters.sellerNip(sellerNip);
        }
        if (invoicingMode != null) {
            filters.invoicingMode(invoicingMode);
        }
        if (selfInvoicing != null) {
            filters.isSelfInvoicing(selfInvoicing);
        }
        if (hasAttachment != null) {
            filters.hasAttachment(hasAttachment);
        }

        return filters;
    }
}
