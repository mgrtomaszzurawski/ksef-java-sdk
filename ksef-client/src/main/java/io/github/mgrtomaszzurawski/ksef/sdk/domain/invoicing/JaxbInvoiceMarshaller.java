/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Internal helper for marshalling and unmarshalling invoice JAXB
 * trees produced by the typed builders.
 *
 * <p>Caches one {@link JAXBContext} per root class (FA(2), FA(3), PEF
 * Invoice, PEF_KOR CreditNote) — context construction is the expensive
 * part of JAXB initialisation, so amortising it across builder calls
 * keeps {@code build()} cheap. The marshallers themselves are not
 * thread-safe and are created on every call.
 *
 * <p>Package-private — typed Invoice impls are the only callers.
 *
 * @since 1.0.0
 */
final class JaxbInvoiceMarshaller {

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String ERR_MARSHAL_FAILED = "Failed to marshal invoice JAXB tree to XML";
    private static final String ERR_UNMARSHAL_FAILED = "Failed to unmarshal invoice XML";
    private static final String ERR_CONTEXT_FAILED = "Failed to initialise JAXB context for ";
    private static final String ERR_SAX_PARSER_FAILED =
            "Failed to configure hardened SAX parser for JAXB unmarshalling";

    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private static final ConcurrentMap<Class<?>, JAXBContext> CONTEXT_CACHE = new ConcurrentHashMap<>();
    private static final SAXParserFactory SAX_PARSER_FACTORY = createHardenedSaxParserFactory();

    private JaxbInvoiceMarshaller() {
    }

    static byte[] marshal(Object jaxbRoot, Class<?> rootClass) {
        try {
            JAXBContext context = contextFor(rootClass);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(XML_HEADER.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            marshaller.marshal(jaxbRoot, output);
            return output.toByteArray();
        } catch (JAXBException | java.io.IOException ex) {
            throw new IllegalStateException(ERR_MARSHAL_FAILED, ex);
        }
    }

    static <T> T unmarshal(byte[] xml, Class<T> rootClass) {
        try {
            JAXBContext context = contextFor(rootClass);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SAXSource hardenedSource = hardenedSource(new ByteArrayInputStream(xml));
            Object result = unmarshaller.unmarshal(hardenedSource);
            if (result instanceof jakarta.xml.bind.JAXBElement<?> wrapper) {
                return rootClass.cast(wrapper.getValue());
            }
            return rootClass.cast(result);
        } catch (JAXBException | SAXException ex) {
            throw new IllegalStateException(ERR_UNMARSHAL_FAILED, ex);
        }
    }

    /**
     * Wrap an input stream in a SAX source with DOCTYPE disabled and
     * external entity resolution turned off — defends against XXE
     * (CWE-611) and billion-laughs (CWE-776) attacks.
     */
    private static SAXSource hardenedSource(ByteArrayInputStream input) throws SAXException {
        try {
            SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setEntityResolver((publicId, systemId) -> new InputSource(new java.io.StringReader("")));
            return new SAXSource(reader, new InputSource(input));
        } catch (javax.xml.parsers.ParserConfigurationException configFailure) {
            throw new IllegalStateException(ERR_SAX_PARSER_FAILED, configFailure);
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
            throw new IllegalStateException(ERR_SAX_PARSER_FAILED, notSupported);
        }
        return factory;
    }

    private static JAXBContext contextFor(Class<?> rootClass) {
        return CONTEXT_CACHE.computeIfAbsent(rootClass, JaxbInvoiceMarshaller::buildContext);
    }

    private static JAXBContext buildContext(Class<?> rootClass) {
        try {
            return JAXBContext.newInstance(rootClass);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ERR_CONTEXT_FAILED + rootClass.getName(), ex);
        }
    }
}
