/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceArchive;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.ClosedSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatExemption;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateBucket;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateSum;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TNaglowek;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot1;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot2;
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
 * Read-side FA(3) invoice fetched from KSeF. Wraps the JAXB-generated
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
 * @since 0.1.0
 */
public final class Fa3InvoiceDocument implements InvoiceDocument {

    private final Faktura faktura;
    private final byte[] xmlBytes;
    private final @Nullable String systemCode;
    private final @Nullable String formVersion;
    private final @Nullable OffsetDateTime issuedAt;
    private final @Nullable String sellerNip;
    private final @Nullable String sellerName;
    private final @Nullable String sellerEmail;
    private final @Nullable String sellerPhone;
    private final @Nullable String sellerAddressL1;
    private final @Nullable String sellerAddressL2;
    private final @Nullable String sellerCountryCode;
    private final @Nullable String buyerNip;
    private final @Nullable String buyerName;
    private final @Nullable String buyerEmail;
    private final @Nullable String buyerPhone;
    private final @Nullable String buyerAddressL1;
    private final @Nullable String buyerAddressL2;
    private final @Nullable String buyerCountryCode;
    private final boolean buyerIsJst;
    private final boolean buyerIsVatGroup;
    private final @Nullable String systemInfo;
    private final boolean splitPayment;
    private final @Nullable String invoiceNumber;
    private final @Nullable LocalDate issueDate;
    private final @Nullable String currency;
    private final @Nullable BigDecimal grossTotal;
    private final Optional<BigDecimal> netTotal;
    private final @Nullable String invoiceTypeCode;
    private final List<InvoiceLineItem> lineItems;
    private final @Nullable LocalDate deliveryDate;
    private final @Nullable LocalDate paymentDueDate;
    private final @Nullable String paymentMethodCode;
    private final @Nullable VatExemption vatExemption;
    private final List<VatRateSum> vatBreakdown;

    Fa3InvoiceDocument(Faktura faktura, byte[] xmlBytes) {
        this.faktura = Objects.requireNonNull(faktura, InvoiceDocumentMessages.ERR_NULL_FAKTURA);
        this.xmlBytes = xmlBytes.clone();
        HeaderSnapshot header = HeaderSnapshot.from(faktura.getNaglowek());
        this.systemCode = header.systemCode;
        this.formVersion = header.formVersion;
        this.issuedAt = header.issuedAt;
        PartySnapshot seller = PartySnapshot.fromSeller(faktura.getPodmiot1());
        this.sellerNip = seller.nip;
        this.sellerName = seller.name;
        this.sellerEmail = seller.email;
        this.sellerPhone = seller.phone;
        this.sellerAddressL1 = seller.addressL1;
        this.sellerAddressL2 = seller.addressL2;
        this.sellerCountryCode = seller.countryCode;
        PartySnapshot buyer = PartySnapshot.fromBuyer(faktura.getPodmiot2());
        this.buyerNip = buyer.nip;
        this.buyerName = buyer.name;
        this.buyerEmail = buyer.email;
        this.buyerPhone = buyer.phone;
        this.buyerAddressL1 = buyer.addressL1;
        this.buyerAddressL2 = buyer.addressL2;
        this.buyerCountryCode = buyer.countryCode;
        this.buyerIsJst = buyer.jst;
        this.buyerIsVatGroup = buyer.vatGroup;
        this.systemInfo = faktura.getNaglowek() != null ? faktura.getNaglowek().getSystemInfo() : null;
        this.splitPayment = extractSplitPayment(faktura.getFa());
        FaSnapshot fa = FaSnapshot.from(faktura.getFa());
        this.invoiceNumber = fa.invoiceNumber;
        this.issueDate = fa.issueDate;
        this.currency = fa.currency;
        this.grossTotal = fa.grossTotal;
        this.netTotal = fa.netTotal;
        this.invoiceTypeCode = fa.invoiceTypeCode;
        this.lineItems = fa.lineItems;
        this.deliveryDate = fa.deliveryDate;
        this.paymentDueDate = fa.paymentDueDate;
        this.paymentMethodCode = fa.paymentMethodCode;
        this.vatExemption = fa.vatExemption;
        this.vatBreakdown = fa.vatBreakdown;
    }

    /**
     * Parse FA(3) XML bytes into a typed document. The bytes are kept
     * verbatim for {@link #xml()}; the JAXB tree is unmarshalled and the
     * flat-accessor values snapshotted at construction.
     *
     * <p>Package-private — SDK creates documents from archive responses;
     * consumers read via {@link InvoiceArchive#getByKsefNumber} or the
     * {@code archive()} flow. Cross-package access by SDK internals is
     * routed through {@code InvoiceDocumentConstructor} (reflective
     * bridge mirroring {@code SessionHandleConstructor}).
     */
    static Fa3InvoiceDocument from(byte[] xml) {
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

    /**
     * Direct reference to the internal JAXB {@link Faktura} root —
     * escape-hatch for fields the flat accessors do not surface (footer,
     * advance payments, KOR_ROZ breakdowns, EU cross-border attachments).
     *
     * <p><strong>Read-only by contract.</strong> Mutations are not
     * reflected in {@link #xml()} bytes nor in the flat accessors below,
     * which snapshot at construction. For a mutable disconnected copy
     * use {@link #toJaxbCopy()}.
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

    /** Seller email from first {@code Podmiot1/DaneKontaktowe/Email} entry. */
    public @Nullable String sellerEmail() { return sellerEmail; }

    /** Seller phone from first {@code Podmiot1/DaneKontaktowe/Telefon} entry. */
    public @Nullable String sellerPhone() { return sellerPhone; }

    /** Seller address line 1 from {@code Podmiot1/Adres/AdresL1}. */
    public @Nullable String sellerAddressL1() { return sellerAddressL1; }

    /** Seller address line 2 from {@code Podmiot1/Adres/AdresL2} (optional, used for foreign addresses or lokal numbers). */
    public @Nullable String sellerAddressL2() { return sellerAddressL2; }

    /** Seller ISO 3166-1 alpha-2 country code from {@code Podmiot1/Adres/KodKraju}. */
    public @Nullable String sellerCountryCode() { return sellerCountryCode; }

    /** Buyer email from first {@code Podmiot2/DaneKontaktowe/Email} entry. */
    public @Nullable String buyerEmail() { return buyerEmail; }

    /** Buyer phone from first {@code Podmiot2/DaneKontaktowe/Telefon} entry. */
    public @Nullable String buyerPhone() { return buyerPhone; }

    /** Buyer address line 1 from {@code Podmiot2/Adres/AdresL1}. */
    public @Nullable String buyerAddressL1() { return buyerAddressL1; }

    /** Buyer address line 2 from {@code Podmiot2/Adres/AdresL2}. */
    public @Nullable String buyerAddressL2() { return buyerAddressL2; }

    /** Buyer ISO 3166-1 alpha-2 country code from {@code Podmiot2/Adres/KodKraju}. */
    public @Nullable String buyerCountryCode() { return buyerCountryCode; }

    /** Buyer is a sub-unit of a JST (Polish local-government unit) — {@code Podmiot2/JST = 1}. */
    public boolean buyerIsJst() { return buyerIsJst; }

    /** Buyer is a member of a VAT group — {@code Podmiot2/GV = 1}. */
    public boolean buyerIsVatGroup() { return buyerIsVatGroup; }

    /** Issuing-system identifier from {@code Naglowek/SystemInfo} — present on most invoices from ERP systems. */
    public @Nullable String systemInfo() { return systemInfo; }

    /** Split-payment / MPP flag from {@code Fa/Adnotacje/P_18A = 1}. */
    public boolean splitPayment() { return splitPayment; }

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

    /** Delivery / service-completion date from {@code Fa/P_6}. Populated only when different from {@link #issueDate()}. */
    public @Nullable LocalDate deliveryDate() { return deliveryDate; }

    /** First {@code Fa/Platnosc/TerminPlatnosci/Termin} entry (multi-term schedules collapse to the first). */
    public @Nullable LocalDate paymentDueDate() { return paymentDueDate; }

    /** Payment-method code from {@code Fa/Platnosc/FormaPlatnosci} as a stable string (e.g. "1" cash, "2" card, "6" transfer). */
    public @Nullable String paymentMethodCode() { return paymentMethodCode; }

    /** VAT exemption basis from {@code Fa/Adnotacje/Zwolnienie}. Null when the invoice is not exempt. */
    public @Nullable VatExemption vatExemption() { return vatExemption; }

    /** VAT-rate breakdown summed from {@code Fa/P_13_x} + {@code Fa/P_14_x}. Empty list when none of the buckets has a non-null net amount. */
    public List<VatRateSum> vatBreakdown() { return vatBreakdown; }

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

    private record PartySnapshot(@Nullable String nip,
                                 @Nullable String name,
                                 @Nullable String email,
                                 @Nullable String phone,
                                 @Nullable String addressL1,
                                 @Nullable String addressL2,
                                 @Nullable String countryCode,
                                 boolean jst,
                                 boolean vatGroup) {
        static PartySnapshot fromSeller(Faktura.@Nullable Podmiot1 podmiot) {
            TPodmiot1 identity = podmiot != null ? podmiot.getDaneIdentyfikacyjne() : null;
            TAdresFa3 adres = podmiot != null ? podmiot.getAdres() : null;
            String email = null, phone = null;
            if (podmiot != null && podmiot.getDaneKontaktowe() != null && !podmiot.getDaneKontaktowe().isEmpty()) {
                var first = podmiot.getDaneKontaktowe().get(0);
                email = first.getEmail();
                phone = first.getTelefon();
            }
            return new PartySnapshot(
                    identity != null ? identity.getNIP() : null,
                    identity != null ? identity.getNazwa() : null,
                    email, phone,
                    adres != null ? adres.getAdresL1() : null,
                    adres != null ? adres.getAdresL2() : null,
                    adres != null && adres.getKodKraju() != null ? adres.getKodKraju().value() : null,
                    false, false);
        }

        static PartySnapshot fromBuyer(Faktura.@Nullable Podmiot2 podmiot) {
            TPodmiot2 identity = podmiot != null ? podmiot.getDaneIdentyfikacyjne() : null;
            TAdresFa3 adres = podmiot != null ? podmiot.getAdres() : null;
            String email = null, phone = null;
            if (podmiot != null && podmiot.getDaneKontaktowe() != null && !podmiot.getDaneKontaktowe().isEmpty()) {
                var first = podmiot.getDaneKontaktowe().get(0);
                email = first.getEmail();
                phone = first.getTelefon();
            }
            boolean jst = podmiot != null && podmiot.getJST() != null && podmiot.getJST().intValue() == 1;
            boolean vatGroup = podmiot != null && podmiot.getGV() != null && podmiot.getGV().intValue() == 1;
            return new PartySnapshot(
                    identity != null ? identity.getNIP() : null,
                    identity != null ? identity.getNazwa() : null,
                    email, phone,
                    adres != null ? adres.getAdresL1() : null,
                    adres != null ? adres.getAdresL2() : null,
                    adres != null && adres.getKodKraju() != null ? adres.getKodKraju().value() : null,
                    jst, vatGroup);
        }
    }

    private record FaSnapshot(@Nullable String invoiceNumber,
                              @Nullable LocalDate issueDate,
                              @Nullable String currency,
                              @Nullable BigDecimal grossTotal,
                              Optional<BigDecimal> netTotal,
                              @Nullable String invoiceTypeCode,
                              List<InvoiceLineItem> lineItems,
                              @Nullable LocalDate deliveryDate,
                              @Nullable LocalDate paymentDueDate,
                              @Nullable String paymentMethodCode,
                              @Nullable VatExemption vatExemption,
                              List<VatRateSum> vatBreakdown) {
        static FaSnapshot from(Faktura.@Nullable Fa faContent) {
            if (faContent == null) {
                return new FaSnapshot(null, null, null, null, Optional.empty(), null,
                        List.of(), null, null, null, null, List.of());
            }
            return new FaSnapshot(
                    faContent.getP2(),
                    faContent.getP1() != null ? toLocalDate(faContent.getP1()) : null,
                    faContent.getKodWaluty() != null ? faContent.getKodWaluty().value() : null,
                    faContent.getP15(),
                    Optional.ofNullable(faContent.getP131()),
                    faContent.getRodzajFaktury() != null ? faContent.getRodzajFaktury().value() : null,
                    snapshotLineItems(faContent),
                    faContent.getP6() != null ? toLocalDate(faContent.getP6()) : null,
                    extractPaymentDueDate(faContent.getPlatnosc()),
                    extractPaymentMethodCode(faContent.getPlatnosc()),
                    extractVatExemption(faContent.getAdnotacje()),
                    extractVatBreakdown(faContent));
        }
    }

    private static @Nullable LocalDate extractPaymentDueDate(Faktura.Fa.@Nullable Platnosc platnosc) {
        if (platnosc == null || platnosc.getTerminPlatnosci() == null || platnosc.getTerminPlatnosci().isEmpty()) {
            return null;
        }
        var first = platnosc.getTerminPlatnosci().get(0);
        return first.getTermin() != null ? toLocalDate(first.getTermin()) : null;
    }

    private static @Nullable String extractPaymentMethodCode(Faktura.Fa.@Nullable Platnosc platnosc) {
        if (platnosc == null || platnosc.getFormaPlatnosci() == null) {
            return null;
        }
        return platnosc.getFormaPlatnosci().toString();
    }

    private static boolean extractSplitPayment(Faktura.@Nullable Fa fa) {
        if (fa == null || fa.getAdnotacje() == null) {
            return false;
        }
        return fa.getAdnotacje().getP18A() == 1;
    }

    private static @Nullable VatExemption extractVatExemption(Faktura.Fa.@Nullable Adnotacje adnotacje) {
        if (adnotacje == null || adnotacje.getZwolnienie() == null) {
            return null;
        }
        var z = adnotacje.getZwolnienie();
        if (z.getP19A() == null && z.getP19B() == null && z.getP19C() == null) {
            return null;
        }
        return new VatExemption(z.getP19A(), z.getP19B(), z.getP19C());
    }

    private static List<VatRateSum> extractVatBreakdown(Faktura.Fa fa) {
        List<VatRateSum> out = new ArrayList<>(10);
        addBucket(out, VatRateBucket.STANDARD, fa.getP131(), fa.getP141());
        addBucket(out, VatRateBucket.REDUCED_FIRST, fa.getP132(), fa.getP142());
        addBucket(out, VatRateBucket.REDUCED_SECOND, fa.getP133(), fa.getP143());
        addBucket(out, VatRateBucket.TAXI_LUMP_SUM, fa.getP134(), fa.getP144());
        addBucket(out, VatRateBucket.SPECIAL_PROCEDURE, fa.getP135(), fa.getP145());
        addBucket(out, VatRateBucket.EXEMPT, fa.getP137(), null);
        addBucket(out, VatRateBucket.OUTSIDE_TERRITORY, fa.getP138(), null);
        addBucket(out, VatRateBucket.INTRA_EU_SERVICES, fa.getP139(), null);
        addBucket(out, VatRateBucket.REVERSE_CHARGE, fa.getP1310(), null);
        addBucket(out, VatRateBucket.MARGIN_SCHEME, fa.getP1311(), null);
        return List.copyOf(out);
    }

    private static void addBucket(List<VatRateSum> out, VatRateBucket bucket,
                                  @Nullable BigDecimal net, @Nullable BigDecimal vat) {
        if (net != null) {
            out.add(new VatRateSum(bucket, net, vat));
        }
    }

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
                wiersz.getGTIN(),
                wiersz.getPKWiU(),
                wiersz.getP8A(),
                wiersz.getP8B(),
                wiersz.getP9A(),
                wiersz.getP11(),
                wiersz.getP12(),
                wiersz.getP11A(),
                wiersz.getP11Vat());
    }

    private static OffsetDateTime toOffsetDateTime(XMLGregorianCalendar gregorian) {
        return gregorian.toGregorianCalendar().toZonedDateTime()
                .withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private static LocalDate toLocalDate(XMLGregorianCalendar gregorian) {
        return LocalDate.of(gregorian.getYear(), gregorian.getMonth(), gregorian.getDay());
    }
}
