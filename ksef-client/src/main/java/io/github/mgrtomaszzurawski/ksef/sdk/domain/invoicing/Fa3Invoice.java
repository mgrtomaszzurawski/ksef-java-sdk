/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Typed FA(3) invoice: wraps the JAXB-generated {@link Faktura} root
 * and exposes its content alongside the {@link Invoice} contract
 * ({@link #formCode()}, {@link #xml()}).
 *
 * <p>Construct via {@link #builder()}. The builder mirrors the
 * minimum-viable subset of the FA(3) XSD — header, seller, buyer,
 * line items, currency, totals, correction reference. Rare fields
 * (advance payments, tourism margin, KOR_ROZ breakdowns, EU
 * cross-border attachments) are deferred to {@link Invoice#fromXml}.
 *
 * @since 1.0.0
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

    Fa3Invoice(Faktura faktura, byte[] xmlBytes) {
        this.faktura = Objects.requireNonNull(faktura, ERR_NULL_FAKTURA);
        this.xmlBytes = xmlBytes.clone();
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

    /** Header section (Naglowek). */
    public TNaglowek header() {
        return faktura.getNaglowek();
    }

    /** Seller (Podmiot1 / DaneIdentyfikacyjne). */
    public TPodmiot1 sellerIdentity() {
        return faktura.getPodmiot1() != null ? faktura.getPodmiot1().getDaneIdentyfikacyjne() : null;
    }

    /** Buyer (Podmiot2 / DaneIdentyfikacyjne). */
    public TPodmiot2 buyerIdentity() {
        return faktura.getPodmiot2() != null ? faktura.getPodmiot2().getDaneIdentyfikacyjne() : null;
    }

    /** Invoice content (Fa). */
    public Faktura.Fa content() {
        return faktura.getFa();
    }

    /** Read-only view of line items (FaWiersz list). */
    public List<Faktura.Fa.FaWiersz> lineItems() {
        return faktura.getFa() != null && faktura.getFa().getFaWiersz() != null
                ? Collections.unmodifiableList(faktura.getFa().getFaWiersz())
                : List.of();
    }

    /** Footer section (Stopka). */
    public Faktura.Stopka footer() {
        return faktura.getStopka();
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
        private static final String ERR_BAD_DATATYPE_FACTORY = "DatatypeFactory unavailable";
        private static final String ERR_CORRECTION_REQUIRED =
                "FA(3) correction invoices (RodzajFaktury KOR / KOR_ZAL / KOR_ROZ) require"
                        + " at least one correctionReference(...) call before build()";

        private static final byte FLAG_FALSE = 2;
        private static final byte FLAG_TRUE = 1;

        private LocalDate issueDate;
        private String issueLocality;
        private String invoiceNumber;
        private TKodWaluty currency = TKodWaluty.PLN;
        private TRodzajFaktury rodzajFaktury = TRodzajFaktury.VAT;
        private InvoiceParty seller;
        private InvoiceParty buyer;
        private final List<InvoiceLineItem> lineItems = new ArrayList<>();
        private final List<InvoiceCorrectionReference> correctionReferences = new ArrayList<>();
        private BigDecimal totalGrossAmount;

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
            faktura.setPodmiot1(buildSeller(seller));
            faktura.setPodmiot2(buildBuyer(buyer));
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
            header.setDataWytworzeniaFa(toGregorianDateTime(issueDate));
            return header;
        }

        private static Faktura.Podmiot1 buildSeller(InvoiceParty party) {
            Faktura.Podmiot1 podmiot = new Faktura.Podmiot1();
            TPodmiot1 identity = new TPodmiot1();
            identity.setNIP(party.nip());
            identity.setNazwa(party.name());
            podmiot.setDaneIdentyfikacyjne(identity);
            podmiot.setAdres(buildAddress(party));
            return podmiot;
        }

        private static Faktura.Podmiot2 buildBuyer(InvoiceParty party) {
            Faktura.Podmiot2 podmiot = new Faktura.Podmiot2();
            TPodmiot2 identity = new TPodmiot2();
            identity.setNIP(party.nip());
            identity.setNazwa(party.name());
            podmiot.setDaneIdentyfikacyjne(identity);
            podmiot.setAdres(buildAddress(party));
            return podmiot;
        }

        private static TAdresFa3 buildAddress(InvoiceParty party) {
            TAdresFa3 address = new TAdresFa3();
            address.setKodKraju(TKodKraju.fromValue(party.resolvedCountryCode()));
            address.setAdresL1(buildAddressLine1(party));
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
            Faktura.Fa fa = new Faktura.Fa();
            fa.setKodWaluty(currency);
            fa.setP1(toGregorianDate(issueDate));
            if (issueLocality != null) {
                fa.setP1M(issueLocality);
            }
            fa.setP2(invoiceNumber);
            fa.setP15(totalGrossAmount);
            fa.setRodzajFaktury(rodzajFaktury);
            fa.setAdnotacje(buildDefaultAdnotacje());
            for (InvoiceLineItem line : lineItems) {
                fa.getFaWiersz().add(buildLineItem(line));
            }
            for (InvoiceCorrectionReference correction : correctionReferences) {
                fa.getDaneFaKorygowanej().add(buildCorrectionRef(correction));
            }
            return fa;
        }

        private static Faktura.Fa.Adnotacje buildDefaultAdnotacje() {
            Faktura.Fa.Adnotacje adnotacje = new Faktura.Fa.Adnotacje();
            adnotacje.setP16(FLAG_FALSE);
            adnotacje.setP17(FLAG_FALSE);
            adnotacje.setP18(FLAG_FALSE);
            adnotacje.setP18A(FLAG_FALSE);
            adnotacje.setP23(FLAG_FALSE);
            Faktura.Fa.Adnotacje.Zwolnienie zwolnienie = new Faktura.Fa.Adnotacje.Zwolnienie();
            zwolnienie.setP19N(FLAG_TRUE);
            adnotacje.setZwolnienie(zwolnienie);
            Faktura.Fa.Adnotacje.NoweSrodkiTransportu nowe = new Faktura.Fa.Adnotacje.NoweSrodkiTransportu();
            nowe.setP22N(FLAG_TRUE);
            adnotacje.setNoweSrodkiTransportu(nowe);
            Faktura.Fa.Adnotacje.PMarzy pMarzy = new Faktura.Fa.Adnotacje.PMarzy();
            pMarzy.setPPMarzyN(FLAG_TRUE);
            adnotacje.setPMarzy(pMarzy);
            return adnotacje;
        }

        private static Faktura.Fa.FaWiersz buildLineItem(InvoiceLineItem line) {
            Faktura.Fa.FaWiersz wiersz = new Faktura.Fa.FaWiersz();
            wiersz.setNrWierszaFa(BigInteger.valueOf(line.rowNumber()));
            wiersz.setP7(line.description());
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
