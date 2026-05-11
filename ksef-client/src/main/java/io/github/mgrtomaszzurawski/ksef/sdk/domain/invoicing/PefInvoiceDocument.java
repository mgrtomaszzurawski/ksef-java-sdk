/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
import io.github.mgrtomaszzurawski.ksef.xml.pef.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.InvoiceLineType;
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
 * Read-side PEF (UBL Invoice) document fetched from KSeF. Wraps the
 * JAXB-generated {@link InvoiceType} root and the raw XML bytes.
 * Construct via {@link #from(byte[])}.
 *
 * <p>Public accessors are flat primitives that read through to the
 * underlying UBL JAXB tree on demand. Two escape hatches expose fields
 * the flat accessors do not surface: {@link #unsafeJaxbView()} returns
 * the live JAXB root (read-only by contract), and {@link #toJaxbCopy()}
 * returns a mutable deep clone.
 *
 * @since 1.0.0
 */
public final class PefInvoiceDocument implements InvoiceDocument {

    private final InvoiceType invoice;
    private final byte[] xmlBytes;

    PefInvoiceDocument(InvoiceType invoice, byte[] xmlBytes) {
        this.invoice = Objects.requireNonNull(invoice, InvoiceDocumentMessages.ERR_NULL_INVOICE);
        this.xmlBytes = xmlBytes.clone();
    }

    /** Parse PEF UBL Invoice XML bytes into a typed document. */
    public static PefInvoiceDocument from(byte[] xml) {
        Objects.requireNonNull(xml, InvoiceDocumentMessages.ERR_NULL_XML);
        InvoiceType jaxb = JaxbInvoiceMarshaller.unmarshal(xml, InvoiceType.class);
        return new PefInvoiceDocument(jaxb, xml);
    }

    @Override
    public FormCode formCode() {
        return FormCode.PEF3;
    }

    @Override
    public byte[] xml() {
        return xmlBytes.clone();
    }

    /**
     * Direct reference to the internal UBL JAXB {@link InvoiceType} root —
     * escape-hatch for fields the flat accessors do not surface.
     *
     * <p><strong>Read-only by contract.</strong> Mutations are not
     * reflected in the {@link #xml()} bytes. For a mutable disconnected
     * copy use {@link #toJaxbCopy()}.
     */
    public InvoiceType unsafeJaxbView() {
        return invoice;
    }

    /**
     * Deep-clone of the internal UBL JAXB tree via a marshal/unmarshal
     * round-trip.
     */
    public InvoiceType toJaxbCopy() {
        return JaxbDeepClone.clone(invoice, InvoiceType.class);
    }

    /** Invoice number from {@code <cbc:ID>}. */
    public String invoiceNumber() {
        return invoice.getID() != null ? invoice.getID().getValue() : null;
    }

    /** Issue date from {@code <cbc:IssueDate>}. */
    public LocalDate issueDate() {
        IssueDateType issue = invoice.getIssueDate();
        if (issue == null || issue.getValue() == null) {
            return null;
        }
        return toLocalDate(issue.getValue());
    }

    /** Currency code from {@code <cbc:DocumentCurrencyCode>}. */
    public String currency() {
        DocumentCurrencyCodeType code = invoice.getDocumentCurrencyCode();
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
        MonetaryTotalType total = invoice.getLegalMonetaryTotal();
        if (total == null || total.getPayableAmount() == null) {
            return null;
        }
        return total.getPayableAmount().getValue();
    }

    /**
     * Lines mapped from UBL {@code <cac:InvoiceLine>} entries to SDK
     * {@link PefInvoiceLine} records. Lines that lack any required
     * UBL field for the SDK record are skipped.
     */
    public List<PefInvoiceLine> lines() {
        if (invoice.getInvoiceLine() == null) {
            return List.of();
        }
        List<PefInvoiceLine> mapped = new ArrayList<>(invoice.getInvoiceLine().size());
        for (InvoiceLineType line : invoice.getInvoiceLine()) {
            PefInvoiceLine item = mapLine(line);
            if (item != null) {
                mapped.add(item);
            }
        }
        return List.copyOf(mapped);
    }

    private PartyType supplierParty() {
        SupplierPartyType supplier = invoice.getAccountingSupplierParty();
        return supplier != null ? supplier.getParty() : null;
    }

    private PartyType customerParty() {
        CustomerPartyType customer = invoice.getAccountingCustomerParty();
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

    private static PefInvoiceLine mapLine(InvoiceLineType line) {
        if (line == null || line.getID() == null
                || line.getInvoicedQuantity() == null
                || line.getLineExtensionAmount() == null
                || line.getItem() == null
                || line.getItem().getName() == null) {
            return null;
        }
        BigDecimal quantity = line.getInvoicedQuantity().getValue();
        String unitCode = line.getInvoicedQuantity().getUnitCode();
        BigDecimal amount = line.getLineExtensionAmount().getValue();
        String itemName = line.getItem().getName().getValue();
        BigDecimal vatPercent = firstClassifiedTaxPercent(line.getItem());
        if (quantity == null || unitCode == null || amount == null
                || itemName == null || vatPercent == null) {
            return null;
        }
        return new PefInvoiceLine(
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
