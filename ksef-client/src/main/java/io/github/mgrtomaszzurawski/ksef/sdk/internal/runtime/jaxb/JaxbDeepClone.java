/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.jaxb;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deep-clone JAXB-generated roots via a marshal/unmarshal round-trip.
 * {@link JAXBContext}s are cached per root class so the marshal-then-parse
 * cost amortises across repeated clone calls.
 *
 * <p>Used by the {@code *Invoice} / {@code *InvoiceDocument} escape-hatch
 * {@code toJaxbCopy()} accessors: the returned object is mutable but
 * disconnected from the internal JAXB tree (and from the bytes that the
 * {@code xml()} accessor still returns).
 *
 * @since 0.1.0
 */
public final class JaxbDeepClone {

    private static final Map<Class<?>, JAXBContext> CONTEXTS = new ConcurrentHashMap<>();

    private static final String ERR_NULL_SOURCE = "source must not be null";
    private static final String ERR_NULL_TYPE = "type must not be null";
    private static final String ERR_FAILED = "JAXB deep-clone failed for ";

    private JaxbDeepClone() {
    }

    /**
     * Marshal {@code source} to a byte buffer using the JAXBContext for
     * {@code type}, then unmarshal back into a fresh instance.
     *
     * @param source the JAXB root to clone (non-null)
     * @param type the root class (used to obtain a cached {@link JAXBContext})
     * @param <T> the JAXB root type
     * @return a fresh, mutable copy that shares no references with {@code source}
     * @throws KsefException when JAXB marshalling or unmarshalling fails — wraps
     *     the underlying {@link JAXBException} so callers do not need to handle
     *     checked exceptions from a serialisation helper
     */
    public static <T> T clone(T source, Class<T> type) {
        Objects.requireNonNull(source, ERR_NULL_SOURCE);
        Objects.requireNonNull(type, ERR_NULL_TYPE);
        try {
            JAXBContext context = CONTEXTS.computeIfAbsent(type, JaxbDeepClone::newContext);
            Marshaller marshaller = context.createMarshaller();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            marshaller.marshal(source, buffer);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return type.cast(unmarshaller.unmarshal(new ByteArrayInputStream(buffer.toByteArray())));
        } catch (JAXBException ex) {
            throw new KsefException(ERR_FAILED + type.getName(), ex);
        }
    }

    private static JAXBContext newContext(Class<?> type) {
        try {
            return JAXBContext.newInstance(type);
        } catch (JAXBException ex) {
            throw new KsefException(ERR_FAILED + type.getName(), ex);
        }
    }
}
