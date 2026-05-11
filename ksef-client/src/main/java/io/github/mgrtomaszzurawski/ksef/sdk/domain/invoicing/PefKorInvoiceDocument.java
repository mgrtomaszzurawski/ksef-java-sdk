/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefCreditNoteLine;
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

/**
 * Read-side PEF_KOR (UBL CreditNote) document fetched from KSeF. Wraps
 * the JAXB-generated {@link CreditNoteType} root and the raw XML bytes.
 * Construct via {@link #from(byte[])}.
 *
 * <p>Public accessors are flat primitives that read through to the
 * underlying UBL JAXB tree on demand. The {@link #creditNote()}
 * escape-hatch provides direct access to fields the flat accessors do
 * not surface (BillingReference details, allowance/charges, tax
 * breakdowns).
 *
 * @since 1.0.0
 */
public final class PefKorInvoiceDocument implements InvoiceDocument {

    private final CreditNoteType creditNote;
    private final byte[] xmlBytes;

    PefKorInvoiceDocument(CreditNoteType creditNote, byte[] xmlBytes) {
        this.creditNote = Objects.requireNonNull(creditNote, InvoiceDocumentMessages.ERR_NULL_CREDIT_NOTE);
        this.xmlBytes = xmlBytes.clone();
    }

    /** Parse PEF_KOR UBL CreditNote XML bytes into a typed document. */
    public static PefKorInvoiceDocument from(byte[] xml) {
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
     * Underlying UBL JAXB tree — escape-hatch for fields the flat
     * accessors do not surface. Read-only access — do not mutate.
     */
    public CreditNoteType creditNote() {
        return creditNote;
    }

    /** Credit-note number from {@code <cbc:ID>}. */
    public String invoiceNumber() {
        return creditNote.getID() != null ? creditNote.getID().getValue() : null;
    }

    /** Issue date from {@code <cbc:IssueDate>}. */
    public LocalDate issueDate() {
        IssueDateType issue = creditNote.getIssueDate();
        if (issue == null || issue.getValue() == null) {
            return null;
        }
        return toLocalDate(issue.getValue());
    }

    /** Currency code from {@code <cbc:DocumentCurrencyCode>}. */
    public String currency() {
        DocumentCurrencyCodeType code = creditNote.getDocumentCurrencyCode();
        return code != null ? code.getValue() : null;
    }

    /** Supplier endpoint identifier (Peppol participant ID). */
    public String supplierEndpointId() {
        PartyType party = supplierParty();
        if (party == null || party.getEndpointID() == null) {
            return null;
        }
        return party.getEndpointID().getValue();
    }

    /** Supplier registered name from {@code Party/PartyName/Name}. */
    public String supplierName() {
        return firstPartyName(supplierParty());
    }

    /** Customer endpoint identifier (Peppol participant ID). */
    public String customerEndpointId() {
        PartyType party = customerParty();
        if (party == null || party.getEndpointID() == null) {
            return null;
        }
        return party.getEndpointID().getValue();
    }

    /** Customer registered name from {@code Party/PartyName/Name}. */
    public String customerName() {
        return firstPartyName(customerParty());
    }

    /** Total payable amount from {@code LegalMonetaryTotal/PayableAmount}. */
    public BigDecimal payableAmount() {
        MonetaryTotalType total = creditNote.getLegalMonetaryTotal();
        if (total == null || total.getPayableAmount() == null) {
            return null;
        }
        return total.getPayableAmount().getValue();
    }

    /**
     * Lines mapped from UBL {@code <cac:CreditNoteLine>} entries to
     * SDK {@link PefCreditNoteLine} records. Lines that lack any
     * required UBL field for the SDK record are skipped.
     */
    public List<PefCreditNoteLine> lines() {
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

    private PartyType supplierParty() {
        SupplierPartyType supplier = creditNote.getAccountingSupplierParty();
        return supplier != null ? supplier.getParty() : null;
    }

    private PartyType customerParty() {
        CustomerPartyType customer = creditNote.getAccountingCustomerParty();
        return customer != null ? customer.getParty() : null;
    }

    private static String firstPartyName(PartyType party) {
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
