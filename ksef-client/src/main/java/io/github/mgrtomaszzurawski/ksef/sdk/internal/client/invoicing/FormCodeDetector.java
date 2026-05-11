/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Infers the {@link FormCode} of a fetched invoice from the XML root
 * element local-name and namespace. KSeF's REST response does not carry
 * a spec-defined {@code FormCode} header, so the SDK reads the first
 * 1KB of the body and matches against an ordered table of
 * {@code (rootLocalName, rootNamespace) → FormCode} pairs.
 *
 * <p>Adding a new schema = extend the {@link #detect(byte[])} switch
 * and add the namespace constant.
 */
public final class FormCodeDetector {

    static final String FA3_NAMESPACE = "http://crd.gov.pl/wzor/2025/06/25/13775/";
    static final String FA2_NAMESPACE = "http://crd.gov.pl/wzor/2023/06/29/12648/";
    static final String UBL_INVOICE_NAMESPACE =
            "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    static final String UBL_CREDIT_NOTE_NAMESPACE =
            "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";

    private static final String FA_ROOT_LOCAL_NAME = "Faktura";
    private static final String UBL_INVOICE_ROOT_LOCAL_NAME = "Invoice";
    private static final String UBL_CREDIT_NOTE_ROOT_LOCAL_NAME = "CreditNote";

    private static final String ERR_PARSE_FAILED =
            "Failed to read XML root element while detecting FormCode";
    private static final String ERR_UNKNOWN_NAMESPACE =
            "Cannot determine FormCode from response — root element {%s} in namespace [%s] not recognised";

    private static final XMLInputFactory XML_FACTORY = createXmlInputFactory();

    private FormCodeDetector() {
    }

    /**
     * Best-effort detection from the XML body. Returns empty when the
     * root element / namespace is not recognised. Caller decides how to
     * handle the unknown case (typically: wrap as {@code FormCode.custom}
     * + {@code InvoiceDocument.fromXml(...)}).
     *
     * @throws KsefException if the bytes cannot be parsed as XML at all
     */
    public static Optional<FormCode> detect(byte[] xml) {
        RootElement root = readRoot(xml);
        return classify(root.localName(), root.namespaceUri());
    }

    /**
     * Same as {@link #detect(byte[])} but throws when the FormCode cannot
     * be determined. Useful for the well-known-schema branch of
     * {@code Invoices.getByKsefNumber}.
     *
     * @throws KsefException when root element is unrecognised
     */
    public static FormCode detectOrThrow(byte[] xml) {
        RootElement root = readRoot(xml);
        return classify(root.localName(), root.namespaceUri()).orElseThrow(() ->
                new KsefException(String.format(java.util.Locale.ROOT,
                        ERR_UNKNOWN_NAMESPACE, root.localName(), root.namespaceUri()), null));
    }

    private static Optional<FormCode> classify(String localName, String namespaceUri) {
        if (FA_ROOT_LOCAL_NAME.equals(localName)) {
            if (FA3_NAMESPACE.equals(namespaceUri)) {
                return Optional.of(FormCode.FA3);
            }
            if (FA2_NAMESPACE.equals(namespaceUri)) {
                return Optional.of(FormCode.FA2);
            }
        }
        if (UBL_INVOICE_ROOT_LOCAL_NAME.equals(localName) && UBL_INVOICE_NAMESPACE.equals(namespaceUri)) {
            return Optional.of(FormCode.PEF3);
        }
        if (UBL_CREDIT_NOTE_ROOT_LOCAL_NAME.equals(localName) && UBL_CREDIT_NOTE_NAMESPACE.equals(namespaceUri)) {
            return Optional.of(FormCode.PEF_KOR3);
        }
        return Optional.empty();
    }

    private static RootElement readRoot(byte[] xml) {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(xml)) {
            XMLStreamReader reader = XML_FACTORY.createXMLStreamReader(bytes);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        String namespaceUri = reader.getNamespaceURI();
                        return new RootElement(reader.getLocalName(),
                                namespaceUri != null ? namespaceUri : "");
                    }
                }
                throw new KsefException(ERR_PARSE_FAILED, null);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException | java.io.IOException parseFailure) {
            throw new KsefException(ERR_PARSE_FAILED, parseFailure);
        }
    }

    private static XMLInputFactory createXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory;
    }

    private record RootElement(String localName, String namespaceUri) {
    }
}
