/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataRaw;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvoiceMetadata other)) {
            return false;
        }
        return Objects.equals(ksefNumber, other.ksefNumber)
                && Objects.equals(invoiceNumber, other.invoiceNumber)
                && Objects.equals(issueDate, other.issueDate)
                && Objects.equals(invoicingDate, other.invoicingDate)
                && Objects.equals(acquisitionDate, other.acquisitionDate)
                && Objects.equals(permanentStorageDate, other.permanentStorageDate)
                && Objects.equals(seller, other.seller)
                && Objects.equals(buyer, other.buyer)
                && Objects.equals(netAmount, other.netAmount)
                && Objects.equals(grossAmount, other.grossAmount)
                && Objects.equals(vatAmount, other.vatAmount)
                && Objects.equals(currency, other.currency)
                && invoicingMode == other.invoicingMode
                && invoiceType == other.invoiceType
                && Objects.equals(formCode, other.formCode)
                && Objects.equals(selfInvoicing, other.selfInvoicing)
                && Objects.equals(hasAttachment, other.hasAttachment)
                && Arrays.equals(invoiceHash, other.invoiceHash)
                && Arrays.equals(hashOfCorrectedInvoice, other.hashOfCorrectedInvoice)
                && Objects.equals(thirdSubjects, other.thirdSubjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ksefNumber, invoiceNumber, issueDate, invoicingDate,
                acquisitionDate, permanentStorageDate, seller, buyer, netAmount, grossAmount,
                vatAmount, currency, invoicingMode, invoiceType, formCode, selfInvoicing,
                hasAttachment, thirdSubjects,
                Arrays.hashCode(invoiceHash), Arrays.hashCode(hashOfCorrectedInvoice));
    }

    @Override
    public String toString() {
        return "InvoiceMetadata[ksefNumber=" + ksefNumber
                + ", invoiceNumber=" + invoiceNumber
                + ", issueDate=" + issueDate
                + ", invoicingDate=" + invoicingDate
                + ", acquisitionDate=" + acquisitionDate
                + ", permanentStorageDate=" + permanentStorageDate
                + ", seller=" + seller
                + ", buyer=" + buyer
                + ", netAmount=" + netAmount
                + ", grossAmount=" + grossAmount
                + ", vatAmount=" + vatAmount
                + ", currency=" + currency
                + ", invoicingMode=" + invoicingMode
                + ", invoiceType=" + invoiceType
                + ", formCode=" + formCode
                + ", selfInvoicing=" + selfInvoicing
                + ", hasAttachment=" + hasAttachment
                + ", invoiceHash=byte[" + (invoiceHash == null ? 0 : invoiceHash.length) + "]"
                + ", hashOfCorrectedInvoice=byte[" + (hashOfCorrectedInvoice == null ? 0 : hashOfCorrectedInvoice.length) + "]"
                + ", thirdSubjects=" + thirdSubjects + "]";
    }
}
