/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.CreditNoteType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.AddressType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.BillingReferenceType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.CountryType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.CreditNoteLineType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.DocumentReferenceType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.ItemType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.MonetaryTotalType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.PartyNameType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.PartyTaxSchemeType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.PartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.SupplierPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.TaxCategoryType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac.TaxSchemeType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.CityNameType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.CompanyIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.CreditNoteTypeCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.CreditedQuantityType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.CustomizationIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.DocumentCurrencyCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.EndpointIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.IDType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.IdentificationCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.IssueDateType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.LineExtensionAmountType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.NameType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.PayableAmountType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.PercentType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.PostalZoneType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.ProfileIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc.StreetNameType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Typed PEF_KOR(3) Peppol/UBL credit-note (correction invoice):
 * wraps the JAXB-generated {@link CreditNoteType} root.
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

    PefKorInvoice(CreditNoteType creditNote, byte[] xmlBytes) {
        this.creditNote = Objects.requireNonNull(creditNote, ERR_NULL_CREDIT_NOTE);
        this.xmlBytes = xmlBytes.clone();
    }

    @Override
    public FormCode formCode() {
        return FormCode.PEF_KOR3;
    }

    @Override
    public byte[] xml() {
        return xmlBytes.clone();
    }

    /** Underlying UBL JAXB tree — read-only access. */
    public CreditNoteType creditNote() {
        return creditNote;
    }

    /** Seller (AccountingSupplierParty). */
    public SupplierPartyType accountingSupplierParty() {
        return creditNote.getAccountingSupplierParty();
    }

    /** Buyer (AccountingCustomerParty). */
    public CustomerPartyType accountingCustomerParty() {
        return creditNote.getAccountingCustomerParty();
    }

    /** Read-only view of UBL CreditNoteLine elements. */
    public List<CreditNoteLineType> lines() {
        return creditNote.getCreditNoteLine() != null
                ? Collections.unmodifiableList(creditNote.getCreditNoteLine())
                : List.of();
    }

    /** Legal monetary total (totals block). */
    public MonetaryTotalType legalMonetaryTotal() {
        return creditNote.getLegalMonetaryTotal();
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

        Builder() {
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
