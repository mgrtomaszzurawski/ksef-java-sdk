/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoiceStatusResponseRaw;
import java.net.URI;
import java.time.OffsetDateTime;

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
 */
public record SessionInvoiceStatus(
        int ordinalNumber,
        String invoiceNumber,
        String ksefNumber,
        String referenceNumber,
        byte[] invoiceHash,
        String invoiceFileName,
        OffsetDateTime acquisitionDate,
        OffsetDateTime invoicingDate,
        OffsetDateTime permanentStorageDate,
        URI upoDownloadUrl,
        OffsetDateTime upoDownloadUrlExpirationDate,
        InvoicingMode invoicingMode,
        InvoiceStatusInfo status) {

    public static SessionInvoiceStatus from(SessionInvoiceStatusResponseRaw raw) {
        return new SessionInvoiceStatus(
                raw.getOrdinalNumber() != null ? raw.getOrdinalNumber() : 0,
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
                InvoicingMode.from(raw.getInvoicingMode()),
                InvoiceStatusInfo.from(raw.getStatus()));
    }
}
