/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.xml.pefkor.CreditNoteType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.CreditNoteLineType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.MonetaryTotalType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.SupplierPartyType;
import java.util.List;
import java.util.Objects;

/**
 * Read-side PEF_KOR (UBL CreditNote) document fetched from KSeF. Wraps
 * the JAXB-generated {@link CreditNoteType} root and the raw XML bytes.
 * Construct via {@link #from(byte[])}.
 *
 * <p>Accessors mirror the UBL CreditNote document structure and return
 * JAXB raw types directly. PR21 will replace these with SDK-owned UBL
 * sub-records pre-1.0; the {@link #creditNote()} escape-hatch survives.
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

    /** The underlying UBL JAXB tree. Read-only access. */
    public CreditNoteType creditNote() {
        return creditNote;
    }

    /** Supplier (seller) party block ({@code <cac:AccountingSupplierParty>}). */
    public SupplierPartyType accountingSupplierParty() {
        return creditNote.getAccountingSupplierParty();
    }

    /** Customer (buyer) party block ({@code <cac:AccountingCustomerParty>}). */
    public CustomerPartyType accountingCustomerParty() {
        return creditNote.getAccountingCustomerParty();
    }

    /** Credit-note line items ({@code <cac:CreditNoteLine>} list). */
    public List<CreditNoteLineType> lines() {
        return creditNote.getCreditNoteLine() != null ? List.copyOf(creditNote.getCreditNoteLine()) : List.of();
    }

    /** Monetary total block ({@code <cac:LegalMonetaryTotal>}). */
    public MonetaryTotalType legalMonetaryTotal() {
        return creditNote.getLegalMonetaryTotal();
    }
}
