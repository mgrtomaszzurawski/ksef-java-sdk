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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BuyerIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportInvoicesResult;
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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ThirdSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoPage;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;
import java.util.List;
import java.util.Map;

/**
 * Internal mappers from generated {@code *Raw} types to public invoicing
 * domain records. Lives in a non-exported package; consumers can't reach it.
 */
public final class InvoicingMappers {

    private InvoicingMappers() { }

    public static io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw toSendInvoiceRequestRaw(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceRequest request) {
        var raw = new io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw();
        raw.setInvoiceHash(request.invoiceHash());
        raw.setInvoiceSize(request.invoiceSize());
        raw.setEncryptedInvoiceHash(request.encryptedInvoiceHash());
        raw.setEncryptedInvoiceSize(request.encryptedInvoiceSize());
        raw.setEncryptedInvoiceContent(request.encryptedInvoiceContent());
        raw.setOfflineMode(request.offlineMode());
        return raw;
    }

    public static BatchSession toBatchSession(OpenBatchSessionResponseRaw raw) {
            List<PartUploadRequest> parts = raw.getPartUploadRequests().stream().map(InvoicingMappers::toPartUploadRequest).toList();
            return new BatchSession(raw.getReferenceNumber(), parts);
        
    }

    public static BatchSessionLimits toBatchSessionLimits(BatchSessionEffectiveContextLimitsRaw raw) {
            if (raw == null) {
                return null;
            }
            return new BatchSessionLimits(
                    raw.getMaxInvoiceSizeInMB(),
                    raw.getMaxInvoiceWithAttachmentSizeInMB(),
                    raw.getMaxInvoices());
        
    }

    public static BuyerIdentifierType toBuyerIdentifierType(BuyerIdentifierTypeRaw raw) {
            if (raw == null) {
                return null;
            }
            return switch (raw) {
                case NIP -> BuyerIdentifierType.NIP;
                case VAT_UE -> BuyerIdentifierType.VAT_UE;
                case OTHER -> BuyerIdentifierType.OTHER;
                case NONE -> BuyerIdentifierType.NONE;
            };
        
    }

    public static ExportInvoicesResult toExportInvoicesResult(ExportInvoicesResponseRaw raw) {
            return new ExportInvoicesResult(raw.getReferenceNumber());
        
    }

    public static FormCodeInfo toFormCodeInfo(FormCodeRaw raw) {
            if (raw == null) {
                return null;
            }
            return new FormCodeInfo(raw.getSystemCode(), raw.getSchemaVersion(), raw.getValue());
        
    }

    public static InvoiceBuyer toInvoiceBuyer(InvoiceMetadataBuyerRaw raw) {
            if (raw == null) {
                return null;
            }
            BuyerIdentifierType idType = null;
            String idValue = null;
            if (raw.getIdentifier() != null) {
                idType = InvoicingMappers.toBuyerIdentifierType(raw.getIdentifier().getType());
                idValue = raw.getIdentifier().getValue();
            }
            return new InvoiceBuyer(idType, idValue, raw.getName());
        
    }

    public static InvoiceExportStatus toInvoiceExportStatus(InvoiceExportStatusResponseRaw raw) {
            return new InvoiceExportStatus(
                    CommonMappers.toStatusInfo(raw.getStatus()),
                    raw.getCompletedDate(),
                    raw.getPackageExpirationDate(),
                    InvoicingMappers.toInvoicePackage(raw.getPackage()));
        
    }

    public static InvoiceMetadata toInvoiceMetadata(InvoiceMetadataRaw raw) {
            List<InvoiceThirdSubject> subjects = raw.getThirdSubjects() != null
                    ? raw.getThirdSubjects().stream().map(InvoicingMappers::toInvoiceThirdSubject).toList()
                    : List.of();
            return new InvoiceMetadata(
                    raw.getKsefNumber(),
                    raw.getInvoiceNumber(),
                    raw.getIssueDate(),
                    raw.getInvoicingDate(),
                    raw.getAcquisitionDate(),
                    raw.getPermanentStorageDate(),
                    InvoicingMappers.toInvoiceSeller(raw.getSeller()),
                    InvoicingMappers.toInvoiceBuyer(raw.getBuyer()),
                    raw.getNetAmount(),
                    raw.getGrossAmount(),
                    raw.getVatAmount(),
                    raw.getCurrency(),
                    InvoicingMappers.toInvoicingMode(raw.getInvoicingMode()),
                    InvoicingMappers.toInvoiceType(raw.getInvoiceType()),
                    InvoicingMappers.toFormCodeInfo(raw.getFormCode()),
                    raw.getIsSelfInvoicing(),
                    raw.getHasAttachment(),
                    raw.getInvoiceHash(),
                    raw.getHashOfCorrectedInvoice(),
                    subjects);
        
    }

    public static InvoiceMetadataResult toInvoiceMetadataResult(QueryInvoicesMetadataResponseRaw raw) {
            List<InvoiceMetadata> mapped = raw.getInvoices().stream().map(InvoicingMappers::toInvoiceMetadata).toList();
            return new InvoiceMetadataResult(
                    raw.getHasMore(),
                    raw.getIsTruncated(),
                    raw.getPermanentStorageHwmDate(),
                    mapped);
        
    }

    public static InvoicePackage toInvoicePackage(InvoicePackageRaw raw) {
            if (raw == null) {
                return null;
            }
            List<InvoicePackagePart> mappedParts = raw.getParts().stream().map(InvoicingMappers::toInvoicePackagePart).toList();
            return new InvoicePackage(
                    raw.getInvoiceCount(),
                    raw.getSize(),
                    mappedParts,
                    raw.getIsTruncated(),
                    raw.getLastIssueDate(),
                    raw.getLastInvoicingDate(),
                    raw.getLastPermanentStorageDate(),
                    raw.getPermanentStorageHwmDate());
        
    }

    public static InvoicePackagePart toInvoicePackagePart(InvoicePackagePartRaw raw) {
            return new InvoicePackagePart(
                    raw.getOrdinalNumber(),
                    raw.getPartName(),
                    raw.getMethod(),
                    raw.getUrl(),
                    raw.getPartSize(),
                    raw.getPartHash(),
                    raw.getEncryptedPartSize(),
                    raw.getEncryptedPartHash(),
                    raw.getExpirationDate());
        
    }

    public static InvoiceSeller toInvoiceSeller(InvoiceMetadataSellerRaw raw) {
            if (raw == null) {
                return null;
            }
            return new InvoiceSeller(raw.getNip(), raw.getName());
        
    }

    public static InvoiceStatusInfo toInvoiceStatusInfo(InvoiceStatusInfoRaw raw) {
            if (raw == null) {
                return null;
            }
            return new InvoiceStatusInfo(
                    raw.getCode(),
                    raw.getDescription(),
                    raw.getDetails() != null ? List.copyOf(raw.getDetails()) : List.of(),
                    raw.getExtensions() != null ? Map.copyOf(raw.getExtensions()) : Map.of());
        
    }

    public static InvoiceThirdSubject toInvoiceThirdSubject(InvoiceMetadataThirdSubjectRaw raw) {
            ThirdSubjectIdentifierType idType = null;
            String idValue = null;
            if (raw.getIdentifier() != null) {
                idType = InvoicingMappers.toThirdSubjectIdentifierType(raw.getIdentifier().getType());
                idValue = raw.getIdentifier().getValue();
            }
            return new InvoiceThirdSubject(idType, idValue, raw.getName(), raw.getRole());
        
    }

    public static InvoiceType toInvoiceType(InvoiceTypeRaw raw) {
            if (raw == null) {
                return null;
            }
            return switch (raw) {
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

    public static InvoicingMode toInvoicingMode(InvoicingModeRaw raw) {
            if (raw == null) {
                return null;
            }
            return switch (raw) {
                case ONLINE -> InvoicingMode.ONLINE;
                case OFFLINE -> InvoicingMode.OFFLINE;
            };
        
    }

    public static OnlineSession toOnlineSession(OpenOnlineSessionResponseRaw raw) {
            return new OnlineSession(raw.getReferenceNumber(), raw.getValidUntil());
        
    }

    public static OnlineSessionLimits toOnlineSessionLimits(OnlineSessionEffectiveContextLimitsRaw raw) {
            if (raw == null) {
                return null;
            }
            return new OnlineSessionLimits(
                    raw.getMaxInvoiceSizeInMB(),
                    raw.getMaxInvoiceWithAttachmentSizeInMB(),
                    raw.getMaxInvoices());
        
    }

    public static PartUploadRequest toPartUploadRequest(PartUploadRequestRaw raw) {
            return new PartUploadRequest(
                    raw.getOrdinalNumber(),
                    raw.getMethod(),
                    raw.getUrl(),
                    Map.copyOf(raw.getHeaders()));
        
    }

    public static SendInvoiceResult toSendInvoiceResult(SendInvoiceResponseRaw raw) {
            return new SendInvoiceResult(raw.getReferenceNumber());
        
    }

    public static SessionInvoices toSessionInvoices(SessionInvoicesResponseRaw raw) {
            List<SessionInvoiceStatus> mapped = raw.getInvoices().stream().map(InvoicingMappers::toSessionInvoiceStatus).toList();
            return new SessionInvoices(raw.getContinuationToken(), mapped);
        
    }

    public static SessionInvoiceStatus toSessionInvoiceStatus(SessionInvoiceStatusResponseRaw raw) {
            return new SessionInvoiceStatus(
                    raw.getOrdinalNumber(),
                    raw.getInvoiceNumber(),
                    raw.getKsefNumber(),
                    raw.getReferenceNumber(),
                    raw.getInvoiceHash(),
                    raw.getInvoiceFileName(),
                    raw.getAcquisitionDate(),
                    raw.getInvoicingDate(),
                    raw.getPermanentStorageDate(),
                    raw.getUpoDownloadUrl(),
                    raw.getUpoDownloadUrlExpirationDate(),
                    InvoicingMappers.toInvoicingMode(raw.getInvoicingMode()),
                    InvoicingMappers.toInvoiceStatusInfo(raw.getStatus()));
        
    }

    public static SessionStatus toSessionStatus(SessionStatusResponseRaw raw) {
            return new SessionStatus(
                    CommonMappers.toStatusInfo(raw.getStatus()),
                    raw.getDateCreated(),
                    raw.getDateUpdated(),
                    raw.getValidUntil(),
                    InvoicingMappers.toUpoInfo(raw.getUpo()),
                    raw.getInvoiceCount(),
                    raw.getSuccessfulInvoiceCount(),
                    raw.getFailedInvoiceCount());
        
    }

    public static ThirdSubjectIdentifierType toThirdSubjectIdentifierType(ThirdSubjectIdentifierTypeRaw raw) {
            if (raw == null) {
                return null;
            }
            return switch (raw) {
                case NIP -> ThirdSubjectIdentifierType.NIP;
                case INTERNAL_ID -> ThirdSubjectIdentifierType.INTERNAL_ID;
                case VAT_UE -> ThirdSubjectIdentifierType.VAT_UE;
                case OTHER -> ThirdSubjectIdentifierType.OTHER;
                case NONE -> ThirdSubjectIdentifierType.NONE;
            };
        
    }

    public static UpoInfo toUpoInfo(UpoResponseRaw raw) {
            if (raw == null) {
                return null;
            }
            List<UpoPage> mappedPages = raw.getPages().stream().map(InvoicingMappers::toUpoPage).toList();
            return new UpoInfo(mappedPages);
        
    }

    public static UpoPage toUpoPage(UpoPageResponseRaw raw) {
            return new UpoPage(
                    raw.getReferenceNumber(),
                    raw.getDownloadUrl(),
                    raw.getDownloadUrlExpirationDate());
        
    }

}
