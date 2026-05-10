/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import io.github.mgrtomaszzurawski.ksef.xml.pef.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.AddressType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.CountryType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.InvoiceLineType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.ItemType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.MonetaryTotalType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.PartyNameType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.PartyTaxSchemeType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.PartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.SupplierPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.TaxCategoryType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cac.TaxSchemeType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.CityNameType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.CompanyIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.CustomizationIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.DocumentCurrencyCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.EndpointIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.IDType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.IdentificationCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.InvoiceTypeCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.InvoicedQuantityType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.IssueDateType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.LineExtensionAmountType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.NameType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.PayableAmountType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.PercentType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.PostalZoneType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.ProfileIDType;
import io.github.mgrtomaszzurawski.ksef.xml.pef.cbc.StreetNameType;
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
 * Typed PEF(3) Peppol/UBL invoice: wraps the JAXB-generated
 * {@link InvoiceType} root and exposes its content alongside the
 * {@link Invoice} contract.
 *
 * <p>Construct via {@link #builder()} for a minimally-valid Peppol
 * BIS-style invoice. Use {@link Invoice#fromXml} for any UBL feature
 * not surfaced by the builder (allowance/charges, tax breakdowns,
 * payment means details, EU cross-border attachments).
 *
 * @since 1.0.0
 */
public final class PefInvoice implements Invoice {

    /** UBL Invoice-2 namespace. */
    static final String UBL_INVOICE_NAMESPACE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    /** Default UBL CustomizationID for PEF Polish-extension profile. */
    static final String PEF_CUSTOMIZATION_ID = "urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0";
    /** Default UBL ProfileID for PEF Polish-extension profile. */
    static final String PEF_PROFILE_ID = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";
    /** Default UBL InvoiceTypeCode for a standard commercial invoice. */
    static final String DEFAULT_INVOICE_TYPE_CODE = "380";

    private static final String ERR_NULL_INVOICE_TYPE = "invoiceType must not be null";

    private final InvoiceType invoiceType;
    private final byte[] xmlBytes;

    PefInvoice(InvoiceType invoiceType, byte[] xmlBytes) {
        this.invoiceType = Objects.requireNonNull(invoiceType, ERR_NULL_INVOICE_TYPE);
        this.xmlBytes = xmlBytes.clone();
    }

    @Override
    public FormCode formCode() {
        return FormCode.PEF3;
    }

    @Override
    public byte[] xml() {
        return xmlBytes.clone();
    }

    /** Underlying UBL JAXB tree — read-only access. */
    public InvoiceType invoiceType() {
        return invoiceType;
    }

    /** Seller (AccountingSupplierParty). */
    public SupplierPartyType accountingSupplierParty() {
        return invoiceType.getAccountingSupplierParty();
    }

    /** Buyer (AccountingCustomerParty). */
    public CustomerPartyType accountingCustomerParty() {
        return invoiceType.getAccountingCustomerParty();
    }

    /** Read-only view of UBL InvoiceLine elements. */
    public List<InvoiceLineType> lines() {
        return invoiceType.getInvoiceLine() != null
                ? Collections.unmodifiableList(invoiceType.getInvoiceLine())
                : List.of();
    }

    /** Legal monetary total (totals block). */
    public MonetaryTotalType legalMonetaryTotal() {
        return invoiceType.getLegalMonetaryTotal();
    }

    /** Begin building a new {@link PefInvoice}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link PefInvoice}. Common cases only. */
    public static final class Builder {

        private static final String ERR_NULL_INVOICE_NUMBER = "invoiceNumber must not be null";
        private static final String ERR_NULL_ISSUE_DATE = "issueDate must not be null";
        private static final String ERR_NULL_CURRENCY = "currencyCode must not be null";
        private static final String ERR_NULL_SUPPLIER = "supplier must not be null";
        private static final String ERR_NULL_CUSTOMER = "customer must not be null";
        private static final String ERR_NULL_LINE = "line must not be null";
        private static final String ERR_NULL_PAYABLE = "payableAmount must not be null";
        private static final String ERR_BAD_DATATYPE_FACTORY = "DatatypeFactory unavailable";
        private static final String UBL_TAX_SCHEME_ID_VAT = "VAT";
        private static final String DEFAULT_CURRENCY_CODE_PLN = "PLN";

        private String invoiceNumber;
        private LocalDate issueDate;
        private String currencyCode = DEFAULT_CURRENCY_CODE_PLN;
        private String invoiceTypeCode = DEFAULT_INVOICE_TYPE_CODE;
        private PefParty supplier;
        private PefParty customer;
        private final List<PefInvoiceLine> lines = new ArrayList<>();
        private BigDecimal payableAmount;

        Builder() {
        }

        /** Invoice number (cbc:ID). */
        public Builder invoiceNumber(String value) {
            this.invoiceNumber = Objects.requireNonNull(value, ERR_NULL_INVOICE_NUMBER);
            return this;
        }

        /** Issue date (cbc:IssueDate). */
        public Builder issueDate(LocalDate value) {
            this.issueDate = Objects.requireNonNull(value, ERR_NULL_ISSUE_DATE);
            return this;
        }

        /** ISO 4217 currency code (cbc:DocumentCurrencyCode). */
        public Builder currencyCode(String value) {
            this.currencyCode = Objects.requireNonNull(value, ERR_NULL_CURRENCY);
            return this;
        }

        /** UBL invoice type code (cbc:InvoiceTypeCode). Defaults to {@code "380"}. */
        public Builder invoiceTypeCode(String value) {
            this.invoiceTypeCode = Objects.requireNonNull(value);
            return this;
        }

        /** Seller (AccountingSupplierParty). */
        public Builder supplier(PefParty value) {
            this.supplier = Objects.requireNonNull(value, ERR_NULL_SUPPLIER);
            return this;
        }

        /** Buyer (AccountingCustomerParty). */
        public Builder customer(PefParty value) {
            this.customer = Objects.requireNonNull(value, ERR_NULL_CUSTOMER);
            return this;
        }

        /** Append one InvoiceLine entry. */
        public Builder addLine(PefInvoiceLine value) {
            this.lines.add(Objects.requireNonNull(value, ERR_NULL_LINE));
            return this;
        }

        /** Total payable amount (cac:LegalMonetaryTotal/cbc:PayableAmount). */
        public Builder payableAmount(BigDecimal value) {
            this.payableAmount = Objects.requireNonNull(value, ERR_NULL_PAYABLE);
            return this;
        }

        /** Build the typed invoice. */
        public PefInvoice build() {
            Objects.requireNonNull(invoiceNumber, ERR_NULL_INVOICE_NUMBER);
            Objects.requireNonNull(issueDate, ERR_NULL_ISSUE_DATE);
            Objects.requireNonNull(supplier, ERR_NULL_SUPPLIER);
            Objects.requireNonNull(customer, ERR_NULL_CUSTOMER);
            Objects.requireNonNull(payableAmount, ERR_NULL_PAYABLE);
            InvoiceType invoice = assembleInvoice();
            byte[] xml = JaxbInvoiceMarshaller.marshal(invoice, InvoiceType.class);
            return new PefInvoice(invoice, xml);
        }

        private InvoiceType assembleInvoice() {
            InvoiceType invoice = new InvoiceType();
            CustomizationIDType customizationId = new CustomizationIDType();
            customizationId.setValue(PEF_CUSTOMIZATION_ID);
            invoice.setCustomizationID(customizationId);
            ProfileIDType profileId = new ProfileIDType();
            profileId.setValue(PEF_PROFILE_ID);
            invoice.setProfileID(profileId);
            IDType id = new IDType();
            id.setValue(invoiceNumber);
            invoice.setID(id);
            IssueDateType issue = new IssueDateType();
            issue.setValue(toGregorianDate(issueDate));
            invoice.setIssueDate(issue);
            InvoiceTypeCodeType typeCode = new InvoiceTypeCodeType();
            typeCode.setValue(invoiceTypeCode);
            invoice.setInvoiceTypeCode(typeCode);
            DocumentCurrencyCodeType currency = new DocumentCurrencyCodeType();
            currency.setValue(currencyCode);
            invoice.setDocumentCurrencyCode(currency);
            invoice.setAccountingSupplierParty(buildSupplier(supplier));
            invoice.setAccountingCustomerParty(buildCustomer(customer));
            invoice.setLegalMonetaryTotal(buildLegalMonetaryTotal());
            for (PefInvoiceLine line : lines) {
                invoice.getInvoiceLine().add(buildInvoiceLine(line));
            }
            return invoice;
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

        private InvoiceLineType buildInvoiceLine(PefInvoiceLine source) {
            InvoiceLineType line = new InvoiceLineType();
            IDType lineId = new IDType();
            lineId.setValue(source.lineId());
            line.setID(lineId);
            InvoicedQuantityType quantity = new InvoicedQuantityType();
            quantity.setValue(source.quantity());
            quantity.setUnitCode(source.unitCode());
            line.setInvoicedQuantity(quantity);
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
