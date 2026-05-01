/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataRaw;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Invoice metadata from a query result.
 *
 * @param ksefNumber the KSeF-assigned invoice number
 * @param invoiceNumber the invoice number from the XML content
 * @param issueDate the issue date
 * @param invoicingDate the invoicing timestamp
 * @param acquisitionDate when the invoice was received by KSeF
 * @param permanentStorageDate when the invoice was stored permanently
 * @param seller seller information
 * @param buyer buyer information
 * @param netAmount net amount
 * @param grossAmount gross amount
 * @param vatAmount VAT amount
 * @param currency currency code (ISO 4217)
 * @param invoicingMode online or offline
 * @param invoiceType type of invoice
 * @param formCode form code identifying the schema version
 * @param selfInvoicing whether this is a self-invoice
 * @param hasAttachment whether the invoice has attachments
 * @param invoiceHash SHA-256 hash of the invoice content
 * @param hashOfCorrectedInvoice hash of the corrected invoice (for corrections)
 * @param thirdSubjects third subjects on the invoice (may be empty)
 */
public record InvoiceMetadata(
        String ksefNumber,
        String invoiceNumber,
        LocalDate issueDate,
        OffsetDateTime invoicingDate,
        OffsetDateTime acquisitionDate,
        OffsetDateTime permanentStorageDate,
        InvoiceSeller seller,
        InvoiceBuyer buyer,
        Double netAmount,
        Double grossAmount,
        Double vatAmount,
        String currency,
        InvoicingMode invoicingMode,
        InvoiceType invoiceType,
        FormCodeInfo formCode,
        Boolean selfInvoicing,
        Boolean hasAttachment,
        byte[] invoiceHash,
        byte[] hashOfCorrectedInvoice,
        List<InvoiceThirdSubject> thirdSubjects) {

    public static InvoiceMetadata from(InvoiceMetadataRaw raw) {
        List<InvoiceThirdSubject> subjects = raw.getThirdSubjects() != null
                ? raw.getThirdSubjects().stream().map(InvoiceThirdSubject::from).toList()
                : List.of();
        return new InvoiceMetadata(
                raw.getKsefNumber(),
                raw.getInvoiceNumber(),
                raw.getIssueDate(),
                raw.getInvoicingDate(),
                raw.getAcquisitionDate(),
                raw.getPermanentStorageDate(),
                InvoiceSeller.from(raw.getSeller()),
                InvoiceBuyer.from(raw.getBuyer()),
                raw.getNetAmount(),
                raw.getGrossAmount(),
                raw.getVatAmount(),
                raw.getCurrency(),
                InvoicingMode.from(raw.getInvoicingMode()),
                InvoiceType.from(raw.getInvoiceType()),
                FormCodeInfo.from(raw.getFormCode()),
                raw.getIsSelfInvoicing(),
                raw.getHasAttachment(),
                raw.getInvoiceHash(),
                raw.getHashOfCorrectedInvoice(),
                subjects);
    }
}
