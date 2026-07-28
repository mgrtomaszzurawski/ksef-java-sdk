/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceArchive;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.ClosedSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AdditionalDescription;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AdvanceOrder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.Agreement;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.AuthorizedParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BankAccount;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.Carrier;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.CorrectionBuyer;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.CorrectionSeller;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.EarlyPaymentDiscount;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceFooter;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePayment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePeriod;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceSettlement;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.MarginScheme;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.NewMeansOfTransport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.NewTransportItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OrderLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartialPayment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartyContact;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartyIdentity;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PaymentTerm;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PurchaseOrder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.RegistryEntry;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SettlementItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ThirdParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ThirdPartyIdentity;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.TransactionConditions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.Transport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatExemption;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateBucket;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.VatRateSum;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.Faktura;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TAdresFa2;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TNaglowek;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot1;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot2;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TPodmiot3;
import io.github.mgrtomaszzurawski.ksef.xml.fa2.TRachunekBankowy;
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
 * @since 0.1.0
 */
// Deliberate flat-accessor facade (ADR-030): one public getter per invoice
// field is the design, so the public-member count is expected to be high, the
// class-total cyclomatic complexity scales with field coverage (one small
// JAXB->record mapper per subtree, each trivial), and the object coupling
// scales with the number of typed sections surfaced (one model record per
// subtree). Per-method complexity stays guarded by CognitiveComplexity; the
// class-total CyclomaticComplexity and CouplingBetweenObjects signals are a
// poor fit for this wide mapping facade, so they are suppressed here.
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects"})
public final class Fa2InvoiceDocument implements InvoiceDocument {

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
    private final @Nullable String systemInfo;
    private final boolean splitPayment;
    private final boolean cashMethod;
    private final boolean selfBilling;
    private final boolean reverseCharge;
    private final boolean simplifiedTriangular;
    private final @Nullable MarginScheme marginScheme;
    private final @Nullable NewMeansOfTransport newMeansOfTransport;
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
    private final @Nullable InvoicePeriod invoicePeriod;
    private final @Nullable TransactionConditions transactionConditions;
    private final List<String> deliveryNoteNumbers;
    private final List<AdditionalDescription> additionalDescriptions;
    private final List<ThirdParty> thirdParties;
    private final @Nullable CorrectionSeller correctionSeller;
    private final List<CorrectionBuyer> correctionBuyers;
    private final @Nullable AdvanceOrder advanceOrder;
    private final @Nullable AuthorizedParty authorizedParty;
    private final @Nullable InvoiceFooter footer;
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

    // Flat-facade constructor (ADR-030): one field assignment per surfaced
    // invoice attribute, so its statement count scales with field coverage
    // while each statement is a trivial assignment — NcssCount is a poor fit.
    @SuppressWarnings("PMD.NcssCount")
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
        this.systemInfo = faktura.getNaglowek() != null ? faktura.getNaglowek().getSystemInfo() : null;
        this.splitPayment = extractSplitPayment(faktura.getFa());
        Faktura.Fa.Adnotacje adnotacje = faktura.getFa() != null ? faktura.getFa().getAdnotacje() : null;
        this.cashMethod = adnotacje != null && adnotacje.getP16() == KSEF_TRUE_MARKER;
        this.selfBilling = adnotacje != null && adnotacje.getP17() == KSEF_TRUE_MARKER;
        this.reverseCharge = adnotacje != null && adnotacje.getP18() == KSEF_TRUE_MARKER;
        this.simplifiedTriangular = adnotacje != null && adnotacje.getP23() == KSEF_TRUE_MARKER;
        this.marginScheme = extractMarginScheme(adnotacje);
        this.newMeansOfTransport = extractNewMeansOfTransport(adnotacje);
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
        // Nested-record sections map directly here (not through FaSnapshot, which
        // holds the original flat scalars) to keep that positional record stable.
        this.invoicePeriod = extractInvoicePeriod(faktura.getFa());
        this.transactionConditions = extractTransactionConditions(faktura.getFa());
        this.deliveryNoteNumbers = extractDeliveryNoteNumbers(faktura.getFa());
        this.additionalDescriptions = extractAdditionalDescriptions(faktura.getFa());
        this.thirdParties = extractThirdParties(faktura);
        this.correctionSeller = extractCorrectionSeller(faktura.getFa());
        this.correctionBuyers = extractCorrectionBuyers(faktura.getFa());
        this.advanceOrder = extractAdvanceOrder(faktura.getFa());
        this.authorizedParty = extractAuthorizedParty(faktura);
        this.footer = extractFooter(faktura);
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
                                 @Nullable String countryCode) {
        static PartySnapshot fromSeller(Faktura.@Nullable Podmiot1 podmiot) {
            TPodmiot1 identity = podmiot != null ? podmiot.getDaneIdentyfikacyjne() : null;
            TAdresFa2 adres = podmiot != null ? podmiot.getAdres() : null;
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
                    adres != null && adres.getKodKraju() != null ? adres.getKodKraju().value() : null);
        }

        static PartySnapshot fromBuyer(Faktura.@Nullable Podmiot2 podmiot) {
            TPodmiot2 identity = podmiot != null ? podmiot.getDaneIdentyfikacyjne() : null;
            TAdresFa2 adres = podmiot != null ? podmiot.getAdres() : null;
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
                    adres != null && adres.getKodKraju() != null ? adres.getKodKraju().value() : null);
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
                // FA(2) Platnosc has no LinkDoPlatnosci / IPKSeF (FA(3) additions).
                null,
                null,
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
            // FA(2) TerminOpis is free text; the FA(3) structured triple has no
            // FA(2) equivalent, so quantity/unit/startEvent stay null.
            out.add(new PaymentTerm(
                    term.getTermin() != null ? toLocalDate(term.getTermin()) : null,
                    term.getTerminOpis(),
                    null,
                    null,
                    null));
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
            // FA(2) ZaplataCzesciowa carries only amount + date (no payment-form choice).
            out.add(new PartialPayment(
                    part.getKwotaZaplatyCzesciowej(),
                    part.getDataZaplatyCzesciowej() != null ? toLocalDate(part.getDataZaplatyCzesciowej()) : null,
                    null,
                    null,
                    null));
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

    private static @Nullable InvoicePeriod extractInvoicePeriod(Faktura.@Nullable Fa fa) {
        Faktura.Fa.OkresFa okres = fa != null ? fa.getOkresFa() : null;
        if (okres == null || okres.getP6Od() == null || okres.getP6Do() == null) {
            return null;
        }
        return new InvoicePeriod(toLocalDate(okres.getP6Od()), toLocalDate(okres.getP6Do()));
    }

    private static List<String> extractDeliveryNoteNumbers(Faktura.@Nullable Fa fa) {
        if (fa == null || fa.getWZ() == null || fa.getWZ().isEmpty()) {
            return List.of();
        }
        return List.copyOf(fa.getWZ());
    }

    private static List<AdditionalDescription> extractAdditionalDescriptions(Faktura.@Nullable Fa fa) {
        if (fa == null || fa.getDodatkowyOpis() == null || fa.getDodatkowyOpis().isEmpty()) {
            return List.of();
        }
        List<AdditionalDescription> out = new ArrayList<>(fa.getDodatkowyOpis().size());
        for (io.github.mgrtomaszzurawski.ksef.xml.fa2.TKluczWartosc entry : fa.getDodatkowyOpis()) {
            if (entry != null) {
                // Klucz and Wartosc are minOccurs=1; trusted non-null.
                out.add(new AdditionalDescription(
                        entry.getNrWiersza() != null ? entry.getNrWiersza().intValue() : null,
                        entry.getKlucz(), entry.getWartosc()));
            }
        }
        return List.copyOf(out);
    }

    private static @Nullable TransactionConditions extractTransactionConditions(Faktura.@Nullable Fa fa) {
        Faktura.Fa.WarunkiTransakcji conditions = fa != null ? fa.getWarunkiTransakcji() : null;
        if (conditions == null) {
            return null;
        }
        return new TransactionConditions(
                extractAgreements(conditions.getUmowy()),
                extractOrders(conditions.getZamowienia()),
                conditions.getNrPartiiTowaru() != null ? List.copyOf(conditions.getNrPartiiTowaru()) : List.of(),
                conditions.getWarunkiDostawy(),
                conditions.getKursUmowny(),
                conditions.getWalutaUmowna() != null ? conditions.getWalutaUmowna().value() : null,
                extractTransports(conditions.getTransport()),
                toBooleanFlag(conditions.getPodmiotPosredniczacy()));
    }

    private static List<Agreement> extractAgreements(
            @Nullable List<Faktura.Fa.WarunkiTransakcji.Umowy> agreements) {
        if (agreements == null || agreements.isEmpty()) {
            return List.of();
        }
        List<Agreement> out = new ArrayList<>(agreements.size());
        for (Faktura.Fa.WarunkiTransakcji.Umowy agreement : agreements) {
            if (agreement != null) {
                out.add(new Agreement(
                        agreement.getDataUmowy() != null ? toLocalDate(agreement.getDataUmowy()) : null,
                        agreement.getNrUmowy()));
            }
        }
        return List.copyOf(out);
    }

    private static List<PurchaseOrder> extractOrders(
            @Nullable List<Faktura.Fa.WarunkiTransakcji.Zamowienia> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<PurchaseOrder> out = new ArrayList<>(orders.size());
        for (Faktura.Fa.WarunkiTransakcji.Zamowienia order : orders) {
            if (order != null) {
                out.add(new PurchaseOrder(
                        order.getDataZamowienia() != null ? toLocalDate(order.getDataZamowienia()) : null,
                        order.getNrZamowienia()));
            }
        }
        return List.copyOf(out);
    }

    private static List<Transport> extractTransports(
            @Nullable List<Faktura.Fa.WarunkiTransakcji.Transport> transports) {
        if (transports == null || transports.isEmpty()) {
            return List.of();
        }
        List<Transport> out = new ArrayList<>(transports.size());
        for (Faktura.Fa.WarunkiTransakcji.Transport transport : transports) {
            if (transport != null) {
                out.add(mapTransport(transport));
            }
        }
        return List.copyOf(out);
    }

    private static Transport mapTransport(Faktura.Fa.WarunkiTransakcji.Transport transport) {
        return new Transport(
                transport.getRodzajTransportu() != null ? transport.getRodzajTransportu().intValue() : null,
                toBooleanFlag(transport.getTransportInny()),
                transport.getOpisInnegoTransportu(),
                extractCarrier(transport.getPrzewoznik()),
                transport.getNrZleceniaTransportu(),
                transport.getOpisLadunku() != null ? transport.getOpisLadunku().intValue() : null,
                toBooleanFlag(transport.getLadunekInny()),
                transport.getOpisInnegoLadunku(),
                transport.getJednostkaOpakowania(),
                transport.getDataGodzRozpTransportu() != null
                        ? toOffsetDateTime(transport.getDataGodzRozpTransportu()) : null,
                transport.getDataGodzZakTransportu() != null
                        ? toOffsetDateTime(transport.getDataGodzZakTransportu()) : null,
                extractAddress(transport.getWysylkaZ()),
                extractAddresses(transport.getWysylkaPrzez()),
                extractAddress(transport.getWysylkaDo()));
    }

    private static @Nullable Carrier extractCarrier(
            Faktura.Fa.WarunkiTransakcji.Transport.@Nullable Przewoznik carrier) {
        if (carrier == null) {
            return null;
        }
        // DaneIdentyfikacyjne and AdresPrzewoznika are minOccurs=1 within
        // Przewoznik, so they are trusted non-null; a contract violation
        // surfaces loudly rather than being masked.
        return new Carrier(
                mapPartyIdentity(carrier.getDaneIdentyfikacyjne()),
                toAddress(carrier.getAdresPrzewoznika()));
    }

    private static PartyIdentity mapPartyIdentity(TPodmiot2 identity) {
        return new PartyIdentity(
                identity.getNIP(),
                identity.getKodUE() != null ? identity.getKodUE().value() : null,
                identity.getNrVatUE(),
                identity.getKodKraju() != null ? identity.getKodKraju().value() : null,
                identity.getNrID(),
                toBooleanFlag(identity.getBrakID()),
                identity.getNazwa());
    }

    private static List<InvoiceAddress> extractAddresses(@Nullable List<TAdresFa2> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        List<InvoiceAddress> out = new ArrayList<>(addresses.size());
        for (TAdresFa2 address : addresses) {
            if (address != null) {
                out.add(toAddress(address));
            }
        }
        return List.copyOf(out);
    }

    private static @Nullable InvoiceAddress extractAddress(@Nullable TAdresFa2 address) {
        return address != null ? toAddress(address) : null;
    }

    private static InvoiceAddress toAddress(TAdresFa2 address) {
        // KodKraju and AdresL1 are minOccurs=1 within TAdres, so they are
        // dereferenced directly; a contract violation surfaces loudly.
        return new InvoiceAddress(
                address.getKodKraju().value(),
                address.getAdresL1(),
                address.getAdresL2(),
                address.getGLN());
    }

    private static List<ThirdParty> extractThirdParties(Faktura faktura) {
        if (faktura.getPodmiot3() == null || faktura.getPodmiot3().isEmpty()) {
            return List.of();
        }
        List<ThirdParty> out = new ArrayList<>(faktura.getPodmiot3().size());
        for (Faktura.Podmiot3 party : faktura.getPodmiot3()) {
            if (party != null) {
                out.add(mapThirdParty(party));
            }
        }
        return List.copyOf(out);
    }

    private static ThirdParty mapThirdParty(Faktura.Podmiot3 party) {
        // DaneIdentyfikacyjne is minOccurs=1; trusted non-null.
        return new ThirdParty(
                mapThirdPartyIdentity(party.getDaneIdentyfikacyjne()),
                party.getIDNabywcy(),
                party.getNrEORI(),
                extractAddress(party.getAdres()),
                extractAddress(party.getAdresKoresp()),
                extractContacts(party.getDaneKontaktowe()),
                party.getRola() != null ? party.getRola().intValue() : null,
                toBooleanFlag(party.getRolaInna()),
                party.getOpisRoli(),
                party.getUdzial(),
                party.getNrKlienta());
    }

    private static ThirdPartyIdentity mapThirdPartyIdentity(TPodmiot3 identity) {
        return new ThirdPartyIdentity(
                identity.getNIP(),
                identity.getIDWew(),
                identity.getKodUE() != null ? identity.getKodUE().value() : null,
                identity.getNrVatUE(),
                identity.getKodKraju() != null ? identity.getKodKraju().value() : null,
                identity.getNrID(),
                toBooleanFlag(identity.getBrakID()),
                identity.getNazwa());
    }

    private static List<PartyContact> extractContacts(
            @Nullable List<Faktura.Podmiot3.DaneKontaktowe> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return List.of();
        }
        List<PartyContact> out = new ArrayList<>(contacts.size());
        for (Faktura.Podmiot3.DaneKontaktowe contact : contacts) {
            if (contact != null) {
                out.add(new PartyContact(contact.getEmail(), contact.getTelefon()));
            }
        }
        return List.copyOf(out);
    }

    private static @Nullable AdvanceOrder extractAdvanceOrder(Faktura.@Nullable Fa fa) {
        Faktura.Fa.Zamowienie order = fa != null ? fa.getZamowienie() : null;
        if (order == null) {
            return null;
        }
        List<OrderLine> lines = new ArrayList<>();
        if (order.getZamowienieWiersz() != null) {
            for (Faktura.Fa.Zamowienie.ZamowienieWiersz line : order.getZamowienieWiersz()) {
                if (line != null) {
                    lines.add(mapOrderLine(line));
                }
            }
        }
        // WartoscZamowienia is minOccurs=1; trusted non-null.
        return new AdvanceOrder(order.getWartoscZamowienia(), lines);
    }

    private static OrderLine mapOrderLine(Faktura.Fa.Zamowienie.ZamowienieWiersz line) {
        int rowNumber = line.getNrWierszaZam() != null ? line.getNrWierszaZam().intValue() : 1;
        return OrderLine.builder()
                .rowNumber(rowNumber)
                .uuid(line.getUUIDZ())
                .description(line.getP7Z())
                .index(line.getIndeksZ())
                .gtin(line.getGTINZ())
                .pkwiu(line.getPKWiUZ())
                .cnCode(line.getCNZ())
                .pkobCode(line.getPKOBZ())
                .unitOfMeasure(line.getP8AZ())
                .quantity(line.getP8BZ())
                .netUnitPrice(line.getP9AZ())
                .netAmount(line.getP11NettoZ())
                .vatAmount(line.getP11VatZ())
                .vatRate(line.getP12Z())
                .valueAddedTaxRate(line.getP12ZXII())
                .annex15(toBooleanFlag(line.getP12ZZal15()))
                .gtuCode(line.getGTUZ() != null ? line.getGTUZ().value() : null)
                .procedureMarking(line.getProceduraZ() != null ? line.getProceduraZ().value() : null)
                .exciseAmount(line.getKwotaAkcyzyZ())
                .correctionStateBefore(toBooleanFlag(line.getStanPrzedZ()))
                .build();
    }

    private static @Nullable CorrectionSeller extractCorrectionSeller(Faktura.@Nullable Fa fa) {
        Faktura.Fa.Podmiot1K seller = fa != null ? fa.getPodmiot1K() : null;
        if (seller == null) {
            return null;
        }
        // DaneIdentyfikacyjne and Adres are minOccurs=1; trusted non-null.
        TPodmiot1 identity = seller.getDaneIdentyfikacyjne();
        return new CorrectionSeller(
                identity.getNIP(),
                identity.getNazwa(),
                toAddress(seller.getAdres()),
                seller.getPrefiksPodatnika() != null ? seller.getPrefiksPodatnika().value() : null);
    }

    private static List<CorrectionBuyer> extractCorrectionBuyers(Faktura.@Nullable Fa fa) {
        if (fa == null || fa.getPodmiot2K() == null || fa.getPodmiot2K().isEmpty()) {
            return List.of();
        }
        List<CorrectionBuyer> out = new ArrayList<>(fa.getPodmiot2K().size());
        for (Faktura.Fa.Podmiot2K buyer : fa.getPodmiot2K()) {
            if (buyer != null) {
                // DaneIdentyfikacyjne is minOccurs=1; trusted non-null.
                out.add(new CorrectionBuyer(
                        mapPartyIdentity(buyer.getDaneIdentyfikacyjne()),
                        extractAddress(buyer.getAdres()),
                        buyer.getIDNabywcy()));
            }
        }
        return List.copyOf(out);
    }

    private static @Nullable AuthorizedParty extractAuthorizedParty(Faktura faktura) {
        Faktura.PodmiotUpowazniony party = faktura.getPodmiotUpowazniony();
        if (party == null) {
            return null;
        }
        // DaneIdentyfikacyjne, Adres and RolaPU are minOccurs=1; trusted non-null.
        TPodmiot1 identity = party.getDaneIdentyfikacyjne();
        return new AuthorizedParty(
                identity.getNIP(),
                identity.getNazwa(),
                party.getNrEORI(),
                toAddress(party.getAdres()),
                extractAddress(party.getAdresKoresp()),
                extractAuthorizedContacts(party.getDaneKontaktowe()),
                party.getRolaPU().intValue());
    }

    private static List<PartyContact> extractAuthorizedContacts(
            @Nullable List<Faktura.PodmiotUpowazniony.DaneKontaktowe> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return List.of();
        }
        List<PartyContact> out = new ArrayList<>(contacts.size());
        for (Faktura.PodmiotUpowazniony.DaneKontaktowe contact : contacts) {
            if (contact != null) {
                out.add(new PartyContact(contact.getEmailPU(), contact.getTelefonPU()));
            }
        }
        return List.copyOf(out);
    }

    private static @Nullable InvoiceFooter extractFooter(Faktura faktura) {
        Faktura.Stopka stopka = faktura.getStopka();
        if (stopka == null) {
            return null;
        }
        List<String> notes = new ArrayList<>();
        if (stopka.getInformacje() != null) {
            for (Faktura.Stopka.Informacje info : stopka.getInformacje()) {
                if (info != null && info.getStopkaFaktury() != null) {
                    notes.add(info.getStopkaFaktury());
                }
            }
        }
        return new InvoiceFooter(notes, extractRegistries(stopka.getRejestry()));
    }

    private static List<RegistryEntry> extractRegistries(@Nullable List<Faktura.Stopka.Rejestry> registries) {
        if (registries == null || registries.isEmpty()) {
            return List.of();
        }
        List<RegistryEntry> out = new ArrayList<>(registries.size());
        for (Faktura.Stopka.Rejestry registry : registries) {
            if (registry != null) {
                out.add(new RegistryEntry(
                        registry.getPelnaNazwa(),
                        registry.getKRS(),
                        registry.getREGON(),
                        registry.getBDO()));
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

    private static @Nullable MarginScheme extractMarginScheme(Faktura.Fa.@Nullable Adnotacje adnotacje) {
        if (adnotacje == null || adnotacje.getPMarzy() == null) {
            return null;
        }
        Faktura.Fa.Adnotacje.PMarzy margin = adnotacje.getPMarzy();
        return new MarginScheme(
                toBooleanFlag(margin.getPPMarzy()),
                toBooleanFlag(margin.getPPMarzy2()),
                toBooleanFlag(margin.getPPMarzy31()),
                toBooleanFlag(margin.getPPMarzy32()),
                toBooleanFlag(margin.getPPMarzy33()),
                toBooleanFlag(margin.getPPMarzyN()));
    }

    private static @Nullable NewMeansOfTransport extractNewMeansOfTransport(Faktura.Fa.@Nullable Adnotacje adnotacje) {
        if (adnotacje == null || adnotacje.getNoweSrodkiTransportu() == null) {
            return null;
        }
        Faktura.Fa.Adnotacje.NoweSrodkiTransportu node = adnotacje.getNoweSrodkiTransportu();
        List<NewTransportItem> items = new ArrayList<>();
        if (node.getNowySrodekTransportu() != null) {
            for (Faktura.Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu item : node.getNowySrodekTransportu()) {
                if (item != null) {
                    items.add(mapNewTransportItem(item));
                }
            }
        }
        return new NewMeansOfTransport(
                toBooleanFlag(node.getP22()),
                toBooleanFlag(node.getP425()),
                items,
                toBooleanFlag(node.getP22N()));
    }

    private static NewTransportItem mapNewTransportItem(
            Faktura.Fa.Adnotacje.NoweSrodkiTransportu.NowySrodekTransportu item) {
        // P_22A and P_NrWierszaNST are both minOccurs=1. Trust the date directly;
        // default the line number to 1 defensively, as mapLineItem/mapOrderLine do.
        int lineNumber = item.getPNrWierszaNST() != null ? item.getPNrWierszaNST().intValue() : 1;
        return NewTransportItem.builder()
                .admissionDate(toLocalDate(item.getP22A()))
                .invoiceLineNumber(lineNumber)
                .make(item.getP22BMK())
                .model(item.getP22BMD())
                .color(item.getP22BK())
                .registrationNumber(item.getP22BNR())
                .productionYear(item.getP22BRP())
                .mileage(item.getP22B())
                .vin(item.getP22B1())
                .bodyNumber(item.getP22B2())
                .chassisNumber(item.getP22B3())
                .frameNumber(item.getP22B4())
                .vehicleType(item.getP22BT())
                .vesselWorkingHours(item.getP22C())
                .hullNumber(item.getP22C1())
                .aircraftWorkingHours(item.getP22D())
                .factoryNumber(item.getP22D1())
                .build();
    }

    private static @Nullable VatExemption extractVatExemption(Faktura.Fa.@Nullable Adnotacje adnotacje) {
        if (adnotacje == null || adnotacje.getZwolnienie() == null) {
            return null;
        }
        var zwolnienie = adnotacje.getZwolnienie();
        // Zwolnienie is an XSD choice: P_19 = 1 carries the exemption (with one
        // legal basis P_19A/B/C), while P_19N = 1 marks an explicitly non-exempt
        // supply. Surface an exemption only for the P_19 branch.
        if (toBooleanFlag(zwolnienie.getP19N()) != null || toBooleanFlag(zwolnienie.getP19()) == null) {
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

    /** Seller email from first {@code Podmiot1/DaneKontaktowe/Email} entry. */
    public @Nullable String sellerEmail() { return sellerEmail; }

    /** Seller phone from first {@code Podmiot1/DaneKontaktowe/Telefon} entry. */
    public @Nullable String sellerPhone() { return sellerPhone; }

    /** Seller address line 1 from {@code Podmiot1/Adres/AdresL1}. */
    public @Nullable String sellerAddressL1() { return sellerAddressL1; }

    /** Seller address line 2 from {@code Podmiot1/Adres/AdresL2}. */
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

    /** Issuing-system identifier from {@code Naglowek/SystemInfo}. */
    public @Nullable String systemInfo() { return systemInfo; }

    /** Split-payment / MPP flag from {@code Fa/Adnotacje/P_18A = 1}. */
    public boolean splitPayment() { return splitPayment; }

    /** Cash-method flag from {@code Fa/Adnotacje/P_16 = 1} (art. 19a/21 — "metoda kasowa"). */
    public boolean cashMethod() { return cashMethod; }

    /** Self-billing flag from {@code Fa/Adnotacje/P_17 = 1} (art. 106d — "samofakturowanie"). */
    public boolean selfBilling() { return selfBilling; }

    /** Reverse-charge flag from {@code Fa/Adnotacje/P_18 = 1} ("odwrotne obciazenie"). */
    public boolean reverseCharge() { return reverseCharge; }

    /** Simplified intra-EU triangular-transaction flag from {@code Fa/Adnotacje/P_23 = 1}. */
    public boolean simplifiedTriangular() { return simplifiedTriangular; }

    /** Margin-scheme annotations from {@code Fa/Adnotacje/PMarzy}. Null only when the annotation block is absent ({@code Adnotacje}/{@code PMarzy}), which the schema makes mandatory. */
    public @Nullable MarginScheme marginScheme() { return marginScheme; }

    /** New-means-of-transport annotations from {@code Fa/Adnotacje/NoweSrodkiTransportu} (intra-Community supply, art. 42 ust. 5). Null only when the annotation block is absent ({@code Adnotacje}/{@code NoweSrodkiTransportu}), which the schema makes mandatory. */
    public @Nullable NewMeansOfTransport newMeansOfTransport() { return newMeansOfTransport; }

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

    /** Billing period from {@code Fa/OkresFa} (continuous supplies), used instead of a single {@link #deliveryDate()}. Null when absent. */
    public @Nullable InvoicePeriod invoicePeriod() { return invoicePeriod; }

    /** Transaction conditions from {@code Fa/WarunkiTransakcji} — contracts, orders, delivery terms, contractual currency and transport legs. Null when absent. */
    public @Nullable TransactionConditions transactionConditions() { return transactionConditions; }

    /** Warehouse-issue (WZ) document numbers from {@code Fa/WZ}. Empty when none. */
    public List<String> deliveryNoteNumbers() { return deliveryNoteNumbers; }

    /** Additional key/value descriptions from {@code Fa/DodatkowyOpis}. Empty when none. */
    public List<AdditionalDescription> additionalDescriptions() { return additionalDescriptions; }

    /** Third parties from {@code Faktura/Podmiot3} — parties other than seller and buyer (additional buyers, factors, recipients). Empty when none. */
    public List<ThirdParty> thirdParties() { return thirdParties; }

    /** Pre-correction seller data from {@code Fa/Podmiot1K}. Non-null only on a correction that changes seller data. */
    public @Nullable CorrectionSeller correctionSeller() { return correctionSeller; }

    /** Pre-correction buyer data from {@code Fa/Podmiot2K} (buyer and any additional buyers). Empty on an original invoice. */
    public List<CorrectionBuyer> correctionBuyers() { return correctionBuyers; }

    /** Advance-payment order from {@code Fa/Zamowienie} — ordered goods/services priced for an advance invoice. Null when absent. */
    public @Nullable AdvanceOrder advanceOrder() { return advanceOrder; }

    /** Authorised party from {@code Faktura/PodmiotUpowazniony}. Null when the invoice carries no authorised party. */
    public @Nullable AuthorizedParty authorizedParty() { return authorizedParty; }

    /** Invoice footer from {@code Faktura/Stopka} — free-text footer lines and issuer register references. Null when absent. */
    public @Nullable InvoiceFooter footer() { return footer; }

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
    // P_11Vat and P_12 are all minOccurs="0" in the FA(2) XSD, so any of
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
