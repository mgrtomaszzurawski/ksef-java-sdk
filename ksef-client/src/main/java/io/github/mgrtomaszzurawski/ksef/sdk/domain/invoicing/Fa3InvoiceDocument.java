/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TNaglowek;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot1;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot2;
import java.util.List;
import java.util.Objects;

/**
 * Read-side FA(3) invoice fetched from KSeF. Wraps the JAXB-generated
 * {@link Faktura} root and the raw XML bytes returned by the server.
 * Construct via {@link #from(byte[])}.
 *
 * <p>Accessors mirror the FA(3) {@code <Faktura>} XSD shape and return
 * JAXB raw types directly. The full SDK sub-record overlay tracked by
 * PR21 will replace these with SDK-owned records pre-1.0; the
 * {@link #faktura()} escape-hatch survives the refactor.
 *
 * @since 1.0.0
 */
public final class Fa3InvoiceDocument implements InvoiceDocument {

    private final Faktura faktura;
    private final byte[] xmlBytes;

    Fa3InvoiceDocument(Faktura faktura, byte[] xmlBytes) {
        this.faktura = Objects.requireNonNull(faktura, InvoiceDocumentMessages.ERR_NULL_FAKTURA);
        this.xmlBytes = xmlBytes.clone();
    }

    /**
     * Parse FA(3) XML bytes into a typed document. The bytes are kept
     * verbatim for {@link #xml()}; the JAXB tree is unmarshalled lazily
     * for typed accessors.
     */
    public static Fa3InvoiceDocument from(byte[] xml) {
        Objects.requireNonNull(xml, InvoiceDocumentMessages.ERR_NULL_XML);
        Faktura jaxb = JaxbInvoiceMarshaller.unmarshal(xml, Faktura.class);
        return new Fa3InvoiceDocument(jaxb, xml);
    }

    @Override
    public FormCode formCode() {
        return FormCode.FA3;
    }

    @Override
    public byte[] xml() {
        return xmlBytes.clone();
    }

    /** The underlying JAXB tree. Read-only access — do not mutate. */
    public Faktura faktura() {
        return faktura;
    }

    /** Header section ({@code <Naglowek>}). */
    public TNaglowek header() {
        return faktura.getNaglowek();
    }

    /** Seller identity block ({@code <Podmiot1>/<DaneIdentyfikacyjne>}). */
    public TPodmiot1 sellerIdentity() {
        return faktura.getPodmiot1() != null ? faktura.getPodmiot1().getDaneIdentyfikacyjne() : null;
    }

    /** Buyer identity block ({@code <Podmiot2>/<DaneIdentyfikacyjne>}). */
    public TPodmiot2 buyerIdentity() {
        return faktura.getPodmiot2() != null ? faktura.getPodmiot2().getDaneIdentyfikacyjne() : null;
    }

    /** Invoice content body ({@code <Fa>}). */
    public Faktura.Fa content() {
        return faktura.getFa();
    }

    /** Invoice line items ({@code <FaWiersz>} list). */
    public List<Faktura.Fa.FaWiersz> lineItems() {
        Faktura.Fa fa = faktura.getFa();
        return fa != null && fa.getFaWiersz() != null ? List.copyOf(fa.getFaWiersz()) : List.of();
    }

    /** Footer section ({@code <Stopka>}, optional). */
    public Faktura.Stopka footer() {
        return faktura.getStopka();
    }
}
