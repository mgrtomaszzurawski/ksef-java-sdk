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
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.InvoiceExportRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.SendInvoiceRequest;
import java.util.List;

/**
 * SDK-record → generated {@code *Raw} mappers for invoicing requests.
 * Lives in a non-exported package; consumers can't reach it.
 *
 * @since 1.0.0
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

    public static InvoiceQueryFiltersRaw toInvoiceQueryFiltersRaw(InvoiceQueryRequest filters) {
        InvoiceQueryFiltersRaw rawValue = new InvoiceQueryFiltersRaw()
                .subjectType(toInvoiceQuerySubjectTypeRaw(filters.subjectType()))
                .dateRange(buildDateRange(filters));
        applyIdentifierFilters(rawValue, filters);
        applyMetadataFilters(rawValue, filters);
        return rawValue;
    }

    public static SendInvoiceRequestRaw toSendInvoiceRequestRaw(SendInvoiceRequest request) {
        SendInvoiceRequestRaw rawValue = new SendInvoiceRequestRaw();
        rawValue.setInvoiceHash(request.invoiceHash());
        rawValue.setInvoiceSize(request.invoiceSize());
        rawValue.setEncryptedInvoiceHash(request.encryptedInvoiceHash());
        rawValue.setEncryptedInvoiceSize(request.encryptedInvoiceSize());
        rawValue.setEncryptedInvoiceContent(request.encryptedInvoiceContent());
        rawValue.setOfflineMode(request.offlineMode());
        if (request.hashOfCorrectedInvoice() != null) {
            rawValue.setHashOfCorrectedInvoice(request.hashOfCorrectedInvoice());
        }
        return rawValue;
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

    private static InvoiceQueryDateRangeRaw buildDateRange(InvoiceQueryRequest filters) {
        InvoiceQueryDateRangeRaw dateRange = new InvoiceQueryDateRangeRaw()
                .dateType(toInvoiceQueryDateTypeRaw(filters.dateType()))
                .from(filters.dateFrom());
        if (filters.dateTo() != null) {
            dateRange.to(filters.dateTo());
        }
        if (filters.restrictToPermanentStorageHwm()) {
            /*
             * Codex round-9 manual-validation A.1.1 — incremental-sync workflow
             * mandates this flag (przyrostowe-pobieranie-faktur.md). The SDK
             * toggles it only when the sync orchestrator builds the export query;
             * ad-hoc one-shot prepareExport calls leave it false so the server
             * returns the full requested range.
             */
            dateRange.restrictToPermanentStorageHwmDate(Boolean.TRUE);
        }
        return dateRange;
    }

    private static void applyIdentifierFilters(InvoiceQueryFiltersRaw rawValue, InvoiceQueryRequest filters) {
        if (filters.ksefNumber() != null) {
            rawValue.ksefNumber(filters.ksefNumber());
        }
        if (filters.invoiceNumber() != null) {
            rawValue.invoiceNumber(filters.invoiceNumber());
        }
        if (filters.sellerNip() != null) {
            rawValue.sellerNip(filters.sellerNip());
        }
    }

    private static void applyMetadataFilters(InvoiceQueryFiltersRaw rawValue, InvoiceQueryRequest filters) {
        if (filters.invoicingMode() != null) {
            rawValue.invoicingMode(toInvoicingModeRaw(filters.invoicingMode()));
        }
        if (filters.selfInvoicing() != null) {
            rawValue.isSelfInvoicing(filters.selfInvoicing());
        }
        if (filters.hasAttachment() != null) {
            rawValue.hasAttachment(filters.hasAttachment());
        }
        // Codex round-9 manual-validation A.3 — 5 spec filter fields previously
        // missing from the typed builder/filters. Each is null-skipped so
        // pre-existing callers continue to send the same wire bytes.
        if (filters.amount() != null) {
            rawValue.amount(toAmountRaw(filters.amount()));
        }
        if (filters.buyerIdentifier() != null) {
            rawValue.buyerIdentifier(toBuyerIdentifierRaw(filters.buyerIdentifier()));
        }
        if (filters.currencyCodes() != null) {
            rawValue.currencyCodes(toCurrencyCodesRaw(filters.currencyCodes()));
        }
        if (filters.formType() != null) {
            rawValue.formType(toInvoiceQueryFormTypeRaw(filters.formType()));
        }
        if (filters.invoiceTypes() != null) {
            rawValue.invoiceTypes(toInvoiceTypesRaw(filters.invoiceTypes()));
        }
    }

    private static io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryAmountRaw toAmountRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryAmount amount) {
        io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryAmountRaw raw =
                new io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryAmountRaw();
        io.github.mgrtomaszzurawski.ksef.client.model.AmountTypeRaw amountType = switch (amount.type()) {
            case BRUTTO -> io.github.mgrtomaszzurawski.ksef.client.model.AmountTypeRaw.BRUTTO;
            case NETTO -> io.github.mgrtomaszzurawski.ksef.client.model.AmountTypeRaw.NETTO;
            case VAT -> io.github.mgrtomaszzurawski.ksef.client.model.AmountTypeRaw.VAT;
        };
        raw.setType(amountType);
        raw.setFrom(amount.from().doubleValue());
        raw.setTo(amount.to().doubleValue());
        return raw;
    }

    private static io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryBuyerIdentifierRaw toBuyerIdentifierRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryBuyerIdentifier buyer) {
        io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryBuyerIdentifierRaw raw =
                new io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryBuyerIdentifierRaw();
        io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw buyerType = switch (buyer.type()) {
            case NIP -> io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw.NIP;
            case VAT_UE -> io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw.VAT_UE;
            case OTHER -> io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw.OTHER;
            case NONE -> io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw.NONE;
        };
        raw.setType(buyerType);
        if (buyer.value() != null) {
            raw.setValue(buyer.value());
        }
        return raw;
    }

    private static List<io.github.mgrtomaszzurawski.ksef.client.model.CurrencyCodeRaw> toCurrencyCodesRaw(
            List<String> codes) {
        return codes.stream()
                .map(io.github.mgrtomaszzurawski.ksef.client.model.CurrencyCodeRaw::fromValue)
                .toList();
    }

    private static io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFormTypeRaw toInvoiceQueryFormTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceFormType formType) {
        return switch (formType) {
            case FA -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFormTypeRaw.FA;
            case PEF -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFormTypeRaw.PEF;
            case RR -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFormTypeRaw.RR;
            case FA_RR -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQueryFormTypeRaw.FA_RR;
        };
    }

    private static List<io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw> toInvoiceTypesRaw(
            List<io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceType> types) {
        return types.stream().map(InvoicingRequestMappers::toInvoiceTypeRaw).toList();
    }

    private static io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw toInvoiceTypeRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceType type) {
        return switch (type) {
            case VAT -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.VAT;
            case ZAL -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.ZAL;
            case KOR -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.KOR;
            case ROZ -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.ROZ;
            case UPR -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.UPR;
            case KOR_ZAL -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.KOR_ZAL;
            case KOR_ROZ -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.KOR_ROZ;
            case VAT_PEF -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.VAT_PEF;
            case VAT_PEF_SP -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.VAT_PEF_SP;
            case KOR_PEF -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.KOR_PEF;
            case VAT_RR -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.VAT_RR;
            case KOR_VAT_RR -> io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw.KOR_VAT_RR;
        };
    }
}
