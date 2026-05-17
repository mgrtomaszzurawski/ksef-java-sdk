/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefCreditNoteLine;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.CreditNoteType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CreditNoteLineType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.ItemType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.MonetaryTotalType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.PartyNameType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.PartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.SupplierPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.TaxCategoryType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.DocumentCurrencyCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.IssueDateType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jspecify.annotations.Nullable;

/**
 * Read-side PEF_KOR (UBL CreditNote) document fetched from KSeF. Wraps
 * the JAXB-generated {@link CreditNoteType} root and the raw XML bytes.
 * Construct via {@link #from(byte[])}.
 *
 * <p>Public accessors are flat primitives that read through to the
 * underlying UBL JAXB tree on demand. Two escape hatches expose fields
 * the flat accessors do not surface (BillingReference details,
 * allowance/charges, tax breakdowns): {@link #unsafeJaxbView()} returns
 * the live JAXB root (read-only by contract), and
 * {@link #toJaxbCopy()} returns a mutable deep clone.
 *
 * @since 1.0.0
 */
public final class PefKorInvoiceDocument implements InvoiceDocument {

    private final CreditNoteType creditNote;
    private final byte[] xmlBytes;
    private final @Nullable String invoiceNumber;
    private final @Nullable LocalDate issueDate;
    private final @Nullable String currency;
    private final @Nullable String supplierEndpointId;
    private final @Nullable String supplierName;
    private final @Nullable String customerEndpointId;
    private final @Nullable String customerName;
    private final @Nullable BigDecimal payableAmount;
    private final List<PefCreditNoteLine> lines;

    PefKorInvoiceDocument(CreditNoteType creditNote, byte[] xmlBytes) {
        this.creditNote = Objects.requireNonNull(creditNote, InvoiceDocumentMessages.ERR_NULL_CREDIT_NOTE);
        this.xmlBytes = xmlBytes.clone();
        this.invoiceNumber = creditNote.getID() != null ? creditNote.getID().getValue() : null;
        IssueDateType issue = creditNote.getIssueDate();
        this.issueDate = issue != null && issue.getValue() != null ? toLocalDate(issue.getValue()) : null;
        DocumentCurrencyCodeType code = creditNote.getDocumentCurrencyCode();
        this.currency = code != null ? code.getValue() : null;
        SupplierPartyType supplier = creditNote.getAccountingSupplierParty();
        PartyType supplierParty = supplier != null ? supplier.getParty() : null;
        this.supplierEndpointId = supplierParty != null && supplierParty.getEndpointID() != null
                ? supplierParty.getEndpointID().getValue() : null;
        this.supplierName = firstPartyName(supplierParty);
        CustomerPartyType customer = creditNote.getAccountingCustomerParty();
        PartyType customerParty = customer != null ? customer.getParty() : null;
        this.customerEndpointId = customerParty != null && customerParty.getEndpointID() != null
                ? customerParty.getEndpointID().getValue() : null;
        this.customerName = firstPartyName(customerParty);
        MonetaryTotalType total = creditNote.getLegalMonetaryTotal();
        this.payableAmount = total != null && total.getPayableAmount() != null
                ? total.getPayableAmount().getValue() : null;
        this.lines = snapshotLines(creditNote);
    }

    private static List<PefCreditNoteLine> snapshotLines(CreditNoteType creditNote) {
        if (creditNote.getCreditNoteLine() == null) {
            return List.of();
        }
        List<PefCreditNoteLine> mapped = new ArrayList<>(creditNote.getCreditNoteLine().size());
        for (CreditNoteLineType line : creditNote.getCreditNoteLine()) {
            PefCreditNoteLine item = mapLine(line);
            if (item != null) {
                mapped.add(item);
            }
        }
        return List.copyOf(mapped);
    }

    /**
     * Parse PEF_KOR UBL CreditNote XML bytes into a typed document.
     * Package-private — SDK orchestrates construction from archive
     * responses; cross-package SDK access via
     * {@code InvoiceDocumentConstructor}.
     */
    static PefKorInvoiceDocument from(byte[] xml) {
        Objects.requireNonNull(xml, InvoiceDocumentMessages.ERR_NULL_XML);
        CreditNoteType jaxb = JaxbInvoiceMarshaller.unmarshal(xml, CreditNoteType.class);
        return new PefKorInvoiceDocument(jaxb, xml);
    }

    @Override
    public FormCode formCode() {
        return FormCode.PEF_KOR3;
    }

    @Override
    public byte[] xml() {
        return xmlBytes.clone();
    }

    /**
     * Direct reference to the internal UBL JAXB {@link CreditNoteType}
     * root — escape-hatch for fields the flat accessors do not surface.
     *
     * <p><strong>Read-only by contract.</strong> Mutations are not
     * reflected in the {@link #xml()} bytes. For a mutable disconnected
     * copy use {@link #toJaxbCopy()}.
     */
    public CreditNoteType unsafeJaxbView() {
        return creditNote;
    }

    /**
     * Deep-clone of the internal UBL JAXB tree via a marshal/unmarshal
     * round-trip.
     */
    public CreditNoteType toJaxbCopy() {
        return JaxbDeepClone.clone(creditNote, CreditNoteType.class);
    }

    /** Credit-note number from {@code <cbc:ID>}. */
    public @Nullable String invoiceNumber() { return invoiceNumber; }

    /** Issue date from {@code <cbc:IssueDate>}. */
    public @Nullable LocalDate issueDate() { return issueDate; }

    /** Currency code from {@code <cbc:DocumentCurrencyCode>}. */
    public @Nullable String currency() { return currency; }

    /** Supplier endpoint identifier (Peppol participant ID). */
    public @Nullable String supplierEndpointId() { return supplierEndpointId; }

    /** Supplier registered name from {@code Party/PartyName/Name}. */
    public @Nullable String supplierName() { return supplierName; }

    /** Customer endpoint identifier (Peppol participant ID). */
    public @Nullable String customerEndpointId() { return customerEndpointId; }

    /** Customer registered name from {@code Party/PartyName/Name}. */
    public @Nullable String customerName() { return customerName; }

    /** Total payable amount from {@code LegalMonetaryTotal/PayableAmount}. */
    public @Nullable BigDecimal payableAmount() { return payableAmount; }

    /**
     * Lines mapped from UBL {@code <cac:CreditNoteLine>} entries to
     * SDK {@link PefCreditNoteLine} records. Lines that lack any
     * required UBL field for the SDK record are skipped.
     */
    public List<PefCreditNoteLine> lines() { return lines; }

    private static @Nullable String firstPartyName(@Nullable PartyType party) {
        if (party == null || party.getPartyName() == null || party.getPartyName().isEmpty()) {
            return null;
        }
        PartyNameType partyName = party.getPartyName().get(0);
        if (partyName == null || partyName.getName() == null) {
            return null;
        }
        return partyName.getName().getValue();
    }

    private static PefCreditNoteLine mapLine(CreditNoteLineType line) {
        if (line == null || line.getID() == null
                || line.getCreditedQuantity() == null
                || line.getLineExtensionAmount() == null
                || line.getItem() == null
                || line.getItem().getName() == null) {
            return null;
        }
        BigDecimal quantity = line.getCreditedQuantity().getValue();
        String unitCode = line.getCreditedQuantity().getUnitCode();
        BigDecimal amount = line.getLineExtensionAmount().getValue();
        String itemName = line.getItem().getName().getValue();
        BigDecimal vatPercent = firstClassifiedTaxPercent(line.getItem());
        if (quantity == null || unitCode == null || amount == null
                || itemName == null || vatPercent == null) {
            return null;
        }
        return new PefCreditNoteLine(
                line.getID().getValue(),
                quantity,
                unitCode,
                amount,
                itemName,
                vatPercent);
    }

    private static BigDecimal firstClassifiedTaxPercent(ItemType item) {
        if (item.getClassifiedTaxCategory() == null || item.getClassifiedTaxCategory().isEmpty()) {
            return null;
        }
        TaxCategoryType category = item.getClassifiedTaxCategory().get(0);
        if (category == null || category.getPercent() == null) {
            return null;
        }
        return category.getPercent().getValue();
    }

    private static LocalDate toLocalDate(XMLGregorianCalendar gregorian) {
        return LocalDate.of(gregorian.getYear(), gregorian.getMonth(), gregorian.getDay());
    }
}
