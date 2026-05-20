/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.BatchSessionEffectiveContextLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceExportStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataBuyerRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataSellerRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataThirdSubjectRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicePackagePartRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicePackageRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceStatusInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicingModeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OnlineSessionEffectiveContextLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenBatchSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PartUploadRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryInvoicesMetadataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoiceStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoicesResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.ThirdSubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.UpoPageResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.UpoResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.BatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.BatchSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BuyerIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.ExportInvoicesResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FormCodeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceBuyer;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadataResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackagePart;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceSeller;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceStatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceThirdSubject;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.OnlineSessionOpenResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.OnlineSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ThirdSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoPage;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Internal mappers from generated {@code *Raw} types to public invoicing
 * domain records. Lives in a non-exported package; consumers can't reach it.
 *
 * @since 1.0.0
 */
public final class InvoicingMappers {

    private InvoicingMappers() { }

    public static BatchSession toBatchSession(OpenBatchSessionResponseRaw rawValue) {
        List<PartUploadRequest> parts = rawValue.getPartUploadRequests().stream().map(InvoicingMappers::toPartUploadRequest).toList();
        return new BatchSession(rawValue.getReferenceNumber(), parts);
    }

    public static @Nullable BatchSessionLimits toBatchSessionLimits(@Nullable BatchSessionEffectiveContextLimitsRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new BatchSessionLimits(
                rawValue.getMaxInvoiceSizeInMB(),
                rawValue.getMaxInvoiceWithAttachmentSizeInMB(),
                rawValue.getMaxInvoices());
    }

    public static @Nullable BuyerIdentifierType toBuyerIdentifierType(@Nullable BuyerIdentifierTypeRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (rawValue) {
            case NIP -> BuyerIdentifierType.NIP;
            case VAT_UE -> BuyerIdentifierType.VAT_UE;
            case OTHER -> BuyerIdentifierType.OTHER;
            case NONE -> BuyerIdentifierType.NONE;
        };
    }

    public static ExportInvoicesResult toExportInvoicesResult(ExportInvoicesResponseRaw rawValue) {
        return new ExportInvoicesResult(rawValue.getReferenceNumber());
    }

    public static @Nullable FormCodeInfo toFormCodeInfo(@Nullable FormCodeRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new FormCodeInfo(rawValue.getSystemCode(), rawValue.getSchemaVersion(), rawValue.getValue());
    }

    public static @Nullable InvoiceBuyer toInvoiceBuyer(@Nullable InvoiceMetadataBuyerRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        BuyerIdentifierType idType = null;
        String idValue = null;
        if (rawValue.getIdentifier() != null) {
            idType = toBuyerIdentifierType(rawValue.getIdentifier().getType());
            idValue = rawValue.getIdentifier().getValue();
        }
        return new InvoiceBuyer(idType, idValue, rawValue.getName());
    }

    public static InvoiceExportStatus toInvoiceExportStatus(InvoiceExportStatusResponseRaw rawValue) {
        return new InvoiceExportStatus(
                CommonMappers.toStatusInfo(rawValue.getStatus()),
                rawValue.getCompletedDate(),
                rawValue.getPackageExpirationDate(),
                toInvoicePackage(rawValue.getPackage()));
    }

    public static InvoiceMetadata toInvoiceMetadata(InvoiceMetadataRaw rawValue) {
        List<InvoiceThirdSubject> subjects = rawValue.getThirdSubjects() != null
                ? rawValue.getThirdSubjects().stream().map(InvoicingMappers::toInvoiceThirdSubject).toList()
                : List.of();
        return new InvoiceMetadata(
                io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber.parse(rawValue.getKsefNumber()),
                rawValue.getInvoiceNumber(),
                rawValue.getIssueDate(),
                rawValue.getInvoicingDate(),
                rawValue.getAcquisitionDate(),
                rawValue.getPermanentStorageDate(),
                toInvoiceSeller(rawValue.getSeller()),
                toInvoiceBuyer(rawValue.getBuyer()),
                rawValue.getNetAmount(),
                rawValue.getGrossAmount(),
                rawValue.getVatAmount(),
                rawValue.getCurrency(),
                toInvoicingMode(rawValue.getInvoicingMode()),
                toInvoiceType(rawValue.getInvoiceType()),
                toFormCodeInfo(rawValue.getFormCode()),
                rawValue.getIsSelfInvoicing(),
                rawValue.getHasAttachment(),
                rawValue.getInvoiceHash(),
                rawValue.getHashOfCorrectedInvoice(),
                subjects);
    }

    public static InvoiceMetadataResult toInvoiceMetadataResult(QueryInvoicesMetadataResponseRaw rawValue) {
        List<InvoiceMetadata> mapped = rawValue.getInvoices().stream().map(InvoicingMappers::toInvoiceMetadata).toList();
        return new InvoiceMetadataResult(
                rawValue.getHasMore(),
                rawValue.getIsTruncated(),
                rawValue.getPermanentStorageHwmDate(),
                mapped);
    }

    public static @Nullable InvoicePackage toInvoicePackage(@Nullable InvoicePackageRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        List<InvoicePackagePart> mappedParts = rawValue.getParts().stream().map(InvoicingMappers::toInvoicePackagePart).toList();
        return new InvoicePackage(
                rawValue.getInvoiceCount(),
                rawValue.getSize(),
                mappedParts,
                rawValue.getIsTruncated(),
                rawValue.getLastIssueDate(),
                rawValue.getLastInvoicingDate(),
                rawValue.getLastPermanentStorageDate(),
                rawValue.getPermanentStorageHwmDate());
    }

    public static InvoicePackagePart toInvoicePackagePart(InvoicePackagePartRaw rawValue) {
        return new InvoicePackagePart(
                rawValue.getOrdinalNumber(),
                rawValue.getPartName(),
                rawValue.getMethod(),
                rawValue.getUrl(),
                rawValue.getPartSize(),
                rawValue.getPartHash(),
                rawValue.getEncryptedPartSize(),
                rawValue.getEncryptedPartHash(),
                rawValue.getExpirationDate());
    }

    public static @Nullable InvoiceSeller toInvoiceSeller(@Nullable InvoiceMetadataSellerRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new InvoiceSeller(rawValue.getNip(), rawValue.getName());
    }

    public static @Nullable InvoiceStatusInfo toInvoiceStatusInfo(@Nullable InvoiceStatusInfoRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new InvoiceStatusInfo(
                rawValue.getCode(),
                rawValue.getDescription(),
                rawValue.getDetails() != null ? List.copyOf(rawValue.getDetails()) : List.of(),
                rawValue.getExtensions() != null ? Map.copyOf(rawValue.getExtensions()) : Map.of());
    }

    public static InvoiceThirdSubject toInvoiceThirdSubject(InvoiceMetadataThirdSubjectRaw rawValue) {
        ThirdSubjectIdentifierType idType = null;
        String idValue = null;
        if (rawValue.getIdentifier() != null) {
            idType = toThirdSubjectIdentifierType(rawValue.getIdentifier().getType());
            idValue = rawValue.getIdentifier().getValue();
        }
        return new InvoiceThirdSubject(idType, idValue, rawValue.getName(), rawValue.getRole());
    }

    public static @Nullable InvoiceType toInvoiceType(@Nullable InvoiceTypeRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (rawValue) {
            case VAT -> InvoiceType.VAT;
            case ZAL -> InvoiceType.ZAL;
            case KOR -> InvoiceType.KOR;
            case ROZ -> InvoiceType.ROZ;
            case UPR -> InvoiceType.UPR;
            case KOR_ZAL -> InvoiceType.KOR_ZAL;
            case KOR_ROZ -> InvoiceType.KOR_ROZ;
            case VAT_PEF -> InvoiceType.VAT_PEF;
            case VAT_PEF_SP -> InvoiceType.VAT_PEF_SP;
            case KOR_PEF -> InvoiceType.KOR_PEF;
            case VAT_RR -> InvoiceType.VAT_RR;
            case KOR_VAT_RR -> InvoiceType.KOR_VAT_RR;
        };
    }

    public static @Nullable InvoicingMode toInvoicingMode(@Nullable InvoicingModeRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (rawValue) {
            case ONLINE -> InvoicingMode.ONLINE;
            case OFFLINE -> InvoicingMode.OFFLINE;
        };
    }

    public static OnlineSessionOpenResult toOnlineSession(OpenOnlineSessionResponseRaw rawValue) {
        return new OnlineSessionOpenResult(rawValue.getReferenceNumber(), rawValue.getValidUntil());
    }

    public static @Nullable OnlineSessionLimits toOnlineSessionLimits(@Nullable OnlineSessionEffectiveContextLimitsRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new OnlineSessionLimits(
                rawValue.getMaxInvoiceSizeInMB(),
                rawValue.getMaxInvoiceWithAttachmentSizeInMB(),
                rawValue.getMaxInvoices());
    }

    public static PartUploadRequest toPartUploadRequest(PartUploadRequestRaw rawValue) {
        return new PartUploadRequest(
                rawValue.getOrdinalNumber(),
                rawValue.getMethod(),
                rawValue.getUrl(),
                Map.copyOf(rawValue.getHeaders()));
    }

    public static SendInvoiceResult toSendInvoiceResult(SendInvoiceResponseRaw rawValue) {
        return new SendInvoiceResult(rawValue.getReferenceNumber());
    }

    public static SessionInvoices toSessionInvoices(SessionInvoicesResponseRaw rawValue) {
        List<SessionInvoiceStatus> mapped = rawValue.getInvoices().stream().map(InvoicingMappers::toSessionInvoiceStatus).toList();
        return new SessionInvoices(rawValue.getContinuationToken(), mapped);
    }

    public static SessionInvoiceStatus toSessionInvoiceStatus(SessionInvoiceStatusResponseRaw rawValue) {
        return new SessionInvoiceStatus(
                rawValue.getOrdinalNumber() != null ? rawValue.getOrdinalNumber() : 0,
                rawValue.getInvoiceNumber(),
                rawValue.getKsefNumber() == null
                        ? null : io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber.parse(rawValue.getKsefNumber()),
                rawValue.getReferenceNumber(),
                rawValue.getInvoiceHash(),
                rawValue.getInvoiceFileName(),
                rawValue.getAcquisitionDate(),
                rawValue.getInvoicingDate(),
                rawValue.getPermanentStorageDate(),
                rawValue.getUpoDownloadUrl(),
                rawValue.getUpoDownloadUrlExpirationDate(),
                toInvoicingMode(rawValue.getInvoicingMode()),
                toInvoiceStatusInfo(rawValue.getStatus()));
    }

    public static SessionStatus toSessionStatus(SessionStatusResponseRaw rawValue) {
        return new SessionStatus(
                CommonMappers.toStatusInfo(rawValue.getStatus()),
                rawValue.getDateCreated(),
                rawValue.getDateUpdated(),
                rawValue.getValidUntil(),
                toUpoInfo(rawValue.getUpo()),
                rawValue.getInvoiceCount(),
                rawValue.getSuccessfulInvoiceCount(),
                rawValue.getFailedInvoiceCount());
    }

    public static @Nullable ThirdSubjectIdentifierType toThirdSubjectIdentifierType(@Nullable ThirdSubjectIdentifierTypeRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (rawValue) {
            case NIP -> ThirdSubjectIdentifierType.NIP;
            case INTERNAL_ID -> ThirdSubjectIdentifierType.INTERNAL_ID;
            case VAT_UE -> ThirdSubjectIdentifierType.VAT_UE;
            case OTHER -> ThirdSubjectIdentifierType.OTHER;
            case NONE -> ThirdSubjectIdentifierType.NONE;
        };
    }

    public static @Nullable UpoInfo toUpoInfo(@Nullable UpoResponseRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        List<UpoPage> mappedPages = rawValue.getPages().stream().map(InvoicingMappers::toUpoPage).toList();
        return new UpoInfo(mappedPages);
    }

    public static UpoPage toUpoPage(UpoPageResponseRaw rawValue) {
        return new UpoPage(
                rawValue.getReferenceNumber(),
                rawValue.getDownloadUrl(),
                rawValue.getDownloadUrlExpirationDate());
    }

}
