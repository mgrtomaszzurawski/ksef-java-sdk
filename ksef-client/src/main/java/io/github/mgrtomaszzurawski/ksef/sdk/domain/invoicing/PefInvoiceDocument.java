/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.xml.pef.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.InvoiceLineType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.MonetaryTotalType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.SupplierPartyType;
import java.util.List;
import java.util.Objects;

/**
 * Read-side PEF (UBL Invoice) document fetched from KSeF. Wraps the
 * JAXB-generated {@link InvoiceType} root and the raw XML bytes.
 * Construct via {@link #from(byte[])}.
 *
 * <p>Accessors mirror the UBL Invoice document structure and return
 * JAXB raw types directly. PR21 will replace these with SDK-owned UBL
 * sub-records pre-1.0; the {@link #invoice()} escape-hatch survives.
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

    /** The underlying UBL JAXB tree. Read-only access. */
    public InvoiceType invoice() {
        return invoice;
    }

    /** Supplier (seller) party block ({@code <cac:AccountingSupplierParty>}). */
    public SupplierPartyType accountingSupplierParty() {
        return invoice.getAccountingSupplierParty();
    }

    /** Customer (buyer) party block ({@code <cac:AccountingCustomerParty>}). */
    public CustomerPartyType accountingCustomerParty() {
        return invoice.getAccountingCustomerParty();
    }

    /** Invoice line items ({@code <cac:InvoiceLine>} list). */
    public List<InvoiceLineType> lines() {
        return invoice.getInvoiceLine() != null ? List.copyOf(invoice.getInvoiceLine()) : List.of();
    }

    /** Monetary total block ({@code <cac:LegalMonetaryTotal>}). */
    public MonetaryTotalType legalMonetaryTotal() {
        return invoice.getLegalMonetaryTotal();
    }
}
