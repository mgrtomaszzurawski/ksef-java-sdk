/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryDateRangeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryDateTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFiltersRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQuerySubjectTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicingModeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceRequest;

/**
 * SDK-record → generated {@code *Raw} mappers for invoicing requests.
 * Lives in a non-exported package; consumers can't reach it.
 */
public final class InvoicingRequestMappers {

    private InvoicingRequestMappers() { }

    public static InvoiceExportRequestRaw toInvoiceExportRequestRaw(InvoiceExportRequest request) {
        return new InvoiceExportRequestRaw()
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(request.encryptedSymmetricKey())
                        .initializationVector(request.initVector()))
                .onlyMetadata(request.onlyMetadata())
                .filters(toInvoiceQueryFiltersRaw(request.filters()));
    }

    public static InvoiceQueryFiltersRaw toInvoiceQueryFiltersRaw(InvoiceQueryFilters filters) {
        InvoiceQueryFiltersRaw raw = new InvoiceQueryFiltersRaw()
                .subjectType(toInvoiceQuerySubjectTypeRaw(filters.subjectType()))
                .dateRange(buildDateRange(filters));
        applyIdentifierFilters(raw, filters);
        applyMetadataFilters(raw, filters);
        return raw;
    }

    public static SendInvoiceRequestRaw toSendInvoiceRequestRaw(SendInvoiceRequest request) {
        SendInvoiceRequestRaw raw = new SendInvoiceRequestRaw();
        raw.setInvoiceHash(request.invoiceHash());
        raw.setInvoiceSize(request.invoiceSize());
        raw.setEncryptedInvoiceHash(request.encryptedInvoiceHash());
        raw.setEncryptedInvoiceSize(request.encryptedInvoiceSize());
        raw.setEncryptedInvoiceContent(request.encryptedInvoiceContent());
        raw.setOfflineMode(request.offlineMode());
        return raw;
    }

    public static InvoiceQuerySubjectTypeRaw toInvoiceQuerySubjectTypeRaw(InvoiceQuerySubjectType value) {
        return switch (value) {
            case SUBJECT1 -> InvoiceQuerySubjectTypeRaw.SUBJECT1;
            case SUBJECT2 -> InvoiceQuerySubjectTypeRaw.SUBJECT2;
            case SUBJECT3 -> InvoiceQuerySubjectTypeRaw.SUBJECT3;
            case SUBJECT_AUTHORIZED -> InvoiceQuerySubjectTypeRaw.SUBJECT_AUTHORIZED;
        };
    }

    public static InvoiceQueryDateTypeRaw toInvoiceQueryDateTypeRaw(InvoiceQueryDateType value) {
        return switch (value) {
            case INVOICING -> InvoiceQueryDateTypeRaw.INVOICING;
            case PERMANENT_STORAGE -> InvoiceQueryDateTypeRaw.PERMANENT_STORAGE;
            case ISSUE -> InvoiceQueryDateTypeRaw.ISSUE;
        };
    }

    public static InvoicingModeRaw toInvoicingModeRaw(InvoicingMode value) {
        return switch (value) {
            case ONLINE -> InvoicingModeRaw.ONLINE;
            case OFFLINE -> InvoicingModeRaw.OFFLINE;
        };
    }

    private static InvoiceQueryDateRangeRaw buildDateRange(InvoiceQueryFilters filters) {
        InvoiceQueryDateRangeRaw dateRange = new InvoiceQueryDateRangeRaw()
                .dateType(toInvoiceQueryDateTypeRaw(filters.dateType()))
                .from(filters.dateFrom());
        if (filters.dateTo() != null) {
            dateRange.to(filters.dateTo());
        }
        return dateRange;
    }

    private static void applyIdentifierFilters(InvoiceQueryFiltersRaw raw, InvoiceQueryFilters filters) {
        if (filters.ksefNumber() != null) {
            raw.ksefNumber(filters.ksefNumber());
        }
        if (filters.invoiceNumber() != null) {
            raw.invoiceNumber(filters.invoiceNumber());
        }
        if (filters.sellerNip() != null) {
            raw.sellerNip(filters.sellerNip());
        }
    }

    private static void applyMetadataFilters(InvoiceQueryFiltersRaw raw, InvoiceQueryFilters filters) {
        if (filters.invoicingMode() != null) {
            raw.invoicingMode(toInvoicingModeRaw(filters.invoicingMode()));
        }
        if (filters.selfInvoicing() != null) {
            raw.isSelfInvoicing(filters.selfInvoicing());
        }
        if (filters.hasAttachment() != null) {
            raw.hasAttachment(filters.hasAttachment());
        }
    }
}
