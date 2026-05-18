/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R1-19 sub: pins the SAX root-element classification for the four
 * KSeF-recognised invoice schemas plus the unrecognised-root fast-fail
 * path. Each test writes a minimal XML stub to a temp file and confirms
 * the reader returns the expected {@link FormCode} (or throws).
 */
class InvoiceFileMetadataReaderTest {

    private static final String FA3_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Faktura xmlns=\"http://crd.gov.pl/wzor/2025/06/25/13775/\"/>";
    private static final String FA2_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Faktura xmlns=\"http://crd.gov.pl/wzor/2023/06/29/12648/\"/>";
    private static final String PEF3_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"/>";
    private static final String PEF_KOR3_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CreditNote xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2\"/>";
    private static final String UNRECOGNISED_NAMESPACE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Faktura xmlns=\"http://example.com/not-ksef\"/>";
    private static final String UNRECOGNISED_ROOT_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<RandomRoot/>";
    private static final String MALFORMED_XML = "<?xml version=\"1.0\"?><Unclosed";

    @TempDir
    Path tempDir;

    @Test
    void readFormCode_whenFa3Namespace_returnsFa3() throws IOException {
        Path file = writeXml("fa3.xml", FA3_XML);
        assertEquals(Optional.of(FormCode.FA3), InvoiceFileMetadataReader.readFormCode(file));
    }

    @Test
    void readFormCode_whenFa2Namespace_returnsFa2() throws IOException {
        Path file = writeXml("fa2.xml", FA2_XML);
        assertEquals(Optional.of(FormCode.FA2), InvoiceFileMetadataReader.readFormCode(file));
    }

    @Test
    void readFormCode_whenUblInvoiceRoot_returnsPef3() throws IOException {
        Path file = writeXml("pef.xml", PEF3_XML);
        assertEquals(Optional.of(FormCode.PEF3), InvoiceFileMetadataReader.readFormCode(file));
    }

    @Test
    void readFormCode_whenUblCreditNoteRoot_returnsPefKor3() throws IOException {
        Path file = writeXml("pef-kor.xml", PEF_KOR3_XML);
        assertEquals(Optional.of(FormCode.PEF_KOR3), InvoiceFileMetadataReader.readFormCode(file));
    }

    @Test
    void readFormCode_whenFakturaButForeignNamespace_returnsEmpty() throws IOException {
        Path file = writeXml("foreign.xml", UNRECOGNISED_NAMESPACE_XML);
        assertEquals(Optional.empty(), InvoiceFileMetadataReader.readFormCode(file));
    }

    @Test
    void readFormCode_whenUnknownRootElement_returnsEmpty() throws IOException {
        Path file = writeXml("unknown.xml", UNRECOGNISED_ROOT_XML);
        assertEquals(Optional.empty(), InvoiceFileMetadataReader.readFormCode(file));
    }

    @Test
    void readFormCode_whenMalformedXml_throwsKsefException() throws IOException {
        Path file = writeXml("malformed.xml", MALFORMED_XML);
        KsefException thrown = assertThrows(KsefException.class,
                () -> InvoiceFileMetadataReader.readFormCode(file));
        assertTrue(thrown.getMessage().contains(file.toString()));
    }

    @Test
    void readFormCodeOrThrow_whenUnknownRoot_throwsKsefExceptionWithFilePath() throws IOException {
        Path file = writeXml("orphan.xml", UNRECOGNISED_ROOT_XML);
        KsefException thrown = assertThrows(KsefException.class,
                () -> InvoiceFileMetadataReader.readFormCodeOrThrow(file));
        assertTrue(thrown.getMessage().contains(file.toString()));
        assertTrue(thrown.getMessage().contains("RandomRoot"));
    }

    @Test
    void readFormCodeOrThrow_whenFa3Namespace_returnsFa3() throws IOException {
        Path file = writeXml("fa3-strict.xml", FA3_XML);
        assertEquals(FormCode.FA3, InvoiceFileMetadataReader.readFormCodeOrThrow(file));
    }

    @Test
    void readFormCode_whenFileMissing_throwsKsefException() {
        Path missing = tempDir.resolve("does-not-exist.xml");
        KsefException thrown = assertThrows(KsefException.class,
                () -> InvoiceFileMetadataReader.readFormCode(missing));
        assertTrue(thrown.getMessage().contains(missing.toString()));
    }

    private Path writeXml(String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
