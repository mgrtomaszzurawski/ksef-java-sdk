/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefCreditNoteLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb.JaxbDeepClone;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.CreditNoteType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.AddressType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.BillingReferenceType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CountryType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CreditNoteLineType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.DocumentReferenceType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.ItemType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.MonetaryTotalType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.PartyNameType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.PartyTaxSchemeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.PartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.SupplierPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.TaxCategoryType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.TaxSchemeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.CityNameType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.CompanyIDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.CreditNoteTypeCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.CreditedQuantityType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.CustomizationIDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.DocumentCurrencyCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.EndpointIDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.IDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.IdentificationCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.IssueDateType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.LineExtensionAmountType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.NameType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.PayableAmountType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.PercentType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.PostalZoneType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.ProfileIDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.StreetNameType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Typed PEF_KOR(3) Peppol/UBL credit-note (correction invoice):
 * wraps the JAXB-generated {@link CreditNoteType} root and exposes
 * its content as flat primitive accessors.
 *
 * <p>Construct via {@link #builder()}. PEF correction handling lives
 * exclusively in this typed impl — {@link PefInvoice} cannot represent
 * a correction document because the PEF wire schema uses a separate
 * UBL CreditNote root element for that purpose.
 *
 * @since 1.0.0
 */
public final class PefKorInvoice implements Invoice {

    /** UBL CreditNote-2 namespace. */
    static final String UBL_CREDIT_NOTE_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
    /** Default UBL CustomizationID for PEF Polish-extension profile. */
    static final String PEF_CUSTOMIZATION_ID = "urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0";
    /** Default UBL ProfileID for PEF Polish-extension profile. */
    static final String PEF_PROFILE_ID = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";
    /** Default UBL CreditNoteTypeCode for a commercial credit note. */
    static final String DEFAULT_CREDIT_NOTE_TYPE_CODE = "381";

    private static final String ERR_NULL_CREDIT_NOTE = "creditNote must not be null";

    private final CreditNoteType creditNote;
    private final byte[] xmlBytes;

    private final @org.jspecify.annotations.Nullable String invoiceNumber;
    private final @org.jspecify.annotations.Nullable LocalDate issueDate;
    private final @org.jspecify.annotations.Nullable String currency;
    private final @org.jspecify.annotations.Nullable String supplierEndpointId;
    private final @org.jspecify.annotations.Nullable String supplierName;
    private final @org.jspecify.annotations.Nullable String customerEndpointId;
    private final @org.jspecify.annotations.Nullable String customerName;
    private final @org.jspecify.annotations.Nullable BigDecimal payableAmount;
    private final List<PefCreditNoteLine> lines;

    PefKorInvoice(CreditNoteType creditNote, byte[] xmlBytes) {
        this.creditNote = Objects.requireNonNull(creditNote, ERR_NULL_CREDIT_NOTE);
        this.xmlBytes = xmlBytes.clone();
        this.invoiceNumber = creditNote.getID() != null ? creditNote.getID().getValue() : null;
        IssueDateType issue = creditNote.getIssueDate();
        this.issueDate = issue != null && issue.getValue() != null ? toLocalDate(issue.getValue()) : null;
        DocumentCurrencyCodeType code = creditNote.getDocumentCurrencyCode();
        this.currency = code != null ? code.getValue() : null;
        SupplierPartyType supplier = creditNote.getAccountingSupplierParty();
        PartyType supplierParty = supplier != null ? supplier.getParty() : null;
        this.supplierEndpointId = supplierParty != null && supplierParty.getEndpointID() != null
                ? supplierParty.getEndpointID().getValue() : null;
        this.supplierName = firstPartyName(supplierParty);
        CustomerPartyType customer = creditNote.getAccountingCustomerParty();
        PartyType customerParty = customer != null ? customer.getParty() : null;
        this.customerEndpointId = customerParty != null && customerParty.getEndpointID() != null
                ? customerParty.getEndpointID().getValue() : null;
        this.customerName = firstPartyName(customerParty);
        MonetaryTotalType total = creditNote.getLegalMonetaryTotal();
        this.payableAmount = total != null && total.getPayableAmount() != null
                ? total.getPayableAmount().getValue() : null;
        this.lines = snapshotLines(creditNote);
    }

    private static List<PefCreditNoteLine> snapshotLines(CreditNoteType creditNote) {
        if (creditNote.getCreditNoteLine() == null) {
            return List.of();
        }
        List<PefCreditNoteLine> mapped = new ArrayList<>(creditNote.getCreditNoteLine().size());
        for (CreditNoteLineType line : creditNote.getCreditNoteLine()) {
            PefCreditNoteLine item = mapLine(line);
            if (item != null) {
                mapped.add(item);
            }
        }
        return List.copyOf(mapped);
    }

    @Override
    public FormCode formCode() {
        return FormCode.PEF_KOR3;
    }

    @Override
    public byte[] xml() {
        return xmlBytes.clone();
    }

    /**
     * Direct reference to the internal UBL JAXB {@link CreditNoteType}
     * root — escape-hatch for fields the flat accessors do not surface
     * (BillingReference details, allowance/charges, tax breakdowns).
     *
     * <p><strong>Read-only by contract.</strong> Mutations are not
     * reflected in {@link #xml()} bytes. For a mutable disconnected
     * copy use {@link #toJaxbCopy()}; for build-time customisation use
     * {@link Builder#customizeJaxb(Consumer)}.
     */
    public CreditNoteType unsafeJaxbView() {
        return creditNote;
    }

    /**
     * Deep-clone of the internal UBL JAXB tree via a marshal/unmarshal
     * round-trip.
     */
    public CreditNoteType toJaxbCopy() {
        return JaxbDeepClone.clone(creditNote, CreditNoteType.class);
    }

    /** Credit-note number from {@code <cbc:ID>}. */
    public @org.jspecify.annotations.Nullable String invoiceNumber() { return invoiceNumber; }

    /** Issue date from {@code <cbc:IssueDate>}. */
    public @org.jspecify.annotations.Nullable LocalDate issueDate() { return issueDate; }

    /** Currency code from {@code <cbc:DocumentCurrencyCode>}. */
    public @org.jspecify.annotations.Nullable String currency() { return currency; }

    /** Supplier endpoint identifier (Peppol participant ID). */
    public @org.jspecify.annotations.Nullable String supplierEndpointId() { return supplierEndpointId; }

    /** Supplier registered name from {@code Party/PartyName/Name}. */
    public @org.jspecify.annotations.Nullable String supplierName() { return supplierName; }

    /** Customer endpoint identifier (Peppol participant ID). */
    public @org.jspecify.annotations.Nullable String customerEndpointId() { return customerEndpointId; }

    /** Customer registered name from {@code Party/PartyName/Name}. */
    public @org.jspecify.annotations.Nullable String customerName() { return customerName; }

    /** Total payable amount from {@code LegalMonetaryTotal/PayableAmount}. */
    public @org.jspecify.annotations.Nullable BigDecimal payableAmount() { return payableAmount; }

    /**
     * Lines mapped from UBL {@code <cac:CreditNoteLine>} entries to
     * SDK {@link PefCreditNoteLine} records. Lines that lack any
     * required UBL field for the SDK record are skipped.
     */
    public List<PefCreditNoteLine> lines() { return lines; }

    private static String firstPartyName(PartyType party) {
        if (party == null || party.getPartyName() == null || party.getPartyName().isEmpty()) {
            return null;
        }
        PartyNameType partyName = party.getPartyName().get(0);
        if (partyName == null || partyName.getName() == null) {
            return null;
        }
        return partyName.getName().getValue();
    }

    private static PefCreditNoteLine mapLine(CreditNoteLineType line) {
        if (line == null || line.getID() == null
                || line.getCreditedQuantity() == null
                || line.getLineExtensionAmount() == null
                || line.getItem() == null
                || line.getItem().getName() == null) {
            return null;
        }
        BigDecimal quantity = line.getCreditedQuantity().getValue();
        String unitCode = line.getCreditedQuantity().getUnitCode();
        BigDecimal amount = line.getLineExtensionAmount().getValue();
        String itemName = line.getItem().getName().getValue();
        BigDecimal vatPercent = firstClassifiedTaxPercent(line.getItem());
        if (quantity == null || unitCode == null || amount == null
                || itemName == null || vatPercent == null) {
            return null;
        }
        return new PefCreditNoteLine(
                line.getID().getValue(),
                quantity,
                unitCode,
                amount,
                itemName,
                vatPercent);
    }

    private static BigDecimal firstClassifiedTaxPercent(ItemType item) {
        if (item.getClassifiedTaxCategory() == null || item.getClassifiedTaxCategory().isEmpty()) {
            return null;
        }
        TaxCategoryType category = item.getClassifiedTaxCategory().get(0);
        if (category == null || category.getPercent() == null) {
            return null;
        }
        return category.getPercent().getValue();
    }

    private static LocalDate toLocalDate(XMLGregorianCalendar gregorian) {
        return LocalDate.of(gregorian.getYear(), gregorian.getMonth(), gregorian.getDay());
    }

    /** Begin building a new {@link PefKorInvoice}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link PefKorInvoice}. Common cases only. */
    public static final class Builder {

        private static final String ERR_NULL_INVOICE_NUMBER = "creditNoteNumber must not be null";
        private static final String ERR_NULL_ISSUE_DATE = "issueDate must not be null";
        private static final String ERR_NULL_CURRENCY = "currencyCode must not be null";
        private static final String ERR_NULL_SUPPLIER = "supplier must not be null";
        private static final String ERR_NULL_CUSTOMER = "customer must not be null";
        private static final String ERR_NULL_LINE = "line must not be null";
        private static final String ERR_NULL_PAYABLE = "payableAmount must not be null";
        private static final String ERR_NULL_ORIGINAL = "originalInvoiceNumber must not be null";
        private static final String ERR_NULL_CUSTOMIZER = "customizer must not be null";
        private static final String ERR_BAD_DATATYPE_FACTORY = "DatatypeFactory unavailable";
        private static final String UBL_TAX_SCHEME_ID_VAT = "VAT";
        private static final String DEFAULT_CURRENCY_CODE_PLN = "PLN";

        private String creditNoteNumber;
        private LocalDate issueDate;
        private String currencyCode = DEFAULT_CURRENCY_CODE_PLN;
        private String creditNoteTypeCode = DEFAULT_CREDIT_NOTE_TYPE_CODE;
        private PefParty supplier;
        private PefParty customer;
        private final List<PefInvoiceLine> lines = new ArrayList<>();
        private BigDecimal payableAmount;
        private String originalInvoiceNumber;
        private Consumer<CreditNoteType> customizer;

        Builder() {
        }

        /**
         * Customise the assembled UBL JAXB tree before marshalling — escape
         * hatch for fields the builder does not surface. The consumer runs
         * after typed setters have populated the root, immediately before
         * XML marshalling. Multiple calls compose by appending.
         */
        public Builder customizeJaxb(Consumer<CreditNoteType> jaxbCustomizer) {
            Objects.requireNonNull(jaxbCustomizer, ERR_NULL_CUSTOMIZER);
            this.customizer = this.customizer == null
                    ? jaxbCustomizer
                    : this.customizer.andThen(jaxbCustomizer);
            return this;
        }

        /** Credit-note number (cbc:ID). */
        public Builder creditNoteNumber(String value) {
            this.creditNoteNumber = Objects.requireNonNull(value, ERR_NULL_INVOICE_NUMBER);
            return this;
        }

        /** Issue date (cbc:IssueDate). */
        public Builder issueDate(LocalDate value) {
            this.issueDate = Objects.requireNonNull(value, ERR_NULL_ISSUE_DATE);
            return this;
        }

        /** ISO 4217 currency code. */
        public Builder currencyCode(String value) {
            this.currencyCode = Objects.requireNonNull(value, ERR_NULL_CURRENCY);
            return this;
        }

        /** UBL credit-note type code. Defaults to {@code "381"}. */
        public Builder creditNoteTypeCode(String value) {
            this.creditNoteTypeCode = Objects.requireNonNull(value);
            return this;
        }

        /** Seller. */
        public Builder supplier(PefParty value) {
            this.supplier = Objects.requireNonNull(value, ERR_NULL_SUPPLIER);
            return this;
        }

        /** Buyer. */
        public Builder customer(PefParty value) {
            this.customer = Objects.requireNonNull(value, ERR_NULL_CUSTOMER);
            return this;
        }

        /** Append one CreditNoteLine. */
        public Builder addLine(PefInvoiceLine value) {
            this.lines.add(Objects.requireNonNull(value, ERR_NULL_LINE));
            return this;
        }

        /** Total payable amount (correction net of any sign). */
        public Builder payableAmount(BigDecimal value) {
            this.payableAmount = Objects.requireNonNull(value, ERR_NULL_PAYABLE);
            return this;
        }

        /** Reference to the corrected invoice (cac:BillingReference). */
        public Builder originalInvoiceNumber(String value) {
            this.originalInvoiceNumber = Objects.requireNonNull(value, ERR_NULL_ORIGINAL);
            return this;
        }

        /** Build the typed credit-note invoice. */
        public PefKorInvoice build() {
            Objects.requireNonNull(creditNoteNumber, ERR_NULL_INVOICE_NUMBER);
            Objects.requireNonNull(issueDate, ERR_NULL_ISSUE_DATE);
            Objects.requireNonNull(supplier, ERR_NULL_SUPPLIER);
            Objects.requireNonNull(customer, ERR_NULL_CUSTOMER);
            Objects.requireNonNull(payableAmount, ERR_NULL_PAYABLE);
            Objects.requireNonNull(originalInvoiceNumber, ERR_NULL_ORIGINAL);
            CreditNoteType creditNote = assembleCreditNote();
            if (customizer != null) {
                customizer.accept(creditNote);
            }
            jakarta.xml.bind.JAXBElement<CreditNoteType> root =
                    new io.github.mgrtomaszzurawski.ksef.xml.pefkor.ObjectFactory().createCreditNote(creditNote);
            byte[] xml = JaxbInvoiceMarshaller.marshal(root, CreditNoteType.class);
            return new PefKorInvoice(creditNote, xml);
        }

        private CreditNoteType assembleCreditNote() {
            CreditNoteType note = new CreditNoteType();
            CustomizationIDType customizationId = new CustomizationIDType();
            customizationId.setValue(PEF_CUSTOMIZATION_ID);
            note.setCustomizationID(customizationId);
            ProfileIDType profileId = new ProfileIDType();
            profileId.setValue(PEF_PROFILE_ID);
            note.setProfileID(profileId);
            IDType id = new IDType();
            id.setValue(creditNoteNumber);
            note.setID(id);
            IssueDateType issue = new IssueDateType();
            issue.setValue(toGregorianDate(issueDate));
            note.setIssueDate(issue);
            CreditNoteTypeCodeType typeCode = new CreditNoteTypeCodeType();
            typeCode.setValue(creditNoteTypeCode);
            note.setCreditNoteTypeCode(typeCode);
            DocumentCurrencyCodeType currency = new DocumentCurrencyCodeType();
            currency.setValue(currencyCode);
            note.setDocumentCurrencyCode(currency);
            note.getBillingReference().add(buildBillingReference());
            note.setAccountingSupplierParty(buildSupplier(supplier));
            note.setAccountingCustomerParty(buildCustomer(customer));
            note.setLegalMonetaryTotal(buildLegalMonetaryTotal());
            for (PefInvoiceLine line : lines) {
                note.getCreditNoteLine().add(buildCreditNoteLine(line));
            }
            return note;
        }

        private BillingReferenceType buildBillingReference() {
            BillingReferenceType reference = new BillingReferenceType();
            DocumentReferenceType invoiceRef = new DocumentReferenceType();
            IDType invoiceId = new IDType();
            invoiceId.setValue(originalInvoiceNumber);
            invoiceRef.setID(invoiceId);
            reference.setInvoiceDocumentReference(invoiceRef);
            return reference;
        }

        private static SupplierPartyType buildSupplier(PefParty source) {
            SupplierPartyType supplierParty = new SupplierPartyType();
            supplierParty.setParty(buildParty(source));
            return supplierParty;
        }

        private static CustomerPartyType buildCustomer(PefParty source) {
            CustomerPartyType customerParty = new CustomerPartyType();
            customerParty.setParty(buildParty(source));
            return customerParty;
        }

        private static PartyType buildParty(PefParty source) {
            PartyType party = new PartyType();
            EndpointIDType endpoint = new EndpointIDType();
            endpoint.setValue(source.endpointId());
            endpoint.setSchemeID(source.resolvedEndpointSchemeId());
            party.setEndpointID(endpoint);
            PartyNameType partyName = new PartyNameType();
            NameType name = new NameType();
            name.setValue(source.registrationName());
            partyName.setName(name);
            party.getPartyName().add(partyName);
            party.setPostalAddress(buildAddress(source.address()));
            PartyTaxSchemeType taxScheme = new PartyTaxSchemeType();
            CompanyIDType companyId = new CompanyIDType();
            companyId.setValue(source.taxId());
            taxScheme.setCompanyID(companyId);
            taxScheme.setTaxScheme(buildVatTaxScheme());
            party.getPartyTaxScheme().add(taxScheme);
            return party;
        }

        private static AddressType buildAddress(PefAddress source) {
            AddressType address = new AddressType();
            if (source.streetName() != null) {
                StreetNameType street = new StreetNameType();
                street.setValue(source.streetName());
                address.setStreetName(street);
            }
            CityNameType city = new CityNameType();
            city.setValue(source.cityName());
            address.setCityName(city);
            PostalZoneType postalZone = new PostalZoneType();
            postalZone.setValue(source.postalZone());
            address.setPostalZone(postalZone);
            CountryType country = new CountryType();
            IdentificationCodeType countryCode = new IdentificationCodeType();
            countryCode.setValue(source.countryCode());
            country.setIdentificationCode(countryCode);
            address.setCountry(country);
            return address;
        }

        private MonetaryTotalType buildLegalMonetaryTotal() {
            MonetaryTotalType total = new MonetaryTotalType();
            PayableAmountType payable = new PayableAmountType();
            payable.setValue(payableAmount);
            payable.setCurrencyID(currencyCode);
            total.setPayableAmount(payable);
            return total;
        }

        private CreditNoteLineType buildCreditNoteLine(PefInvoiceLine source) {
            CreditNoteLineType line = new CreditNoteLineType();
            IDType lineId = new IDType();
            lineId.setValue(source.lineId());
            line.setID(lineId);
            CreditedQuantityType quantity = new CreditedQuantityType();
            quantity.setValue(source.quantity());
            quantity.setUnitCode(source.unitCode());
            line.setCreditedQuantity(quantity);
            LineExtensionAmountType lineAmount = new LineExtensionAmountType();
            lineAmount.setValue(source.lineExtensionAmount());
            lineAmount.setCurrencyID(currencyCode);
            line.setLineExtensionAmount(lineAmount);
            line.setItem(buildItem(source));
            return line;
        }

        private static ItemType buildItem(PefInvoiceLine source) {
            ItemType item = new ItemType();
            NameType itemName = new NameType();
            itemName.setValue(source.itemName());
            item.setName(itemName);
            TaxCategoryType taxCategory = new TaxCategoryType();
            PercentType percent = new PercentType();
            percent.setValue(source.vatPercent());
            taxCategory.setPercent(percent);
            taxCategory.setTaxScheme(buildVatTaxScheme());
            item.getClassifiedTaxCategory().add(taxCategory);
            return item;
        }

        private static TaxSchemeType buildVatTaxScheme() {
            TaxSchemeType scheme = new TaxSchemeType();
            IDType schemeId = new IDType();
            schemeId.setValue(UBL_TAX_SCHEME_ID_VAT);
            scheme.setID(schemeId);
            return scheme;
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
    }
}
