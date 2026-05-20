/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Streaming root-element form-code reader for {@code submitFromFiles}.
 *
 * <p>R1-19 Phase 1A: walk the first non-whitespace
 * {@link XMLStreamConstants#START_ELEMENT} event on a {@link Path}, then
 * classify the {@code (localName, namespaceUri)} pair against the four
 * KSeF-recognised schemas. The full file is never materialised — StAX
 * pulls one event at a time, and the reader is closed immediately after
 * the first start element, so even a 5 GB batch file costs microseconds.
 *
 * <p>The classification table is the same one used by
 * {@code FormCodeDetector} on archive-fetched bytes; centralising on a
 * shared lookup table is intentionally avoided because the file-stream
 * path needs StAX-on-{@link InputStream} (this class) and the
 * byte-array path uses StAX-on-{@code ByteArrayInputStream} — the two
 * paths share knowledge of the namespace URIs but otherwise have no
 * shared infrastructure worth extracting.
 *
 * @apiNote Internal — never call from consumer code.
 *
 * @since 0.1.0
 */
public final class InvoiceFileMetadataReader {

    private static final String FA3_NAMESPACE = "http://crd.gov.pl/wzor/2025/06/25/13775/";
    private static final String FA2_NAMESPACE = "http://crd.gov.pl/wzor/2023/06/29/12648/";
    private static final String UBL_INVOICE_NAMESPACE =
            "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    private static final String UBL_CREDIT_NOTE_NAMESPACE =
            "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";

    private static final String FA_ROOT_LOCAL_NAME = "Faktura";
    private static final String UBL_INVOICE_ROOT_LOCAL_NAME = "Invoice";
    private static final String UBL_CREDIT_NOTE_ROOT_LOCAL_NAME = "CreditNote";

    private static final String ERR_NULL_FILE = "file must not be null";
    private static final String ERR_PARSE_FAILED =
            "Failed to read XML root element from %s";
    private static final String ERR_UNKNOWN_ROOT =
            "Cannot determine FormCode for %s — root element {%s} in namespace [%s] not recognised";

    private static final XMLInputFactory XML_FACTORY = createXmlInputFactory();

    private InvoiceFileMetadataReader() { }

    /**
     * Detect the {@link FormCode} of an invoice XML file by streaming
     * just the first start element. Returns empty when the root element /
     * namespace combination does not match any of FA(2), FA(3), PEF(3),
     * PEF_KOR(3) — caller decides how to handle the unknown case
     * (typically: fast-fail with the file path, since {@code submitFromFiles}
     * accepts a single declared {@link FormCode} for the whole batch).
     *
     * @throws KsefException if the bytes cannot be parsed as XML at all
     */
    public static Optional<FormCode> readFormCode(Path file) {
        Objects.requireNonNull(file, ERR_NULL_FILE);
        RootElement root = readRoot(file);
        return classify(root.localName(), root.namespaceUri());
    }

    /**
     * Same as {@link #readFormCode(Path)} but throws when the FormCode
     * cannot be determined.
     */
    public static FormCode readFormCodeOrThrow(Path file) {
        Objects.requireNonNull(file, ERR_NULL_FILE);
        RootElement root = readRoot(file);
        return classify(root.localName(), root.namespaceUri()).orElseThrow(() ->
                new KsefException(String.format(java.util.Locale.ROOT,
                        ERR_UNKNOWN_ROOT, file, root.localName(), root.namespaceUri()), null));
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

    private static RootElement readRoot(Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            XMLStreamReader reader = XML_FACTORY.createXMLStreamReader(stream);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        String namespaceUri = reader.getNamespaceURI();
                        return new RootElement(reader.getLocalName(),
                                namespaceUri != null ? namespaceUri : "");
                    }
                }
                throw new KsefException(String.format(java.util.Locale.ROOT,
                        ERR_PARSE_FAILED, file), null);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException | IOException parseFailure) {
            throw new KsefException(String.format(java.util.Locale.ROOT,
                    ERR_PARSE_FAILED, file), parseFailure);
        }
    }

    private static XMLInputFactory createXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory;
    }

    private record RootElement(String localName, String namespaceUri) { }
}
