/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

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
 *
 * @since 1.0.0
 */
public record InvoiceMetadata(
        KsefNumber ksefNumber,
        String invoiceNumber,
        LocalDate issueDate,
        OffsetDateTime invoicingDate,
        OffsetDateTime acquisitionDate,
        OffsetDateTime permanentStorageDate,
        @Nullable InvoiceSeller seller,
        @Nullable InvoiceBuyer buyer,
        @Nullable Double netAmount,
        @Nullable Double grossAmount,
        @Nullable Double vatAmount,
        @Nullable String currency,
        @Nullable InvoicingMode invoicingMode,
        @Nullable InvoiceType invoiceType,
        @Nullable FormCodeInfo formCode,
        @Nullable Boolean selfInvoicing,
        @Nullable Boolean hasAttachment,
        byte @Nullable [] invoiceHash,
        byte @Nullable [] hashOfCorrectedInvoice,
        List<InvoiceThirdSubject> thirdSubjects) {

    public InvoiceMetadata {
        invoiceHash = invoiceHash == null ? null : invoiceHash.clone();
        hashOfCorrectedInvoice = hashOfCorrectedInvoice == null ? null : hashOfCorrectedInvoice.clone();
        thirdSubjects = thirdSubjects == null ? List.of() : List.copyOf(thirdSubjects);
    }

    @Override
    public byte @Nullable [] invoiceHash() {
        return invoiceHash == null ? null : invoiceHash.clone();
    }

    @Override
    public byte @Nullable [] hashOfCorrectedInvoice() {
        return hashOfCorrectedInvoice == null ? null : hashOfCorrectedInvoice.clone();
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
