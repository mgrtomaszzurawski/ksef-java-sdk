/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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
    private static final String OBJECT_FACTORY_CLASS_NAME = "ObjectFactory";
    private static final String PEF_ROOT_PACKAGE = "io.github.mgrtomaszzurawski.ksef.xml.pef";
    private static final String PEFKOR_ROOT_PACKAGE = "io.github.mgrtomaszzurawski.ksef.xml.pefkor";
    private static final String UBL_ROOT_PACKAGE = "io.github.mgrtomaszzurawski.ksef.xml.ubl";
    /**
     * UBL sub-packages under {@code xml.ubl} — shared between PEF Invoice
     * and PEF_KOR CreditNote after the consolidation in
     * ADR-031. Each sub-package owns its own {@code ObjectFactory}
     * carrying the {@code @XmlElementDecl} bindings JAXB needs to resolve
     * qualified UBL element names like {@code {urn:oasis:Invoice-2}Invoice}
     * during unmarshal. Single-package context construction misses those
     * declarations and unmarshal fails on a global element lookup.
     *
     * <p>Only loaded when the root class is PEF or PEF_KOR — non-UBL
     * schemas (FA(2), FA(3), UPO, AUTH) build a much cheaper context
     * from just their own ObjectFactory, so a consumer using only FA(3)
     * never pays the UBL JAXBContext cost.
     */
    private static final String[] UBL_SUB_PACKAGES = {
            "cac", "cacpl", "cbc", "cbcpl", "ccts", "ext",
            "sig", "sigcac", "sigcbc",
            "udt",
            "xades132", "xades141", "xmldsig"
    };
    /**
     * Per-root-class JAXBContext cache. {@link ClassValue} ties the cache
     * lifetime to the keying Class's ClassLoader — in a hot-redeploy
     * container the redeployed app's class graph is collected with its
     * loader, the context goes with it. A static ConcurrentMap would
     * pin both the Class and the ClassLoader as GC roots, leaking memory
     * across redeploys.
     */
    private static final ClassValue<JAXBContext> CONTEXT_CACHE = new ClassValue<>() {
        @Override
        protected JAXBContext computeValue(Class<?> rootClass) {
            return buildContext(rootClass);
        }
    };
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
        return CONTEXT_CACHE.get(rootClass);
    }

    private static JAXBContext buildContext(Class<?> rootClass) {
        try {
            Class<?>[] factoryClasses = collectObjectFactories(rootClass);
            return JAXBContext.newInstance(factoryClasses);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ERR_CONTEXT_FAILED + rootClass.getName(), ex);
        }
    }

    /**
     * Collect every {@code ObjectFactory} required by {@code rootClass}'s
     * schema: always the root package's own factory; additionally, for the
     * UBL-derived PEF and PEFKOR schemas, the shared {@code xml.ubl.*}
     * sub-package factories that declare the qualified UBL element names.
     *
     * <p>For non-UBL schemas (FA(2), FA(3), UPO, AUTH) only the root
     * factory is returned, so a consumer using only FA(3) gets a JAXBContext
     * built over ~50 classes (~30 MB peak) rather than the full UBL world
     * (~1300 classes, ~150-300 MB peak). The cache in {@link #CONTEXT_CACHE}
     * is keyed per root class so the cost of each is paid lazily, once.
     */
    private static Class<?>[] collectObjectFactories(Class<?> rootClass) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String rootPackage = rootClass.getPackage().getName();
        java.util.List<Class<?>> factories = new java.util.ArrayList<>();
        Class<?> rootFactory = loadObjectFactory(loader, rootPackage);
        if (rootFactory != null) {
            factories.add(rootFactory);
        }
        if (PEF_ROOT_PACKAGE.equals(rootPackage) || PEFKOR_ROOT_PACKAGE.equals(rootPackage)) {
            for (String subPackage : UBL_SUB_PACKAGES) {
                Class<?> subFactory = loadObjectFactory(loader, UBL_ROOT_PACKAGE + '.' + subPackage);
                if (subFactory != null) {
                    factories.add(subFactory);
                }
            }
        }
        return factories.toArray(new Class<?>[0]);
    }

    private static Class<?> loadObjectFactory(ClassLoader loader, String packageName) {
        try {
            return Class.forName(packageName + '.' + OBJECT_FACTORY_CLASS_NAME, false, loader);
        } catch (ClassNotFoundException notPresent) {
            return null;
        }
    }
}
