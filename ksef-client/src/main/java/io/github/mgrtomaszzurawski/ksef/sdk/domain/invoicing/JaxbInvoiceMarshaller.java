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
    private static final ConcurrentMap<Class<?>, JAXBContext> CONTEXT_CACHE = new ConcurrentHashMap<>();
    private static final javax.xml.stream.XMLInputFactory XML_INPUT_FACTORY = createHardenedXmlInputFactory();

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
            javax.xml.stream.XMLStreamReader streamReader =
                    XML_INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(xml));
            try {
                jakarta.xml.bind.JAXBElement<T> result = unmarshaller.unmarshal(streamReader, rootClass);
                return result.getValue();
            } finally {
                streamReader.close();
            }
        } catch (JAXBException | javax.xml.stream.XMLStreamException ex) {
            throw new IllegalStateException(ERR_UNMARSHAL_FAILED, ex);
        }
    }

    private static javax.xml.stream.XMLInputFactory createHardenedXmlInputFactory() {
        javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
        factory.setProperty(javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory;
    }

    private static JAXBContext contextFor(Class<?> rootClass) {
        return CONTEXT_CACHE.computeIfAbsent(rootClass, JaxbInvoiceMarshaller::buildContext);
    }

    private static JAXBContext buildContext(Class<?> rootClass) {
        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader loader = contextLoader != null ? contextLoader : rootClass.getClassLoader();
            return JAXBContext.newInstance(rootClass.getPackage().getName(), loader);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ERR_CONTEXT_FAILED + rootClass.getName(), ex);
        }
    }
}
