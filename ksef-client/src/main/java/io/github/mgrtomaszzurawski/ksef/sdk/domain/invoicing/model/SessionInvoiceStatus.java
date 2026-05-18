/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Status of an invoice within a session.
 *
 * @param ordinalNumber invoice sequence number in the session
 * @param invoiceNumber the invoice number from the XML content
 * @param ksefNumber the KSeF-assigned invoice number
 * @param referenceNumber invoice reference number
 * @param invoiceHash SHA-256 hash of the invoice content
 * @param invoiceFileName original file name
 * @param acquisitionDate when the invoice was received by KSeF
 * @param invoicingDate the invoicing date from the invoice content
 * @param permanentStorageDate when the invoice was stored permanently
 * @param upoDownloadUrl URL to download the UPO for this invoice
 * @param upoDownloadUrlExpirationDate when the UPO download URL expires
 * @param invoicingMode whether submitted online or offline
 * @param status invoice processing status
 *
 * @since 1.0.0
 */
public record SessionInvoiceStatus(
        int ordinalNumber,
        String invoiceNumber,
        @Nullable KsefNumber ksefNumber,
        String referenceNumber,
        byte @Nullable [] invoiceHash,
        @Nullable String invoiceFileName,
        OffsetDateTime acquisitionDate,
        OffsetDateTime invoicingDate,
        @Nullable OffsetDateTime permanentStorageDate,
        @Nullable URI upoDownloadUrl,
        @Nullable OffsetDateTime upoDownloadUrlExpirationDate,
        @Nullable InvoicingMode invoicingMode,
        @Nullable InvoiceStatusInfo status) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionInvoiceStatus other)) {
            return false;
        }
        return ordinalNumber == other.ordinalNumber
                && Objects.equals(invoiceNumber, other.invoiceNumber)
                && Objects.equals(ksefNumber, other.ksefNumber)
                && Objects.equals(referenceNumber, other.referenceNumber)
                && Arrays.equals(invoiceHash, other.invoiceHash)
                && Objects.equals(invoiceFileName, other.invoiceFileName)
                && Objects.equals(acquisitionDate, other.acquisitionDate)
                && Objects.equals(invoicingDate, other.invoicingDate)
                && Objects.equals(permanentStorageDate, other.permanentStorageDate)
                && Objects.equals(upoDownloadUrl, other.upoDownloadUrl)
                && Objects.equals(upoDownloadUrlExpirationDate, other.upoDownloadUrlExpirationDate)
                && invoicingMode == other.invoicingMode
                && Objects.equals(status, other.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ordinalNumber, invoiceNumber, ksefNumber, referenceNumber,
                invoiceFileName, acquisitionDate, invoicingDate, permanentStorageDate,
                upoDownloadUrl, upoDownloadUrlExpirationDate, invoicingMode, status,
                Arrays.hashCode(invoiceHash));
    }

    @Override
    public String toString() {
        return "SessionInvoiceStatus[ordinalNumber=" + ordinalNumber
                + ", invoiceNumber=" + invoiceNumber
                + ", ksefNumber=" + ksefNumber
                + ", referenceNumber=" + referenceNumber
                + ", invoiceHash=byte[" + (invoiceHash == null ? 0 : invoiceHash.length) + "]"
                + ", invoiceFileName=" + invoiceFileName
                + ", acquisitionDate=" + acquisitionDate
                + ", invoicingDate=" + invoicingDate
                + ", permanentStorageDate=" + permanentStorageDate
                + ", upoDownloadUrl=" + upoDownloadUrl
                + ", upoDownloadUrlExpirationDate=" + upoDownloadUrlExpirationDate
                + ", invoicingMode=" + invoicingMode
                + ", status=" + status + "]";
    }
}
