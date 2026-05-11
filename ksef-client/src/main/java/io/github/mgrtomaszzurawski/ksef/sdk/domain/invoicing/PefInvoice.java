/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefAddress;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefInvoiceLine;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PefParty;
import io.github.mgrtomaszzurawski.ksef.xml.pef.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.AddressType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CountryType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.CustomerPartyType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cac.InvoiceLineType;
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
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.CustomizationIDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.DocumentCurrencyCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.EndpointIDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.IDType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.IdentificationCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.InvoiceTypeCodeType;
import io.github.mgrtomaszzurawski.ksef.xml.ubl.cbc.InvoicedQuantityType;
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
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Typed PEF(3) Peppol/UBL invoice: wraps the JAXB-generated
 * {@link InvoiceType} root and exposes its content as flat primitive
 * accessors alongside the {@link Invoice} contract.
 *
 * <p>Construct via {@link #builder()} for a minimally-valid Peppol
 * BIS-style invoice. Use the {@link #invoice()} escape-hatch for any
 * UBL feature not surfaced by the flat accessors (allowance/charges,
 * tax breakdowns, payment means details, EU cross-border
 * attachments).
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

    /**
     * Underlying UBL JAXB tree — escape-hatch for fields the flat
     * accessors do not surface. Read-only access — do not mutate.
     */
    public InvoiceType invoice() {
        return invoiceType;
    }

    /** Invoice number from {@code <cbc:ID>}. */
    public String invoiceNumber() {
        return invoiceType.getID() != null ? invoiceType.getID().getValue() : null;
    }

    /** Issue date from {@code <cbc:IssueDate>}. */
    public LocalDate issueDate() {
        IssueDateType issue = invoiceType.getIssueDate();
        if (issue == null || issue.getValue() == null) {
            return null;
        }
        return toLocalDate(issue.getValue());
    }

    /** Currency code from {@code <cbc:DocumentCurrencyCode>}. */
    public String currency() {
        DocumentCurrencyCodeType code = invoiceType.getDocumentCurrencyCode();
        return code != null ? code.getValue() : null;
    }

    /** Supplier endpoint identifier (Peppol participant ID). */
    public String supplierEndpointId() {
        PartyType party = supplierParty();
        if (party == null || party.getEndpointID() == null) {
            return null;
        }
        return party.getEndpointID().getValue();
    }

    /** Supplier registered name from {@code Party/PartyName/Name}. */
    public String supplierName() {
        return firstPartyName(supplierParty());
    }

    /** Customer endpoint identifier (Peppol participant ID). */
    public String customerEndpointId() {
        PartyType party = customerParty();
        if (party == null || party.getEndpointID() == null) {
            return null;
        }
        return party.getEndpointID().getValue();
    }

    /** Customer registered name from {@code Party/PartyName/Name}. */
    public String customerName() {
        return firstPartyName(customerParty());
    }

    /** Total payable amount from {@code LegalMonetaryTotal/PayableAmount}. */
    public BigDecimal payableAmount() {
        MonetaryTotalType total = invoiceType.getLegalMonetaryTotal();
        if (total == null || total.getPayableAmount() == null) {
            return null;
        }
        return total.getPayableAmount().getValue();
    }

    /**
     * Line items mapped from UBL {@code <cac:InvoiceLine>} entries to
     * SDK {@link PefInvoiceLine} records. Lines that lack any
     * required UBL field for the SDK record are skipped.
     */
    public List<PefInvoiceLine> lines() {
        if (invoiceType.getInvoiceLine() == null) {
            return List.of();
        }
        List<PefInvoiceLine> mapped = new ArrayList<>(invoiceType.getInvoiceLine().size());
        for (InvoiceLineType line : invoiceType.getInvoiceLine()) {
            PefInvoiceLine item = mapLine(line);
            if (item != null) {
                mapped.add(item);
            }
        }
        return List.copyOf(mapped);
    }

    private PartyType supplierParty() {
        SupplierPartyType supplier = invoiceType.getAccountingSupplierParty();
        return supplier != null ? supplier.getParty() : null;
    }

    private PartyType customerParty() {
        CustomerPartyType customer = invoiceType.getAccountingCustomerParty();
        return customer != null ? customer.getParty() : null;
    }

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

    private static PefInvoiceLine mapLine(InvoiceLineType line) {
        if (line == null || line.getID() == null
                || line.getInvoicedQuantity() == null
                || line.getLineExtensionAmount() == null
                || line.getItem() == null
                || line.getItem().getName() == null) {
            return null;
        }
        BigDecimal quantity = line.getInvoicedQuantity().getValue();
        String unitCode = line.getInvoicedQuantity().getUnitCode();
        BigDecimal amount = line.getLineExtensionAmount().getValue();
        String itemName = line.getItem().getName().getValue();
        BigDecimal vatPercent = firstClassifiedTaxPercent(line.getItem());
        if (quantity == null || unitCode == null || amount == null
                || itemName == null || vatPercent == null) {
            return null;
        }
        return new PefInvoiceLine(
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
            jakarta.xml.bind.JAXBElement<InvoiceType> root =
                    new io.github.mgrtomaszzurawski.ksef.xml.pef.ObjectFactory().createInvoice(invoice);
            byte[] xml = JaxbInvoiceMarshaller.marshal(root, InvoiceType.class);
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
