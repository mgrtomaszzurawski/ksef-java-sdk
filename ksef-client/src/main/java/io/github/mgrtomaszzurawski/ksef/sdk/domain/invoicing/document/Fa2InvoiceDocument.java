/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceArchive;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.ClosedSession;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
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
import org.jspecify.annotations.Nullable;

/**
 * Read-side FA(2) invoice fetched from KSeF. Wraps the JAXB-generated
 * {@link Faktura} root and the raw XML bytes returned by the server.
 *
 * <p>Constructed by the SDK; consumers receive instances via
 * {@link InvoiceArchive#getByKsefNumber} or
 * {@link ClosedSession#cleared}. The {@code from(byte[])} factory is
 * package-private — cross-package SDK construction is routed through
 * {@code InvoiceDocumentConstructor} (R1-5 reflective bridge).
 *
 * <p>Public accessors are flat primitives snapshotted at construction;
 * mutations to {@link #unsafeJaxbView()} do not affect the flat accessor
 * outputs or the {@link #xml()} bytes. Two escape hatches expose fields
 * the flat accessors do not surface: {@link #unsafeJaxbView()} returns
 * the live JAXB root (read-only by contract), and {@link #toJaxbCopy()}
 * returns a mutable deep clone.
 *
 * @since 1.0.0
 */
public final class Fa2InvoiceDocument implements InvoiceDocument {

    private final Faktura faktura;
    private final byte[] xmlBytes;
    private final @Nullable String systemCode;
    private final @Nullable String formVersion;
    private final @Nullable OffsetDateTime issuedAt;
    private final @Nullable String sellerNip;
    private final @Nullable String sellerName;
    private final @Nullable String buyerNip;
    private final @Nullable String buyerName;
    private final @Nullable String invoiceNumber;
    private final @Nullable LocalDate issueDate;
    private final @Nullable String currency;
    private final @Nullable BigDecimal grossTotal;
    private final Optional<BigDecimal> netTotal;
    private final @Nullable String invoiceTypeCode;
    private final List<InvoiceLineItem> lineItems;

    Fa2InvoiceDocument(Faktura faktura, byte[] xmlBytes) {
        this.faktura = Objects.requireNonNull(faktura, InvoiceDocumentMessages.ERR_NULL_FAKTURA);
        this.xmlBytes = xmlBytes.clone();
        HeaderSnapshot header = HeaderSnapshot.from(faktura.getNaglowek());
        this.systemCode = header.systemCode;
        this.formVersion = header.formVersion;
        this.issuedAt = header.issuedAt;
        PartySnapshot seller = PartySnapshot.fromSeller(faktura.getPodmiot1());
        this.sellerNip = seller.nip;
        this.sellerName = seller.name;
        PartySnapshot buyer = PartySnapshot.fromBuyer(faktura.getPodmiot2());
        this.buyerNip = buyer.nip;
        this.buyerName = buyer.name;
        FaSnapshot fa = FaSnapshot.from(faktura.getFa());
        this.invoiceNumber = fa.invoiceNumber;
        this.issueDate = fa.issueDate;
        this.currency = fa.currency;
        this.grossTotal = fa.grossTotal;
        this.netTotal = fa.netTotal;
        this.invoiceTypeCode = fa.invoiceTypeCode;
        this.lineItems = fa.lineItems;
    }

    private record HeaderSnapshot(@Nullable String systemCode,
                                  @Nullable String formVersion,
                                  @Nullable OffsetDateTime issuedAt) {
        static HeaderSnapshot from(@Nullable TNaglowek header) {
            if (header == null) {
                return new HeaderSnapshot(null, null, null);
            }
            TNaglowek.KodFormularza kodFormularza = header.getKodFormularza();
            String systemCode = kodFormularza != null ? kodFormularza.getKodSystemowy() : null;
            String formVersion = kodFormularza != null ? kodFormularza.getWersjaSchemy() : null;
            OffsetDateTime issuedAt = header.getDataWytworzeniaFa() != null
                    ? toOffsetDateTime(header.getDataWytworzeniaFa()) : null;
            return new HeaderSnapshot(systemCode, formVersion, issuedAt);
        }
    }

    private record PartySnapshot(@Nullable String nip, @Nullable String name) {
        static PartySnapshot fromSeller(Faktura.@Nullable Podmiot1 podmiot) {
            TPodmiot1 identity = podmiot != null ? podmiot.getDaneIdentyfikacyjne() : null;
            return new PartySnapshot(
                    identity != null ? identity.getNIP() : null,
                    identity != null ? identity.getNazwa() : null);
        }

        static PartySnapshot fromBuyer(Faktura.@Nullable Podmiot2 podmiot) {
            TPodmiot2 identity = podmiot != null ? podmiot.getDaneIdentyfikacyjne() : null;
            return new PartySnapshot(
                    identity != null ? identity.getNIP() : null,
                    identity != null ? identity.getNazwa() : null);
        }
    }

    private record FaSnapshot(@Nullable String invoiceNumber,
                              @Nullable LocalDate issueDate,
                              @Nullable String currency,
                              @Nullable BigDecimal grossTotal,
                              Optional<BigDecimal> netTotal,
                              @Nullable String invoiceTypeCode,
                              List<InvoiceLineItem> lineItems) {
        static FaSnapshot from(Faktura.@Nullable Fa faContent) {
            if (faContent == null) {
                return new FaSnapshot(null, null, null, null, Optional.empty(), null, List.of());
            }
            return new FaSnapshot(
                    faContent.getP2(),
                    faContent.getP1() != null ? toLocalDate(faContent.getP1()) : null,
                    faContent.getKodWaluty() != null ? faContent.getKodWaluty().value() : null,
                    faContent.getP15(),
                    Optional.ofNullable(faContent.getP131()),
                    faContent.getRodzajFaktury() != null ? faContent.getRodzajFaktury().value() : null,
                    snapshotLineItems(faContent));
        }
    }

    /**
     * Parse FA(2) XML bytes into a typed document. Package-private —
     * SDK orchestrates construction from archive responses; cross-package
     * SDK access via {@code InvoiceDocumentConstructor}.
     */
    static Fa2InvoiceDocument from(byte[] xml) {
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
     * Direct reference to the internal JAXB {@link Faktura} root —
     * escape-hatch for fields the flat accessors do not surface.
     *
     * <p><strong>Read-only by contract.</strong> Mutations are not
     * reflected in the {@link #xml()} bytes nor in the flat accessors
     * (which snapshot at construction). For a mutable disconnected
     * copy use {@link #toJaxbCopy()}.
     */
    public Faktura unsafeJaxbView() {
        return faktura;
    }

    /**
     * Deep-clone of the internal JAXB tree via a marshal/unmarshal
     * round-trip. The returned object is mutable but shares no
     * references with this document — mutations do not affect
     * {@link #xml()}.
     */
    public Faktura toJaxbCopy() {
        return JaxbDeepClone.clone(faktura, Faktura.class);
    }

    /** Form-systemCode token from {@code Naglowek/KodFormularza/@kodSystemowy}. */
    public @Nullable String systemCode() { return systemCode; }

    /** Schema version token from {@code Naglowek/KodFormularza/@wersjaSchemy}. */
    public @Nullable String formVersion() { return formVersion; }

    /** Issue timestamp from {@code Naglowek/DataWytworzeniaFa}. */
    public @Nullable OffsetDateTime issuedAt() { return issuedAt; }

    /** Seller NIP from {@code Podmiot1/DaneIdentyfikacyjne/NIP}. */
    public @Nullable String sellerNip() { return sellerNip; }

    /** Seller name from {@code Podmiot1/DaneIdentyfikacyjne/Nazwa}. */
    public @Nullable String sellerName() { return sellerName; }

    /** Buyer NIP from {@code Podmiot2/DaneIdentyfikacyjne/NIP}. */
    public @Nullable String buyerNip() { return buyerNip; }

    /** Buyer name from {@code Podmiot2/DaneIdentyfikacyjne/Nazwa}. */
    public @Nullable String buyerName() { return buyerName; }

    /** Invoice number from {@code Fa/P_2}. */
    public @Nullable String invoiceNumber() { return invoiceNumber; }

    /** Issue date from {@code Fa/P_1}. */
    public @Nullable LocalDate issueDate() { return issueDate; }

    /** ISO 4217 currency code from {@code Fa/KodWaluty}. */
    public @Nullable String currency() { return currency; }

    /** Gross total from {@code Fa/P_15}. */
    public @Nullable BigDecimal grossTotal() { return grossTotal; }

    /** Optional net total from {@code Fa/P_13_1}. */
    public Optional<BigDecimal> netTotal() { return netTotal; }

    /** Invoice type code from {@code Fa/RodzajFaktury}. */
    public @Nullable String invoiceTypeCode() { return invoiceTypeCode; }

    /**
     * Line items mapped from {@code Fa/FaWiersz} entries to SDK
     * records. Returns an empty list when the underlying JAXB tree
     * had no line items at construction.
     */
    public List<InvoiceLineItem> lineItems() { return lineItems; }

    private static List<InvoiceLineItem> snapshotLineItems(Faktura.@Nullable Fa faContent) {
        if (faContent == null || faContent.getFaWiersz() == null) {
            return List.of();
        }
        List<InvoiceLineItem> mapped = new ArrayList<>(faContent.getFaWiersz().size());
        for (Faktura.Fa.FaWiersz wiersz : faContent.getFaWiersz()) {
            InvoiceLineItem item = mapLineItem(wiersz);
            if (item != null) {
                mapped.add(item);
            }
        }
        return List.copyOf(mapped);
    }

    private static @Nullable InvoiceLineItem mapLineItem(Faktura.Fa.FaWiersz wiersz) {
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
