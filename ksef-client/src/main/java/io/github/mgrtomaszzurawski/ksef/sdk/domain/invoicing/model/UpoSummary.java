/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.xml.upo.Potwierdzenie;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Parsed view of a UPO (Urzędowe Poświadczenie Odbioru — official KSeF
 * receipt). Use {@link #parse(byte[])} to build from raw XAdES bytes
 * (e.g. archived UPO loaded from disk, or returned by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.UpoEntry#xml()}).
 *
 * <p>Includes the bit-exact {@code rawXml} bytes alongside the typed
 * accessors — the XAdES signature covers exact byte content, so any
 * archive use case must keep the original bytes too. Defensive byte[]
 * copy on construction and on every accessor.
 *
 * @param upoReferenceNumber the UPO/session reference number ({@code numerReferencyjnySesji})
 * @param acceptanceDate the earliest {@code dataNadaniaNumeruKSeF} across all documents in the UPO
 *     (UPO does not carry a session-level acceptance timestamp; per-document is the canonical source)
 * @param ksefNumbers the {@link KsefNumber} of every accepted invoice in this UPO
 * @param invoiceCount {@code opisPotwierdzenia.calkowitaLiczbaDokumentow} when present, otherwise
 *     {@code ksefNumbers.size()} (single-page UPOs may omit the pagination block)
 * @param successfulCount equal to {@code ksefNumbers.size()} (UPO only lists accepted invoices)
 * @param failedCount zero (UPO is for accepted invoices only — failures appear in the session
 *     status page, not in the UPO)
 * @param receivingEntity the human-readable name of the receiving entity
 *     ({@code nazwaPodmiotuPrzyjmujacego}, typically "Ministerstwo Finansów")
 * @param rawXml bit-exact UPO XML bytes (defensive copy; preserves the XAdES signature)
 *
 * @since 1.0.0
 */
public record UpoSummary(
        String upoReferenceNumber,
        OffsetDateTime acceptanceDate,
        List<KsefNumber> ksefNumbers,
        int invoiceCount,
        int successfulCount,
        int failedCount,
        String receivingEntity,
        byte[] rawXml) {

    private static final String ERR_NULL_REF = "upoReferenceNumber must not be null";
    private static final String ERR_NULL_ACCEPTANCE = "acceptanceDate must not be null";
    private static final String ERR_NULL_KSEF_NUMBERS = "ksefNumbers must not be null";
    private static final String ERR_NULL_RECEIVING = "receivingEntity must not be null";
    private static final String ERR_NULL_RAW_XML = "rawXml must not be null";
    private static final String ERR_NULL_BYTES = "xml must not be null";
    private static final String ERR_PARSE_FAILED = "Failed to parse UPO XML";
    private static final String ERR_NO_DOCUMENTS = "UPO contains no <Dokument> entries — cannot derive acceptanceDate";
    private static final String ERR_SAX_FAILED = "Failed to configure hardened SAX parser for UPO unmarshalling";
    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private static final SAXParserFactory SAX_PARSER_FACTORY = createHardenedSaxParserFactory();
    private static final JAXBContext UPO_CONTEXT = createUpoContext();

    public UpoSummary {
        Objects.requireNonNull(upoReferenceNumber, ERR_NULL_REF);
        Objects.requireNonNull(acceptanceDate, ERR_NULL_ACCEPTANCE);
        Objects.requireNonNull(ksefNumbers, ERR_NULL_KSEF_NUMBERS);
        Objects.requireNonNull(receivingEntity, ERR_NULL_RECEIVING);
        Objects.requireNonNull(rawXml, ERR_NULL_RAW_XML);
        ksefNumbers = List.copyOf(ksefNumbers);
        rawXml = rawXml.clone();
    }

    /**
     * Parse the bit-exact bytes of a UPO XAdES document into a typed
     * summary. The XAdES signature is preserved as-is in {@link #rawXml()}
     * for archive purposes; signature verification is out of scope and
     * remains the caller's responsibility if regulatory needs require it.
     *
     * @throws IllegalStateException when the bytes cannot be parsed as a
     *     valid UPO {@code Potwierdzenie} document
     */
    public static UpoSummary parse(byte[] xml) {
        Objects.requireNonNull(xml, ERR_NULL_BYTES);
        Potwierdzenie potwierdzenie = unmarshal(xml);
        List<Potwierdzenie.Dokument> documents = potwierdzenie.getDokument() != null
                ? potwierdzenie.getDokument()
                : List.of();
        if (documents.isEmpty()) {
            throw new IllegalStateException(ERR_NO_DOCUMENTS);
        }
        OffsetDateTime acceptance = documents.stream()
                .map(Potwierdzenie.Dokument::getDataNadaniaNumeruKSeF)
                .filter(Objects::nonNull)
                .map(UpoSummary::toOffsetDateTime)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException(ERR_NO_DOCUMENTS));
        List<KsefNumber> ksefNumbers = documents.stream()
                .map(Potwierdzenie.Dokument::getNumerKSeFDokumentu)
                .filter(Objects::nonNull)
                .map(KsefNumber::parse)
                .toList();
        int totalCount = potwierdzenie.getOpisPotwierdzenia() != null
                && potwierdzenie.getOpisPotwierdzenia().getCalkowitaLiczbaDokumentow() != null
                ? potwierdzenie.getOpisPotwierdzenia().getCalkowitaLiczbaDokumentow().intValue()
                : ksefNumbers.size();
        return new UpoSummary(
                potwierdzenie.getNumerReferencyjnySesji(),
                acceptance,
                ksefNumbers,
                totalCount,
                ksefNumbers.size(),
                0,
                potwierdzenie.getNazwaPodmiotuPrzyjmujacego(),
                xml);
    }

    @Override
    public byte[] rawXml() {
        return rawXml.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UpoSummary that)) {
            return false;
        }
        return invoiceCount == that.invoiceCount
                && successfulCount == that.successfulCount
                && failedCount == that.failedCount
                && Objects.equals(upoReferenceNumber, that.upoReferenceNumber)
                && Objects.equals(acceptanceDate, that.acceptanceDate)
                && Objects.equals(ksefNumbers, that.ksefNumbers)
                && Objects.equals(receivingEntity, that.receivingEntity)
                && Arrays.equals(rawXml, that.rawXml);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upoReferenceNumber, acceptanceDate, ksefNumbers,
                invoiceCount, successfulCount, failedCount, receivingEntity,
                Arrays.hashCode(rawXml));
    }

    @Override
    public String toString() {
        return "UpoSummary[upoReferenceNumber=" + upoReferenceNumber
                + ", acceptanceDate=" + acceptanceDate
                + ", invoiceCount=" + invoiceCount
                + ", successfulCount=" + successfulCount
                + ", rawXml=byte[" + rawXml.length + "]]";
    }

    private static OffsetDateTime toOffsetDateTime(XMLGregorianCalendar gregorian) {
        return gregorian.toGregorianCalendar().toZonedDateTime().withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private static Potwierdzenie unmarshal(byte[] xml) {
        try {
            Unmarshaller unmarshaller = UPO_CONTEXT.createUnmarshaller();
            SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setEntityResolver((publicId, systemId) -> new InputSource(new java.io.StringReader("")));
            SAXSource source = new SAXSource(reader, new InputSource(new ByteArrayInputStream(xml)));
            Object result = unmarshaller.unmarshal(source);
            if (result instanceof jakarta.xml.bind.JAXBElement<?> wrapper) {
                return (Potwierdzenie) wrapper.getValue();
            }
            return (Potwierdzenie) result;
        } catch (JAXBException | SAXException | javax.xml.parsers.ParserConfigurationException ex) {
            throw new IllegalStateException(ERR_PARSE_FAILED, ex);
        }
    }

    private static JAXBContext createUpoContext() {
        try {
            return JAXBContext.newInstance(Potwierdzenie.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ERR_PARSE_FAILED, ex);
        }
    }

    private static SAXParserFactory createHardenedSaxParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);
            factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);
            factory.setXIncludeAware(false);
        } catch (SAXNotRecognizedException | SAXNotSupportedException
                | javax.xml.parsers.ParserConfigurationException notSupported) {
            throw new IllegalStateException(ERR_SAX_FAILED, notSupported);
        }
        return factory;
    }
}
