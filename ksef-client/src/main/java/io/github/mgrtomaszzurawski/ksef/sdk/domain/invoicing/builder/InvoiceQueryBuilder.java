/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BuyerIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceFormType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryAmount;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryAmountType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryBuyerIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SortOrder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for invoice metadata query filters.
 * <p>Required fields: subjectType, dateType, dateFrom. Server enforces dateRange max 3 months.
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.TooManyFields")  // 16 fields = the spec's full filter set; splitting would just hide the spec parity.
public final class InvoiceQueryBuilder {

    private static final int MAX_DATE_RANGE_MONTHS = 3;
    private static final String ERR_DATE_FROM_REQUIRED = "dateFrom is required";
    private static final String ERR_DATE_RANGE_EXCEEDED = "dateRange must not exceed 3 months";

    private final InvoiceQuerySubjectType subjectType;
    private InvoiceQueryDateType dateType = InvoiceQueryDateType.INVOICING;
    private @Nullable OffsetDateTime dateFrom;
    private @Nullable OffsetDateTime dateTo;
    private io.github.mgrtomaszzurawski.ksef.sdk.common.@Nullable KsefNumber ksefNumber;
    private @Nullable String invoiceNumber;
    private @Nullable String sellerNip;
    private @Nullable InvoicingMode invoicingMode;
    private @Nullable Boolean selfInvoicing;
    private @Nullable Boolean hasAttachment;
    private boolean restrictToPermanentStorageHwm;
    private @Nullable InvoiceQueryAmount amount;
    private @Nullable InvoiceQueryBuyerIdentifier buyerIdentifier;
    private @Nullable List<String> currencyCodes;
    private @Nullable InvoiceFormType formType;
    private @Nullable List<InvoiceType> invoiceTypes;
    private @Nullable SortOrder sortOrder;
    private @Nullable Integer pageOffset;
    private @Nullable Integer pageSize;

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

    public InvoiceQueryBuilder ksefNumber(io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber ksefNumber) {
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

    /**
     * @apiNote Internal — set automatically by the SDK's sync orchestrator on
     *     every export it opens. Caps the export's {@code dateRange.to} at
     *     the server-side PermanentStorage HWM, per the incremental-retrieval
     *     spec ({@code pobieranie-faktur/przyrostowe-pobieranie-faktur.md},
     *     "Kluczowe znaczenie daty PermanentStorage"). Consumers do not
     *     normally need to call this; one-shot {@code export().prepare(...)}
     *     calls intentionally omit the flag so the server returns the full
     *     requested range.
     */
    public InvoiceQueryBuilder restrictToPermanentStorageHwm() {
        this.restrictToPermanentStorageHwm = true;
        return this;
    }

    /**
     * Filter by an inclusive amount range on the chosen monetary axis
     * (gross / net / VAT). Mirrors OpenAPI {@code amount}.
     */
    public InvoiceQueryBuilder amount(InvoiceQueryAmountType type, BigDecimal from, BigDecimal to) {
        this.amount = new InvoiceQueryAmount(type, from, to);
        return this;
    }

    /**
     * Filter by buyer identifier (kind + value). For
     * {@link BuyerIdentifierType#NONE} the value is ignored. Mirrors
     * OpenAPI {@code buyerIdentifier}.
     */
    public InvoiceQueryBuilder buyerIdentifier(BuyerIdentifierType type, String value) {
        this.buyerIdentifier = new InvoiceQueryBuyerIdentifier(type, value);
        return this;
    }

    /**
     * Filter by ISO-4217 currency codes (e.g. {@code "PLN"}, {@code "EUR"}).
     * Mirrors OpenAPI {@code currencyCodes}.
     */
    public InvoiceQueryBuilder currencyCodes(String... codes) {
        this.currencyCodes = List.of(codes);
        return this;
    }

    /**
     * Filter by form type (FA / PEF / RR / FA_RR). Mirrors OpenAPI
     * {@code formType}.
     */
    public InvoiceQueryBuilder formType(InvoiceFormType type) {
        this.formType = type;
        return this;
    }

    /**
     * Filter by invoice types (Vat, Kor, KorPef, …). Mirrors OpenAPI
     * {@code invoiceTypes}.
     */
    public InvoiceQueryBuilder invoiceTypes(InvoiceType... types) {
        this.invoiceTypes = List.of(types);
        return this;
    }

    /**
     * Set the result sort order for the {@code POST /invoices/query/metadata}
     * call. Maps to the spec's {@code sortOrder} query parameter; default
     * (when not set) is {@link SortOrder#ASC} per spec.
     */
    public InvoiceQueryBuilder sortOrder(SortOrder sortOrder) {
        this.sortOrder = Objects.requireNonNull(sortOrder, "sortOrder");
        return this;
    }

    /**
     * Set the page offset (0-based) for explicit page navigation via
     * {@code InvoiceArchive.queryByMetadata}. Default (when not set):
     * server default 0 (first page). Ignored by
     * {@code InvoiceArchive.streamByMetadata}, which always starts at
     * page 0.
     *
     * @param pageOffset zero-based page index (must be {@code >= 0})
     */
    public InvoiceQueryBuilder pageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
        return this;
    }

    /**
     * Set the page size for both {@code queryByMetadata} and
     * {@code streamByMetadata}. Server bounds (per OpenAPI):
     * {@code 10 <= pageSize <= 250}. Default (when not set): server
     * default 10.
     */
    public InvoiceQueryBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
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
        copy.restrictToPermanentStorageHwm = this.restrictToPermanentStorageHwm;
        copy.amount = this.amount;
        copy.buyerIdentifier = this.buyerIdentifier;
        copy.currencyCodes = this.currencyCodes;
        copy.formType = this.formType;
        copy.invoiceTypes = this.invoiceTypes;
        copy.sortOrder = this.sortOrder;
        copy.pageOffset = this.pageOffset;
        copy.pageSize = this.pageSize;
        return copy;
    }

    public InvoiceQueryRequest build() {
        Objects.requireNonNull(dateFrom, ERR_DATE_FROM_REQUIRED);
        if (dateTo != null && dateFrom.plusMonths(MAX_DATE_RANGE_MONTHS).isBefore(dateTo)) {
            throw new IllegalStateException(ERR_DATE_RANGE_EXCEEDED);
        }
        OffsetDateTime truncatedFrom = dateFrom.withNano(0);
        OffsetDateTime truncatedTo = dateTo == null ? null : dateTo.withNano(0);
        return new InvoiceQueryRequest(
                subjectType, dateType, truncatedFrom, truncatedTo,
                ksefNumber, invoiceNumber, sellerNip,
                invoicingMode, selfInvoicing, hasAttachment,
                restrictToPermanentStorageHwm,
                amount, buyerIdentifier, currencyCodes, formType, invoiceTypes,
                sortOrder, pageOffset, pageSize);
    }
}
