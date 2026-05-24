/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatExemption;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodFormularza;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodKraju;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TKodWaluty;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TNaglowek;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot1;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot2;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TRodzajFaktury;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jspecify.annotations.Nullable;

/**
 * Typed FA(3) invoice: wraps the JAXB-generated {@link Faktura} root
 * and exposes its content as flat primitive accessors alongside the
 * {@link Invoice} contract ({@link #formCode()}, {@link #xml()}).
 *
 * <p>Construct via {@link #builder()}. The builder mirrors the
 * minimum-viable subset of the FA(3) XSD — header, seller, buyer,
 * line items, currency, totals, correction reference.
 *
 * <p><strong>Escape hatches.</strong> Rare fields (advance payments,
 * tourism margin, KOR_ROZ breakdowns, EU cross-border attachments) are
 * reachable through two methods:
 *
 * <ul>
 *   <li>{@link #unsafeJaxbView()} — direct reference to the internal
 *       JAXB root. <em>Read-only</em>: mutations are not reflected in
 *       the {@link #xml()} bytes nor in the flat accessors (which
 *       snapshot at construction).</li>
 *   <li>{@link #toJaxbCopy()} — deep clone. Mutable but disconnected
 *       from this invoice's state.</li>
 * </ul>
 *
 * <p>For build-time customisation use
 * {@link Builder#customizeJaxb(Consumer)} — the consumer runs on the
 * assembled tree before XML marshalling, so its effects are captured
 * in the resulting bytes and flat accessors.
 *
 * @since 0.1.0
 */
public final class Fa3Invoice implements Invoice {

    /** Wire-level XSD namespace for FA(3). Effective 2025-06-25. */
    static final String FA3_NAMESPACE = "http://crd.gov.pl/wzor/2025/06/25/13775/";
    /** Schema version FA(3). */
    static final byte FA3_WARIANT_FORMULARZA = 3;
    /** {@code KodFormularza/@kodSystemowy} — wire systemCode for FA(3). */
    static final String FA3_KOD_SYSTEMOWY = "FA (3)";
    /** {@code KodFormularza/@wersjaSchemy} — XSD version for FA(3). */
    static final String FA3_WERSJA_SCHEMY = "1-0E";

    private static final String ERR_NULL_FAKTURA = "faktura must not be null";

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

    Fa3Invoice(Faktura faktura, byte[] xmlBytes) {
        this.faktura = Objects.requireNonNull(faktura, ERR_NULL_FAKTURA);
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
     * which snapshot at construction. For a mutable disconnected copy use
     * {@link #toJaxbCopy()}; for build-time customisation use
     * {@link Builder#customizeJaxb(Consumer)}.
     */
    public Faktura unsafeJaxbView() {
        return faktura;
    }

    /**
     * Deep-clone of the internal JAXB tree via a marshal/unmarshal
     * round-trip. The returned object is mutable but shares no
     * references with this invoice — neither {@link #xml()} bytes nor
     * the flat accessors observe mutations to the clone.
     */
    public Faktura toJaxbCopy() {
        return JaxbDeepClone.clone(faktura, Faktura.class);
    }

    /** Form-systemCode token from {@code Naglowek/KodFormularza/@kodSystemowy}. */
    public @Nullable String systemCode() {
        return systemCode;
    }

    /** Schema version token from {@code Naglowek/KodFormularza/@wersjaSchemy}. */
    public @Nullable String formVersion() {
        return formVersion;
    }

    /** Issue timestamp from {@code Naglowek/DataWytworzeniaFa}. */
    public @Nullable OffsetDateTime issuedAt() {
        return issuedAt;
    }

    /** Seller NIP from {@code Podmiot1/DaneIdentyfikacyjne/NIP}. */
    public @Nullable String sellerNip() {
        return sellerNip;
    }

    /** Seller name from {@code Podmiot1/DaneIdentyfikacyjne/Nazwa}. */
    public @Nullable String sellerName() {
        return sellerName;
    }

    /** Buyer NIP from {@code Podmiot2/DaneIdentyfikacyjne/NIP}. */
    public @Nullable String buyerNip() {
        return buyerNip;
    }

    /** Buyer name from {@code Podmiot2/DaneIdentyfikacyjne/Nazwa}. */
    public @Nullable String buyerName() {
        return buyerName;
    }

    /** Invoice number from {@code Fa/P_2}. */
    public @Nullable String invoiceNumber() {
        return invoiceNumber;
    }

    /** Issue date from {@code Fa/P_1}. */
    public @Nullable LocalDate issueDate() {
        return issueDate;
    }

    /** ISO 4217 currency code from {@code Fa/KodWaluty}. */
    public @Nullable String currency() {
        return currency;
    }

    /** Gross total from {@code Fa/P_15}. */
    public @Nullable BigDecimal grossTotal() {
        return grossTotal;
    }

    /** Optional net total from {@code Fa/P_13_1}. */
    public Optional<BigDecimal> netTotal() {
        return netTotal;
    }

    /** Invoice type code from {@code Fa/RodzajFaktury}. */
    public @Nullable String invoiceTypeCode() {
        return invoiceTypeCode;
    }

    /**
     * Line items mapped from {@code Fa/FaWiersz} entries to SDK
     * records. Returns an empty list when the underlying JAXB tree
     * had no line items at construction. Lines whose JAXB element
     * lacked the required fields {@code P_7}, {@code P_11} or
     * {@code P_12} were skipped.
     */
    public List<InvoiceLineItem> lineItems() {
        return lineItems;
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

    /** Begin building a new {@link Fa3Invoice}. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Fa3Invoice}. Common-cases only:
     * standard sale, services, line items with VAT, totals, and
     * (when the {@code RodzajFaktury} is a correction type) one or
     * more correction references.
     *
     * <p>For fields the builder does not surface (footer, advance
     * payments, KOR_ROZ breakdowns), supply a {@link Consumer} via
     * {@link #customizeJaxb(Consumer)}. The consumer runs on the
     * assembled JAXB root immediately before XML marshalling, so its
     * effects are captured in the resulting bytes and flat accessors.
     */
    public static final class Builder {

        /** Default invoice issuance time when caller provides {@code issueDate} only. */
        private static final String DEFAULT_ISSUE_TIME_ZONE = "+00:00";
        private static final String DEFAULT_ISSUE_TIME = "T00:00:00";
        private static final String ERR_NULL_ISSUE_DATE = "issueDate must not be null";
        private static final String ERR_NULL_INVOICE_NUMBER = "invoiceNumber must not be null";
        private static final String ERR_NULL_SELLER = "seller must not be null";
        private static final String ERR_NULL_BUYER = "buyer must not be null";
        private static final String ERR_NULL_LINE_ITEM = "lineItem must not be null";
        private static final String ERR_NULL_CURRENCY = "currency must not be null";
        private static final String ERR_NULL_TOTAL = "totalGrossAmount must not be null";
        private static final String ERR_NULL_CORRECTION_REF = "correctionReference must not be null";
        private static final String ERR_NULL_RODZAJ = "rodzajFaktury must not be null";
        private static final String ERR_NULL_CUSTOMIZER = "customizer must not be null";
        private static final String ERR_BAD_DATATYPE_FACTORY = "DatatypeFactory unavailable";
        private static final String ERR_CORRECTION_REQUIRED =
                "FA(3) correction invoices (RodzajFaktury KOR / KOR_ZAL / KOR_ROZ) require"
                        + " at least one correctionReference(...) call before build()";

        private static final byte FLAG_FALSE = 2;
        private static final byte FLAG_TRUE = 1;

        private @Nullable LocalDate issueDate;
        private @Nullable String issueLocality;
        private @Nullable String invoiceNumber;
        private TKodWaluty currency = TKodWaluty.PLN;
        private TRodzajFaktury rodzajFaktury = TRodzajFaktury.VAT;
        private @Nullable InvoiceParty seller;
        private @Nullable InvoiceParty buyer;
        private final List<InvoiceLineItem> lineItems = new ArrayList<>();
        private final List<InvoiceCorrectionReference> correctionReferences = new ArrayList<>();
        private @Nullable BigDecimal totalGrossAmount;
        private @Nullable LocalDate deliveryDate;
        private @Nullable LocalDate paymentDueDate;
        private @Nullable String paymentMethodCode;
        private @Nullable VatExemption vatExemption;
        private boolean splitPayment;
        private @Nullable String systemInfo;
        private @Nullable Consumer<Faktura> customizer;

        Builder() {
        }

        /** Issue date (P_1) — required. */
        public Builder issueDate(LocalDate value) {
            this.issueDate = Objects.requireNonNull(value, ERR_NULL_ISSUE_DATE);
            return this;
        }

        /** Place of issue (P_1M) — optional but commonly populated. */
        public Builder issueLocality(String value) {
            this.issueLocality = value;
            return this;
        }

        /** Invoice number (P_2) — required. */
        public Builder invoiceNumber(String value) {
            this.invoiceNumber = Objects.requireNonNull(value, ERR_NULL_INVOICE_NUMBER);
            return this;
        }

        /** Currency code (KodWaluty) — defaults to {@link TKodWaluty#PLN}. */
        public Builder currency(TKodWaluty value) {
            this.currency = Objects.requireNonNull(value, ERR_NULL_CURRENCY);
            return this;
        }

        /** Invoice kind (RodzajFaktury) — defaults to {@link TRodzajFaktury#VAT}. */
        public Builder rodzajFaktury(TRodzajFaktury value) {
            this.rodzajFaktury = Objects.requireNonNull(value, ERR_NULL_RODZAJ);
            return this;
        }

        /** Seller (Podmiot1) — required. */
        public Builder seller(InvoiceParty value) {
            this.seller = Objects.requireNonNull(value, ERR_NULL_SELLER);
            return this;
        }

        /** Buyer (Podmiot2) — required. */
        public Builder buyer(InvoiceParty value) {
            this.buyer = Objects.requireNonNull(value, ERR_NULL_BUYER);
            return this;
        }

        /** Append one line item to the invoice. */
        public Builder addLineItem(InvoiceLineItem value) {
            this.lineItems.add(Objects.requireNonNull(value, ERR_NULL_LINE_ITEM));
            return this;
        }

        /** Total gross amount (P_15) — required. */
        public Builder totalGrossAmount(BigDecimal value) {
            this.totalGrossAmount = Objects.requireNonNull(value, ERR_NULL_TOTAL);
            return this;
        }

        /**
         * Append a correction reference. Required when the
         * {@code RodzajFaktury} is one of {@code KOR}, {@code KOR_ZAL},
         * {@code KOR_ROZ}.
         */
        public Builder correctionReference(InvoiceCorrectionReference value) {
            this.correctionReferences.add(Objects.requireNonNull(value, ERR_NULL_CORRECTION_REF));
            return this;
        }

        /**
         * Customise the assembled JAXB tree before marshalling — escape-hatch
         * for fields the builder does not surface as typed setters
         * (advance-payment metadata, tourism margin, footer, KOR_ROZ
         * breakdowns, EU cross-border attachments).
         *
         * <p>The consumer runs after all typed setters have populated the
         * root, immediately before XML marshalling. Its effects appear in
         * both the resulting {@link #xml()} bytes and the flat accessors
         * (which snapshot at construction). Multiple {@code customizeJaxb}
         * calls compose by appending — later customisations see earlier ones.
         */
        /** Delivery / service-completion date ({@code Fa/P_6}) — optional. Populated when different from {@code issueDate}. */
        public Builder deliveryDate(LocalDate value) {
            this.deliveryDate = value;
            return this;
        }

        /** Payment due date ({@code Fa/Platnosc/TerminPlatnosci/Termin}) — optional. */
        public Builder paymentDueDate(LocalDate value) {
            this.paymentDueDate = value;
            return this;
        }

        /**
         * Payment-method code ({@code Fa/Platnosc/FormaPlatnosci}) — optional. Stable
         * string codes per spec (e.g. {@code "1"} cash, {@code "2"} card, {@code "6"}
         * transfer).
         */
        public Builder paymentMethodCode(String value) {
            this.paymentMethodCode = value;
            return this;
        }

        /**
         * VAT exemption basis ({@code Fa/Adnotacje/Zwolnienie}) — optional. When
         * set, builder emits {@code P_19A}/{@code P_19B}/{@code P_19C} per the
         * record fields and clears the default {@code P_19N=TRUE} flag.
         */
        public Builder vatExemption(VatExemption value) {
            this.vatExemption = value;
            return this;
        }

        /** Split-payment / MPP flag ({@code Fa/Adnotacje/P_18A}). When true emits 1 instead of the default 2. */
        public Builder splitPayment(boolean value) {
            this.splitPayment = value;
            return this;
        }

        /** Issuing-system identifier ({@code Naglowek/SystemInfo}) — optional. Useful for traceability across ERP / SDK versions. */
        public Builder systemInfo(String value) {
            this.systemInfo = value;
            return this;
        }

        public Builder customizeJaxb(Consumer<Faktura> jaxbCustomizer) {
            Objects.requireNonNull(jaxbCustomizer, ERR_NULL_CUSTOMIZER);
            this.customizer = this.customizer == null
                    ? jaxbCustomizer
                    : this.customizer.andThen(jaxbCustomizer);
            return this;
        }

        /** Build the typed invoice. */
        public Fa3Invoice build() {
            Objects.requireNonNull(issueDate, ERR_NULL_ISSUE_DATE);
            Objects.requireNonNull(invoiceNumber, ERR_NULL_INVOICE_NUMBER);
            Objects.requireNonNull(seller, ERR_NULL_SELLER);
            Objects.requireNonNull(buyer, ERR_NULL_BUYER);
            Objects.requireNonNull(totalGrossAmount, ERR_NULL_TOTAL);
            if (isCorrectionKind(rodzajFaktury) && correctionReferences.isEmpty()) {
                throw new IllegalStateException(ERR_CORRECTION_REQUIRED);
            }
            Faktura faktura = assembleFaktura();
            if (customizer != null) {
                customizer.accept(faktura);
            }
            byte[] xml = JaxbInvoiceMarshaller.marshal(faktura, Faktura.class);
            return new Fa3Invoice(faktura, xml);
        }

        private static boolean isCorrectionKind(TRodzajFaktury kind) {
            return kind == TRodzajFaktury.KOR
                    || kind == TRodzajFaktury.KOR_ZAL
                    || kind == TRodzajFaktury.KOR_ROZ;
        }

        private Faktura assembleFaktura() {
            Faktura faktura = new Faktura();
            faktura.setNaglowek(buildHeader());
            faktura.setPodmiot1(buildSeller(Objects.requireNonNull(seller, ERR_NULL_SELLER)));
            faktura.setPodmiot2(buildBuyer(Objects.requireNonNull(buyer, ERR_NULL_BUYER)));
            faktura.setFa(buildFa());
            return faktura;
        }

        private TNaglowek buildHeader() {
            TNaglowek header = new TNaglowek();
            TNaglowek.KodFormularza kodFormularza = new TNaglowek.KodFormularza();
            kodFormularza.setValue(TKodFormularza.FA);
            kodFormularza.setKodSystemowy(FA3_KOD_SYSTEMOWY);
            kodFormularza.setWersjaSchemy(FA3_WERSJA_SCHEMY);
            header.setKodFormularza(kodFormularza);
            header.setWariantFormularza(FA3_WARIANT_FORMULARZA);
            header.setDataWytworzeniaFa(toGregorianDateTime(Objects.requireNonNull(issueDate, ERR_NULL_ISSUE_DATE)));
            if (systemInfo != null) {
                header.setSystemInfo(systemInfo);
            }
            return header;
        }

        private static Faktura.Podmiot1 buildSeller(InvoiceParty party) {
            Faktura.Podmiot1 podmiot = new Faktura.Podmiot1();
            TPodmiot1 identity = new TPodmiot1();
            identity.setNIP(party.nip());
            identity.setNazwa(party.name());
            podmiot.setDaneIdentyfikacyjne(identity);
            podmiot.setAdres(buildAddress(party));
            if (party.email() != null || party.phone() != null) {
                Faktura.Podmiot1.DaneKontaktowe dk = new Faktura.Podmiot1.DaneKontaktowe();
                if (party.email() != null) dk.setEmail(party.email());
                if (party.phone() != null) dk.setTelefon(party.phone());
                podmiot.getDaneKontaktowe().add(dk);
            }
            return podmiot;
        }

        /** {@code Podmiot2/JST} marker — value 1 = invoice IS for a JST sub-unit. */
        private static final BigInteger PODMIOT2_JST_YES = BigInteger.valueOf(1);
        /** {@code Podmiot2/JST} marker — value 2 = invoice is NOT for a JST sub-unit. */
        private static final BigInteger PODMIOT2_JST_NO = BigInteger.valueOf(2);
        /** {@code Podmiot2/GV} marker — value 1 = invoice IS for a VAT-group member. */
        private static final BigInteger PODMIOT2_GV_YES = BigInteger.valueOf(1);
        /** {@code Podmiot2/GV} marker — value 2 = invoice is NOT for a VAT-group member. */
        private static final BigInteger PODMIOT2_GV_NO = BigInteger.valueOf(2);

        private static Faktura.Podmiot2 buildBuyer(InvoiceParty party) {
            Faktura.Podmiot2 podmiot = new Faktura.Podmiot2();
            TPodmiot2 identity = new TPodmiot2();
            identity.setNIP(party.nip());
            identity.setNazwa(party.name());
            podmiot.setDaneIdentyfikacyjne(identity);
            podmiot.setAdres(buildAddress(party));
            podmiot.setJST(party.jst() ? PODMIOT2_JST_YES : PODMIOT2_JST_NO);
            podmiot.setGV(party.vatGroup() ? PODMIOT2_GV_YES : PODMIOT2_GV_NO);
            if (party.email() != null || party.phone() != null) {
                Faktura.Podmiot2.DaneKontaktowe dk = new Faktura.Podmiot2.DaneKontaktowe();
                if (party.email() != null) dk.setEmail(party.email());
                if (party.phone() != null) dk.setTelefon(party.phone());
                podmiot.getDaneKontaktowe().add(dk);
            }
            return podmiot;
        }

        private static TAdresFa3 buildAddress(InvoiceParty party) {
            TAdresFa3 address = new TAdresFa3();
            address.setKodKraju(TKodKraju.fromValue(party.resolvedCountryCode()));
            address.setAdresL1(buildAddressLine1(party));
            if (party.addressL2() != null) {
                address.setAdresL2(party.addressL2());
            }
            return address;
        }

        private static String buildAddressLine1(InvoiceParty party) {
            StringBuilder line = new StringBuilder();
            if (party.street() != null) {
                line.append(party.street()).append(' ');
            }
            line.append(party.houseNumber()).append(", ")
                    .append(party.postalCode()).append(' ')
                    .append(party.locality());
            return line.toString();
        }

        private Faktura.Fa buildFa() {
            Faktura.Fa faContent = new Faktura.Fa();
            faContent.setKodWaluty(currency);
            faContent.setP1(toGregorianDate(Objects.requireNonNull(issueDate, ERR_NULL_ISSUE_DATE)));
            if (issueLocality != null) {
                faContent.setP1M(issueLocality);
            }
            faContent.setP2(invoiceNumber);
            faContent.setP15(totalGrossAmount);
            faContent.setRodzajFaktury(rodzajFaktury);
            if (deliveryDate != null) {
                faContent.setP6(toGregorianDate(deliveryDate));
            }
            faContent.setAdnotacje(buildAdnotacje(vatExemption, splitPayment));
            if (paymentDueDate != null || paymentMethodCode != null) {
                faContent.setPlatnosc(buildPlatnosc(paymentDueDate, paymentMethodCode));
            }
            for (InvoiceLineItem line : lineItems) {
                faContent.getFaWiersz().add(buildLineItem(line));
            }
            for (InvoiceCorrectionReference correction : correctionReferences) {
                faContent.getDaneFaKorygowanej().add(buildCorrectionRef(correction));
            }
            return faContent;
        }

        private static Faktura.Fa.Adnotacje buildAdnotacje(
                @Nullable VatExemption exemption, boolean splitPayment) {
            Faktura.Fa.Adnotacje adnotacje = new Faktura.Fa.Adnotacje();
            adnotacje.setP16(FLAG_FALSE);
            adnotacje.setP17(FLAG_FALSE);
            adnotacje.setP18(FLAG_FALSE);
            adnotacje.setP18A(splitPayment ? FLAG_TRUE : FLAG_FALSE);
            adnotacje.setP23(FLAG_FALSE);
            Faktura.Fa.Adnotacje.Zwolnienie zwolnienie = new Faktura.Fa.Adnotacje.Zwolnienie();
            if (exemption == null) {
                zwolnienie.setP19N(FLAG_TRUE);
            } else {
                if (exemption.legalBasisArticle() != null) {
                    zwolnienie.setP19A(exemption.legalBasisArticle());
                }
                if (exemption.legalBasisDirective() != null) {
                    zwolnienie.setP19B(exemption.legalBasisDirective());
                }
                if (exemption.otherReason() != null) {
                    zwolnienie.setP19C(exemption.otherReason());
                }
            }
            adnotacje.setZwolnienie(zwolnienie);
            Faktura.Fa.Adnotacje.NoweSrodkiTransportu nowe = new Faktura.Fa.Adnotacje.NoweSrodkiTransportu();
            nowe.setP22N(FLAG_TRUE);
            adnotacje.setNoweSrodkiTransportu(nowe);
            Faktura.Fa.Adnotacje.PMarzy pMarzy = new Faktura.Fa.Adnotacje.PMarzy();
            pMarzy.setPPMarzyN(FLAG_TRUE);
            adnotacje.setPMarzy(pMarzy);
            return adnotacje;
        }

        private Faktura.Fa.Platnosc buildPlatnosc(@Nullable LocalDate dueDate, @Nullable String methodCode) {
            Faktura.Fa.Platnosc platnosc = new Faktura.Fa.Platnosc();
            if (methodCode != null) {
                platnosc.setFormaPlatnosci(new BigInteger(methodCode));
            }
            if (dueDate != null) {
                Faktura.Fa.Platnosc.TerminPlatnosci term = new Faktura.Fa.Platnosc.TerminPlatnosci();
                term.setTermin(toGregorianDate(dueDate));
                platnosc.getTerminPlatnosci().add(term);
            }
            return platnosc;
        }

        private static Faktura.Fa.FaWiersz buildLineItem(InvoiceLineItem line) {
            Faktura.Fa.FaWiersz wiersz = new Faktura.Fa.FaWiersz();
            wiersz.setNrWierszaFa(BigInteger.valueOf(line.rowNumber()));
            wiersz.setP7(line.description());
            if (line.gtin() != null) {
                wiersz.setGTIN(line.gtin());
            }
            if (line.pkwiu() != null) {
                wiersz.setPKWiU(line.pkwiu());
            }
            if (line.unitOfMeasure() != null) {
                wiersz.setP8A(line.unitOfMeasure());
            }
            if (line.quantity() != null) {
                wiersz.setP8B(line.quantity());
            }
            if (line.netUnitPrice() != null) {
                wiersz.setP9A(line.netUnitPrice());
            }
            wiersz.setP11(line.netAmount());
            wiersz.setP12(line.vatRate());
            if (line.grossAmount() != null) {
                wiersz.setP11A(line.grossAmount());
            }
            if (line.vatAmount() != null) {
                wiersz.setP11Vat(line.vatAmount());
            }
            return wiersz;
        }

        private static Faktura.Fa.DaneFaKorygowanej buildCorrectionRef(InvoiceCorrectionReference correction) {
            Faktura.Fa.DaneFaKorygowanej entry = new Faktura.Fa.DaneFaKorygowanej();
            entry.setDataWystFaKorygowanej(toGregorianDate(correction.originalInvoiceDate()));
            entry.setNrFaKorygowanej(correction.originalInvoiceNumber());
            return entry;
        }

        private static final DatatypeFactory DATATYPE_FACTORY = createDatatypeFactory();

        private static DatatypeFactory createDatatypeFactory() {
            try {
                return DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException ex) {
                throw new IllegalStateException(ERR_BAD_DATATYPE_FACTORY, ex);
            }
        }

        private static XMLGregorianCalendar toGregorianDate(LocalDate date) {
            return DATATYPE_FACTORY.newXMLGregorianCalendar(date.toString());
        }

        private static XMLGregorianCalendar toGregorianDateTime(LocalDate date) {
            return DATATYPE_FACTORY.newXMLGregorianCalendar(
                    date.toString() + DEFAULT_ISSUE_TIME + DEFAULT_ISSUE_TIME_ZONE);
        }
    }
}
