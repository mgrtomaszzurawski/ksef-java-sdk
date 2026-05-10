/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TNaglowek;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot1;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot2;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Read-side FA(2) invoice fetched from KSeF. Wraps the JAXB-generated
 * {@link Faktura} root and the raw XML bytes returned by the server.
 * Construct via {@link #from(byte[])}.
 *
 * <p>Public accessors are flat primitives that read through to the
 * underlying JAXB tree on demand. The {@link #faktura()} escape-hatch
 * provides direct access to fields the flat accessors do not surface.
 *
 * @since 1.0.0
 */
public final class Fa2InvoiceDocument implements InvoiceDocument {

    private final Faktura faktura;
    private final byte[] xmlBytes;

    Fa2InvoiceDocument(Faktura faktura, byte[] xmlBytes) {
        this.faktura = Objects.requireNonNull(faktura, InvoiceDocumentMessages.ERR_NULL_FAKTURA);
        this.xmlBytes = xmlBytes.clone();
    }

    /** Parse FA(2) XML bytes into a typed document. */
    public static Fa2InvoiceDocument from(byte[] xml) {
        Objects.requireNonNull(xml, InvoiceDocumentMessages.ERR_NULL_XML);
        Faktura jaxb = JaxbInvoiceMarshaller.unmarshal(xml, Faktura.class);
        return new Fa2InvoiceDocument(jaxb, xml);
    }

    @Override
    public FormCode formCode() {
        return FormCode.FA2;
    }

    @Override
    public byte[] xml() {
        return xmlBytes.clone();
    }

    /**
     * Underlying JAXB tree — escape-hatch for fields the flat
     * accessors do not surface. Read-only access — do not mutate.
     */
    public Faktura faktura() {
        return faktura;
    }

    /** Form-systemCode token from {@code Naglowek/KodFormularza/@kodSystemowy}. */
    public String systemCode() {
        TNaglowek header = faktura.getNaglowek();
        if (header == null || header.getKodFormularza() == null) {
            return null;
        }
        return header.getKodFormularza().getKodSystemowy();
    }

    /** Schema version token from {@code Naglowek/KodFormularza/@wersjaSchemy}. */
    public String formVersion() {
        TNaglowek header = faktura.getNaglowek();
        if (header == null || header.getKodFormularza() == null) {
            return null;
        }
        return header.getKodFormularza().getWersjaSchemy();
    }

    /** Issue timestamp from {@code Naglowek/DataWytworzeniaFa}. */
    public OffsetDateTime issuedAt() {
        TNaglowek header = faktura.getNaglowek();
        if (header == null || header.getDataWytworzeniaFa() == null) {
            return null;
        }
        return toOffsetDateTime(header.getDataWytworzeniaFa());
    }

    /** Seller NIP from {@code Podmiot1/DaneIdentyfikacyjne/NIP}. */
    public String sellerNip() {
        TPodmiot1 identity = sellerIdentityInternal();
        return identity != null ? identity.getNIP() : null;
    }

    /** Seller name from {@code Podmiot1/DaneIdentyfikacyjne/Nazwa}. */
    public String sellerName() {
        TPodmiot1 identity = sellerIdentityInternal();
        return identity != null ? identity.getNazwa() : null;
    }

    /** Buyer NIP from {@code Podmiot2/DaneIdentyfikacyjne/NIP}. */
    public String buyerNip() {
        TPodmiot2 identity = buyerIdentityInternal();
        return identity != null ? identity.getNIP() : null;
    }

    /** Buyer name from {@code Podmiot2/DaneIdentyfikacyjne/Nazwa}. */
    public String buyerName() {
        TPodmiot2 identity = buyerIdentityInternal();
        return identity != null ? identity.getNazwa() : null;
    }

    /** Invoice number from {@code Fa/P_2}. */
    public String invoiceNumber() {
        Faktura.Fa fa = faktura.getFa();
        return fa != null ? fa.getP2() : null;
    }

    /** Issue date from {@code Fa/P_1}. */
    public LocalDate issueDate() {
        Faktura.Fa fa = faktura.getFa();
        if (fa == null || fa.getP1() == null) {
            return null;
        }
        return toLocalDate(fa.getP1());
    }

    /** ISO 4217 currency code from {@code Fa/KodWaluty}. */
    public String currency() {
        Faktura.Fa fa = faktura.getFa();
        if (fa == null || fa.getKodWaluty() == null) {
            return null;
        }
        return fa.getKodWaluty().value();
    }

    /** Gross total from {@code Fa/P_15}. */
    public BigDecimal grossTotal() {
        Faktura.Fa fa = faktura.getFa();
        return fa != null ? fa.getP15() : null;
    }

    /** Optional net total from {@code Fa/P_13_1}. */
    public Optional<BigDecimal> netTotal() {
        Faktura.Fa fa = faktura.getFa();
        return fa != null ? Optional.ofNullable(fa.getP131()) : Optional.empty();
    }

    /** Invoice type code from {@code Fa/RodzajFaktury}. */
    public String invoiceTypeCode() {
        Faktura.Fa fa = faktura.getFa();
        if (fa == null || fa.getRodzajFaktury() == null) {
            return null;
        }
        return fa.getRodzajFaktury().value();
    }

    /**
     * Line items mapped from {@code Fa/FaWiersz} entries to SDK
     * records. Returns an empty list when the underlying JAXB tree
     * has no line items. Lines whose JAXB element lacks the required
     * fields {@code P_7}, {@code P_11} or {@code P_12} are skipped.
     */
    public List<InvoiceLineItem> lineItems() {
        Faktura.Fa fa = faktura.getFa();
        if (fa == null || fa.getFaWiersz() == null) {
            return List.of();
        }
        List<InvoiceLineItem> mapped = new ArrayList<>(fa.getFaWiersz().size());
        for (Faktura.Fa.FaWiersz wiersz : fa.getFaWiersz()) {
            InvoiceLineItem item = mapLineItem(wiersz);
            if (item != null) {
                mapped.add(item);
            }
        }
        return List.copyOf(mapped);
    }

    private TPodmiot1 sellerIdentityInternal() {
        return faktura.getPodmiot1() != null ? faktura.getPodmiot1().getDaneIdentyfikacyjne() : null;
    }

    private TPodmiot2 buyerIdentityInternal() {
        return faktura.getPodmiot2() != null ? faktura.getPodmiot2().getDaneIdentyfikacyjne() : null;
    }

    private static InvoiceLineItem mapLineItem(Faktura.Fa.FaWiersz wiersz) {
        if (wiersz == null || wiersz.getP7() == null
                || wiersz.getP11() == null || wiersz.getP12() == null) {
            return null;
        }
        int rowNumber = wiersz.getNrWierszaFa() != null ? wiersz.getNrWierszaFa().intValue() : 1;
        return new InvoiceLineItem(
                rowNumber,
                wiersz.getP7(),
                wiersz.getP8A(),
                wiersz.getP8B(),
                wiersz.getP9A(),
                wiersz.getP11(),
                wiersz.getP12());
    }

    private static OffsetDateTime toOffsetDateTime(XMLGregorianCalendar gregorian) {
        return gregorian.toGregorianCalendar().toZonedDateTime()
                .withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private static LocalDate toLocalDate(XMLGregorianCalendar gregorian) {
        return LocalDate.of(gregorian.getYear(), gregorian.getMonth(), gregorian.getDay());
    }
}
