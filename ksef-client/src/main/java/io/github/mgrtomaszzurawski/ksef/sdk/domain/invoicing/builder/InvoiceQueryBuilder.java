/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Builder for invoice metadata query filters.
 * <p>Required fields: subjectType, dateType, dateFrom. Server enforces dateRange max 3 months.
 */
public final class InvoiceQueryBuilder {

    private static final int MAX_DATE_RANGE_MONTHS = 3;
    private static final String ERR_DATE_FROM_REQUIRED = "dateFrom is required";
    private static final String ERR_DATE_RANGE_EXCEEDED = "dateRange must not exceed 3 months";

    private final InvoiceQuerySubjectType subjectType;
    private InvoiceQueryDateType dateType = InvoiceQueryDateType.INVOICING;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;
    private String ksefNumber;
    private String invoiceNumber;
    private String sellerNip;
    private InvoicingMode invoicingMode;
    private Boolean selfInvoicing;
    private Boolean hasAttachment;

    private InvoiceQueryBuilder(InvoiceQuerySubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public static InvoiceQueryBuilder seller() {
        return new InvoiceQueryBuilder(InvoiceQuerySubjectType.SUBJECT1);
    }

    public static InvoiceQueryBuilder buyer() {
        return new InvoiceQueryBuilder(InvoiceQuerySubjectType.SUBJECT2);
    }

    public static InvoiceQueryBuilder thirdParty() {
        return new InvoiceQueryBuilder(InvoiceQuerySubjectType.SUBJECT3);
    }

    /**
     * Query invoices where the authenticated subject acts on behalf of an
     * authorized partner (KSeF {@code SUBJECT_AUTHORIZED} mode).
     */
    public static InvoiceQueryBuilder authorized() {
        return new InvoiceQueryBuilder(InvoiceQuerySubjectType.SUBJECT_AUTHORIZED);
    }

    public InvoiceQueryBuilder invoicingDateFrom(OffsetDateTime from) {
        this.dateType = InvoiceQueryDateType.INVOICING;
        this.dateFrom = from;
        return this;
    }

    public InvoiceQueryBuilder permanentStorageDateFrom(OffsetDateTime from) {
        this.dateType = InvoiceQueryDateType.PERMANENT_STORAGE;
        this.dateFrom = from;
        return this;
    }

    public InvoiceQueryBuilder issueDateFrom(OffsetDateTime from) {
        this.dateType = InvoiceQueryDateType.ISSUE;
        this.dateFrom = from;
        return this;
    }

    public InvoiceQueryBuilder dateTo(OffsetDateTime endDate) {
        this.dateTo = endDate;
        return this;
    }

    public InvoiceQueryBuilder ksefNumber(String ksefNumber) {
        this.ksefNumber = ksefNumber;
        return this;
    }

    public InvoiceQueryBuilder invoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
        return this;
    }

    public InvoiceQueryBuilder sellerNip(String nip) {
        this.sellerNip = nip;
        return this;
    }

    public InvoiceQueryBuilder onlineOnly() {
        this.invoicingMode = InvoicingMode.ONLINE;
        return this;
    }

    public InvoiceQueryBuilder offlineOnly() {
        this.invoicingMode = InvoicingMode.OFFLINE;
        return this;
    }

    public InvoiceQueryBuilder selfInvoicing(boolean selfInvoicing) {
        this.selfInvoicing = selfInvoicing;
        return this;
    }

    public InvoiceQueryBuilder hasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
        return this;
    }

    public InvoiceQueryBuilder toBuilder() {
        InvoiceQueryBuilder copy = new InvoiceQueryBuilder(this.subjectType);
        copy.dateType = this.dateType;
        copy.dateFrom = this.dateFrom;
        copy.dateTo = this.dateTo;
        copy.ksefNumber = this.ksefNumber;
        copy.invoiceNumber = this.invoiceNumber;
        copy.sellerNip = this.sellerNip;
        copy.invoicingMode = this.invoicingMode;
        copy.selfInvoicing = this.selfInvoicing;
        copy.hasAttachment = this.hasAttachment;
        return copy;
    }

    public InvoiceQueryFilters build() {
        Objects.requireNonNull(dateFrom, ERR_DATE_FROM_REQUIRED);
        if (dateTo != null && dateFrom.plusMonths(MAX_DATE_RANGE_MONTHS).isBefore(dateTo)) {
            throw new IllegalStateException(ERR_DATE_RANGE_EXCEEDED);
        }
        OffsetDateTime truncatedFrom = dateFrom.withNano(0);
        OffsetDateTime truncatedTo = dateTo == null ? null : dateTo.withNano(0);
        return new InvoiceQueryFilters(
                subjectType, dateType, truncatedFrom, truncatedTo,
                ksefNumber, invoiceNumber, sellerNip,
                invoicingMode, selfInvoicing, hasAttachment);
    }
}
