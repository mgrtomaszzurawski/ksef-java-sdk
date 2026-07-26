/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceArchive;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.ClosedSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BankAccount;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.EarlyPaymentDiscount;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePayment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceSettlement;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartialPayment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PaymentTerm;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SettlementItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatExemption;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateBucket;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateSum;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TAdresFa3;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TNaglowek;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot1;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TPodmiot2;
import io.github.mgrtomaszzurawski.ksef.xml.fa3.TRachunekBankowy;
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
// Deliberate flat-accessor facade (ADR-030): one public getter per invoice
// field is the design, so the public-member count is expected to be high.
@SuppressWarnings("PMD.ExcessivePublicCount")
public final class Fa3InvoiceDocument implements InvoiceDocument {

    /** KSeF boolean marker: 1 = yes/true (2 = no/false; absent = not set). */
    private static final int KSEF_TRUE_MARKER = 1;

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
    private final @Nullable InvoicePayment payment;
    private final @Nullable InvoiceSettlement settlement;
    private final @Nullable VatExemption vatExemption;
    private final List<VatRateSum> vatBreakdown;
    private final List<InvoiceCorrectionReference> correctedInvoices;
    private final @Nullable String correctionReason;
    private final @Nullable Integer correctionType;
    private final @Nullable String correctedPeriod;
    private final @Nullable String issueLocality;
    private final @Nullable BigDecimal taxExchangeRate;
    private final @Nullable BigDecimal grossTotalBeforeCorrection;
    private final @Nullable BigDecimal taxExchangeRateBeforeCorrection;
    private final @Nullable String correctedInvoiceNumberReplacement;
    private final @Nullable Boolean issuedToReceipt;
    private final @Nullable Boolean relatedParty;
    private final @Nullable Boolean exciseDutyRefund;

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
        this.payment = fa.payment;
        this.settlement = fa.settlement;
        this.vatExemption = fa.vatExemption;
        this.vatBreakdown = fa.vatBreakdown;
        this.correctedInvoices = fa.correctedInvoices;
        this.correctionReason = fa.correctionReason;
        this.correctionType = fa.correctionType;
        this.correctedPeriod = fa.correctedPeriod;
        this.issueLocality = fa.issueLocality;
        this.taxExchangeRate = fa.taxExchangeRate;
        this.grossTotalBeforeCorrection = fa.grossTotalBeforeCorrection;
        this.taxExchangeRateBeforeCorrection = fa.taxExchangeRateBeforeCorrection;
        this.correctedInvoiceNumberReplacement = fa.correctedInvoiceNumberReplacement;
        this.issuedToReceipt = fa.issuedToReceipt;
        this.relatedParty = fa.relatedParty;
        this.exciseDutyRefund = fa.exciseDutyRefund;
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

    /** Payment terms from {@code Fa/Platnosc} — due dates, method, bank accounts, installments, skonto. Null when the invoice carries no payment block. */
    public @Nullable InvoicePayment payment() { return payment; }

    /** Additional settlements from {@code Fa/Rozliczenie} — charges, deductions, amount payable or overpayment. Null when absent. */
    public @Nullable InvoiceSettlement settlement() { return settlement; }

    /** VAT exemption basis from {@code Fa/Adnotacje/Zwolnienie}. Null when the invoice is not exempt. */
    public @Nullable VatExemption vatExemption() { return vatExemption; }

    /** VAT-rate breakdown summed from {@code Fa/P_13_x} + {@code Fa/P_14_x}. Empty list when none of the buckets has a non-null net amount. */
    public List<VatRateSum> vatBreakdown() { return vatBreakdown; }

    /**
     * Corrected-invoice references from {@code Fa/DaneFaKorygowanej}.
     * Non-empty only on a correction ({@code RodzajFaktury} KOR / KOR_ZAL
     * / KOR_ROZ); empty on an original invoice.
     */
    public List<InvoiceCorrectionReference> correctedInvoices() { return correctedInvoices; }

    /** Correction reason from {@code Fa/PrzyczynaKorekty}. Null on an original invoice. */
    public @Nullable String correctionReason() { return correctionReason; }

    /** Correction type from {@code Fa/TypKorekty} (1/2/3). Null on an original invoice. */
    public @Nullable Integer correctionType() { return correctionType; }

    /** Corrected accounting period from {@code Fa/OkresFaKorygowanej}. Null when not supplied. */
    public @Nullable String correctedPeriod() { return correctedPeriod; }

    /** Place of issue from {@code Fa/P_1M}. Null when not supplied. */
    public @Nullable String issueLocality() { return issueLocality; }

    /** Currency exchange rate used to compute VAT from {@code Fa/KursWalutyZ} (foreign-currency invoices). Null when in PLN or not supplied. */
    public @Nullable BigDecimal taxExchangeRate() { return taxExchangeRate; }

    /** Gross total before correction from {@code Fa/P_15ZK} (advance-payment corrections). Null on an original invoice. */
    public @Nullable BigDecimal grossTotalBeforeCorrection() { return grossTotalBeforeCorrection; }

    /** Currency exchange rate used to compute VAT before correction from {@code Fa/KursWalutyZK}. Null when not supplied. */
    public @Nullable BigDecimal taxExchangeRateBeforeCorrection() { return taxExchangeRateBeforeCorrection; }

    /** Replacement invoice number from {@code Fa/NrFaKorygowany} — the correct number when a correction fixes a wrong corrected-invoice number. Null otherwise. */
    public @Nullable String correctedInvoiceNumberReplacement() { return correctedInvoiceNumberReplacement; }

    /** Receipt-linked invoice marker from {@code Fa/FP = 1} (art. 109 ust. 3d). Null when not flagged. */
    public @Nullable Boolean issuedToReceipt() { return issuedToReceipt; }

    /** Related-party transaction marker from {@code Fa/TP = 1} (existing links between buyer and seller). Null when not flagged. */
    public @Nullable Boolean relatedParty() { return relatedParty; }

    /** Excise-duty refund marker from {@code Fa/ZwrotAkcyzy = 1} (fuel excise refund for farmers). Null when not flagged. */
    public @Nullable Boolean exciseDutyRefund() { return exciseDutyRefund; }

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
            String email = null;
            String phone = null;
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
            String email = null;
            String phone = null;
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
                              @Nullable InvoicePayment payment,
                              @Nullable InvoiceSettlement settlement,
                              @Nullable VatExemption vatExemption,
                              List<VatRateSum> vatBreakdown,
                              List<InvoiceCorrectionReference> correctedInvoices,
                              @Nullable String correctionReason,
                              @Nullable Integer correctionType,
                              @Nullable String correctedPeriod,
                              @Nullable String issueLocality,
                              @Nullable BigDecimal taxExchangeRate,
                              @Nullable BigDecimal grossTotalBeforeCorrection,
                              @Nullable BigDecimal taxExchangeRateBeforeCorrection,
                              @Nullable String correctedInvoiceNumberReplacement,
                              @Nullable Boolean issuedToReceipt,
                              @Nullable Boolean relatedParty,
                              @Nullable Boolean exciseDutyRefund) {
        static FaSnapshot from(Faktura.@Nullable Fa faContent) {
            if (faContent == null) {
                return new FaSnapshot(null, null, null, null, Optional.empty(), null,
                        List.of(), null, null, null, null, List.of(),
                        List.of(), null, null, null,
                        null, null, null, null, null, null, null, null);
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
                    extractPayment(faContent.getPlatnosc()),
                    extractSettlement(faContent.getRozliczenie()),
                    extractVatExemption(faContent.getAdnotacje()),
                    extractVatBreakdown(faContent),
                    extractCorrectedInvoices(faContent),
                    faContent.getPrzyczynaKorekty(),
                    faContent.getTypKorekty() != null ? faContent.getTypKorekty().intValue() : null,
                    faContent.getOkresFaKorygowanej(),
                    faContent.getP1M(),
                    faContent.getKursWalutyZ(),
                    faContent.getP15ZK(),
                    faContent.getKursWalutyZK(),
                    faContent.getNrFaKorygowany(),
                    toBooleanFlag(faContent.getFP()),
                    toBooleanFlag(faContent.getTP()),
                    toBooleanFlag(faContent.getZwrotAkcyzy()));
        }
    }

    private static List<InvoiceCorrectionReference> extractCorrectedInvoices(Faktura.Fa faContent) {
        if (faContent.getDaneFaKorygowanej() == null || faContent.getDaneFaKorygowanej().isEmpty()) {
            return List.of();
        }
        List<InvoiceCorrectionReference> refs = new ArrayList<>(faContent.getDaneFaKorygowanej().size());
        for (Faktura.Fa.DaneFaKorygowanej dane : faContent.getDaneFaKorygowanej()) {
            if (dane == null) {
                continue;
            }
            // NrFaKorygowanej and DataWystFaKorygowanej are minOccurs=1 in the
            // DaneFaKorygowanej XSD, so they are trusted non-null; a contract
            // violation surfaces loudly rather than being masked.
            refs.add(new InvoiceCorrectionReference(
                    dane.getNrFaKorygowanej(),
                    toLocalDate(dane.getDataWystFaKorygowanej()),
                    dane.getNrKSeFFaKorygowanej()));
        }
        return List.copyOf(refs);
    }

    private static @Nullable InvoicePayment extractPayment(Faktura.Fa.@Nullable Platnosc platnosc) {
        if (platnosc == null) {
            return null;
        }
        return new InvoicePayment(
                toBooleanFlag(platnosc.getZaplacono()),
                platnosc.getDataZaplaty() != null ? toLocalDate(platnosc.getDataZaplaty()) : null,
                platnosc.getZnacznikZaplatyCzesciowej() != null
                        ? platnosc.getZnacznikZaplatyCzesciowej().intValue() : null,
                platnosc.getFormaPlatnosci() != null ? platnosc.getFormaPlatnosci().toString() : null,
                toBooleanFlag(platnosc.getPlatnoscInna()),
                platnosc.getOpisPlatnosci(),
                platnosc.getLinkDoPlatnosci(),
                platnosc.getIPKSeF(),
                extractPaymentTerms(platnosc),
                extractPartialPayments(platnosc),
                extractBankAccounts(platnosc.getRachunekBankowy()),
                extractBankAccounts(platnosc.getRachunekBankowyFaktora()),
                extractSkonto(platnosc.getSkonto()));
    }

    private static List<PaymentTerm> extractPaymentTerms(Faktura.Fa.Platnosc platnosc) {
        if (platnosc.getTerminPlatnosci() == null || platnosc.getTerminPlatnosci().isEmpty()) {
            return List.of();
        }
        List<PaymentTerm> out = new ArrayList<>(platnosc.getTerminPlatnosci().size());
        for (Faktura.Fa.Platnosc.TerminPlatnosci term : platnosc.getTerminPlatnosci()) {
            if (term == null) {
                continue;
            }
            Faktura.Fa.Platnosc.TerminPlatnosci.TerminOpis opis = term.getTerminOpis();
            out.add(new PaymentTerm(
                    term.getTermin() != null ? toLocalDate(term.getTermin()) : null,
                    null,
                    opis != null && opis.getIlosc() != null ? opis.getIlosc().intValue() : null,
                    opis != null ? opis.getJednostka() : null,
                    opis != null ? opis.getZdarzeniePoczatkowe() : null));
        }
        return List.copyOf(out);
    }

    private static List<PartialPayment> extractPartialPayments(Faktura.Fa.Platnosc platnosc) {
        if (platnosc.getZaplataCzesciowa() == null || platnosc.getZaplataCzesciowa().isEmpty()) {
            return List.of();
        }
        List<PartialPayment> out = new ArrayList<>(platnosc.getZaplataCzesciowa().size());
        for (Faktura.Fa.Platnosc.ZaplataCzesciowa part : platnosc.getZaplataCzesciowa()) {
            if (part == null) {
                continue;
            }
            out.add(new PartialPayment(
                    part.getKwotaZaplatyCzesciowej(),
                    part.getDataZaplatyCzesciowej() != null ? toLocalDate(part.getDataZaplatyCzesciowej()) : null,
                    part.getFormaPlatnosci() != null ? part.getFormaPlatnosci().toString() : null,
                    toBooleanFlag(part.getPlatnoscInna()),
                    part.getOpisPlatnosci()));
        }
        return List.copyOf(out);
    }

    private static List<BankAccount> extractBankAccounts(@Nullable List<TRachunekBankowy> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }
        List<BankAccount> out = new ArrayList<>(accounts.size());
        for (TRachunekBankowy account : accounts) {
            if (account == null) {
                continue;
            }
            out.add(new BankAccount(
                    account.getNrRB(),
                    account.getSWIFT(),
                    account.getRachunekWlasnyBanku() != null ? account.getRachunekWlasnyBanku().intValue() : null,
                    account.getNazwaBanku(),
                    account.getOpisRachunku()));
        }
        return List.copyOf(out);
    }

    private static @Nullable EarlyPaymentDiscount extractSkonto(Faktura.Fa.Platnosc.@Nullable Skonto skonto) {
        if (skonto == null) {
            return null;
        }
        // WarunkiSkonta and WysokoscSkonta are minOccurs=1 within Skonto, so
        // they are trusted non-null; a contract violation surfaces loudly.
        return new EarlyPaymentDiscount(skonto.getWarunkiSkonta(), skonto.getWysokoscSkonta());
    }

    private static @Nullable InvoiceSettlement extractSettlement(Faktura.Fa.@Nullable Rozliczenie rozliczenie) {
        if (rozliczenie == null) {
            return null;
        }
        return new InvoiceSettlement(
                extractCharges(rozliczenie.getObciazenia()),
                rozliczenie.getSumaObciazen(),
                extractDeductions(rozliczenie.getOdliczenia()),
                rozliczenie.getSumaOdliczen(),
                rozliczenie.getDoZaplaty(),
                rozliczenie.getDoRozliczenia());
    }

    private static List<SettlementItem> extractCharges(@Nullable List<Faktura.Fa.Rozliczenie.Obciazenia> charges) {
        if (charges == null || charges.isEmpty()) {
            return List.of();
        }
        List<SettlementItem> out = new ArrayList<>(charges.size());
        for (Faktura.Fa.Rozliczenie.Obciazenia charge : charges) {
            if (charge != null) {
                // Kwota and Powod are minOccurs=1; trusted non-null.
                out.add(new SettlementItem(charge.getKwota(), charge.getPowod()));
            }
        }
        return List.copyOf(out);
    }

    private static List<SettlementItem> extractDeductions(
            @Nullable List<Faktura.Fa.Rozliczenie.Odliczenia> deductions) {
        if (deductions == null || deductions.isEmpty()) {
            return List.of();
        }
        List<SettlementItem> out = new ArrayList<>(deductions.size());
        for (Faktura.Fa.Rozliczenie.Odliczenia deduction : deductions) {
            if (deduction != null) {
                out.add(new SettlementItem(deduction.getKwota(), deduction.getPowod()));
            }
        }
        return List.copyOf(out);
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
        var zwolnienie = adnotacje.getZwolnienie();
        if (zwolnienie.getP19A() == null && zwolnienie.getP19B() == null && zwolnienie.getP19C() == null) {
            return null;
        }
        return new VatExemption(zwolnienie.getP19A(), zwolnienie.getP19B(), zwolnienie.getP19C());
    }

    private static List<VatRateSum> extractVatBreakdown(Faktura.Fa fa) {
        List<VatRateSum> out = new ArrayList<>(13);
        addBucket(out, VatRateBucket.STANDARD, fa.getP131(), fa.getP141(), fa.getP141W());
        addBucket(out, VatRateBucket.REDUCED_FIRST, fa.getP132(), fa.getP142(), fa.getP142W());
        addBucket(out, VatRateBucket.REDUCED_SECOND, fa.getP133(), fa.getP143(), fa.getP143W());
        addBucket(out, VatRateBucket.TAXI_LUMP_SUM, fa.getP134(), fa.getP144(), fa.getP144W());
        addBucket(out, VatRateBucket.SPECIAL_PROCEDURE, fa.getP135(), fa.getP145(), null);
        addBucket(out, VatRateBucket.ZERO_RATE_DOMESTIC, fa.getP1361(), null, null);
        addBucket(out, VatRateBucket.ZERO_RATE_INTRA_EU, fa.getP1362(), null, null);
        addBucket(out, VatRateBucket.ZERO_RATE_EXPORT, fa.getP1363(), null, null);
        addBucket(out, VatRateBucket.EXEMPT, fa.getP137(), null, null);
        addBucket(out, VatRateBucket.OUTSIDE_TERRITORY, fa.getP138(), null, null);
        addBucket(out, VatRateBucket.INTRA_EU_SERVICES, fa.getP139(), null, null);
        addBucket(out, VatRateBucket.REVERSE_CHARGE, fa.getP1310(), null, null);
        addBucket(out, VatRateBucket.MARGIN_SCHEME, fa.getP1311(), null, null);
        return List.copyOf(out);
    }

    private static void addBucket(List<VatRateSum> out, VatRateBucket bucket, @Nullable BigDecimal netAmount,
                                  @Nullable BigDecimal vatAmount, @Nullable BigDecimal vatAmountConvertedToPln) {
        if (netAmount != null) {
            out.add(new VatRateSum(bucket, netAmount, vatAmount, vatAmountConvertedToPln));
        }
    }

    private static List<InvoiceLineItem> snapshotLineItems(Faktura.@Nullable Fa faContent) {
        if (faContent == null || faContent.getFaWiersz() == null) {
            return List.of();
        }
        List<InvoiceLineItem> mapped = new ArrayList<>(faContent.getFaWiersz().size());
        for (Faktura.Fa.FaWiersz wiersz : faContent.getFaWiersz()) {
            if (wiersz != null) {
                mapped.add(mapLineItem(wiersz));
            }
        }
        return List.copyOf(mapped);
    }

    // Every FaWiersz maps to exactly one line item. P_7, P_11, P_11A,
    // P_11Vat and P_12 are all minOccurs="0" in the FA(3) XSD, so any of
    // them may be null (e.g. a gross-only line carries P_11A without
    // P_11). Never drop a line — that would silently lose invoice data.
    private static InvoiceLineItem mapLineItem(Faktura.Fa.FaWiersz wiersz) {
        int rowNumber = wiersz.getNrWierszaFa() != null ? wiersz.getNrWierszaFa().intValue() : 1;
        return InvoiceLineItem.builder()
                .rowNumber(rowNumber)
                .description(wiersz.getP7())
                .gtin(wiersz.getGTIN())
                .pkwiu(wiersz.getPKWiU())
                .unitOfMeasure(wiersz.getP8A())
                .quantity(wiersz.getP8B())
                .netUnitPrice(wiersz.getP9A())
                .netAmount(wiersz.getP11())
                .vatRate(wiersz.getP12())
                .grossAmount(wiersz.getP11A())
                .vatAmount(wiersz.getP11Vat())
                .deliveryDate(wiersz.getP6A() != null ? toLocalDate(wiersz.getP6A()) : null)
                .uuid(wiersz.getUUID())
                .index(wiersz.getIndeks())
                .cnCode(wiersz.getCN())
                .pkobCode(wiersz.getPKOB())
                .grossUnitPrice(wiersz.getP9B())
                .discountAmount(wiersz.getP10())
                .exciseAmount(wiersz.getKwotaAkcyzy())
                .exchangeRate(wiersz.getKursWaluty())
                .valueAddedTaxRate(wiersz.getP12XII())
                .annex15(toBooleanFlag(wiersz.getP12Zal15()))
                .correctionStateBefore(toBooleanFlag(wiersz.getStanPrzed()))
                .gtuCode(wiersz.getGTU() != null ? wiersz.getGTU().value() : null)
                .procedureMarking(wiersz.getProcedura() != null ? wiersz.getProcedura().value() : null)
                .build();
    }

    private static @Nullable Boolean toBooleanFlag(@Nullable Byte marker) {
        return marker != null ? marker == KSEF_TRUE_MARKER : null;
    }

    private static OffsetDateTime toOffsetDateTime(XMLGregorianCalendar gregorian) {
        return gregorian.toGregorianCalendar().toZonedDateTime()
                .withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private static LocalDate toLocalDate(XMLGregorianCalendar gregorian) {
        return LocalDate.of(gregorian.getYear(), gregorian.getMonth(), gregorian.getDay());
    }
}
