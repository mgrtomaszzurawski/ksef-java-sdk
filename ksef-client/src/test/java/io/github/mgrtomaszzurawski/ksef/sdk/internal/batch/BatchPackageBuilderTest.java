/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchPackageBuilderTest {

    private static final String SHA_256 = "SHA-256";
    private static final int AES_KEY_BYTES = 32;
    private static final int AES_IV_BYTES = 16;
    private static final byte AES_KEY_FILL = (byte) 0xAA;
    private static final byte AES_IV_FILL = (byte) 0xBB;
    private static final String INVOICE_ONE_XML = "<Invoice><Number>INV-1</Number></Invoice>";
    private static final String INVOICE_TWO_XML = "<Invoice><Number>INV-2</Number></Invoice>";
    private static final String INVOICE_THREE_XML = "<Invoice><Number>INV-3</Number></Invoice>";
    private static final long SMALL_PART_SIZE = 64L;
    private static final int EXPECTED_TWO_INVOICES = 2;
    private static final int EXPECTED_HASH_BYTES = 32;
    private static final int FIRST_PART_ORDINAL = 1;
    private static final int SECOND_PART_ORDINAL = 2;

    private static byte[] aesKey() {
        byte[] key = new byte[AES_KEY_BYTES];
        Arrays.fill(key, AES_KEY_FILL);
        return key;
    }

    private static byte[] aesIv() {
        byte[] iv = new byte[AES_IV_BYTES];
        Arrays.fill(iv, AES_IV_FILL);
        return iv;
    }

    private static byte[] sha256(byte[] data) throws Exception {
        return MessageDigest.getInstance(SHA_256).digest(data);
    }

    /**
     * Per KSeF spec: each part is independently encrypted. Decrypt each part separately,
     * concatenate the plaintext chunks → the original (unencrypted) ZIP.
     */
    private static byte[] decryptAndConcat(List<Path> partFiles) throws Exception {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (Path part : partFiles) {
            byte[] encrypted = Files.readAllBytes(part);
            byte[] decrypted = CryptoService.decryptAes(encrypted, aesKey(), aesIv());
            all.write(decrypted);
        }
        return all.toByteArray();
    }

    @Test
    void build_whenNullInvoices_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> BatchPackageBuilder.build(null, aesKey(), aesIv()));
    }

    @Test
    void build_whenEmptyInvoices_throwsIllegalArgumentException() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> BatchPackageBuilder.build(List.of(), aesKey(), aesIv()));
    }

    @Test
    void build_whenSingleInvoice_producesOnePart() throws Exception {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());

        // then
        try {
            assertEquals(1, pkg.spec().parts().size());
            assertEquals(FIRST_PART_ORDINAL, pkg.spec().parts().get(0).ordinalNumber());
            assertEquals(1, pkg.partFiles().size());

            // fileSize/fileHash describe the unencrypted ZIP — recover it by decrypting
            byte[] zipBytes = decryptAndConcat(pkg.partFiles());
            assertEquals(zipBytes.length, pkg.spec().fileSize());
            assertArrayEquals(sha256(zipBytes), pkg.spec().fileHash());
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void build_whenInvoicesEncrypted_zipDecryptsToValidArchive() throws Exception {
        // given
        byte[] invoiceOne = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        byte[] invoiceTwo = INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoiceOne, invoiceTwo), aesKey(), aesIv());

        // then — decrypt each part, concat, parse as ZIP
        try {
            byte[] zipBytes = decryptAndConcat(pkg.partFiles());

            Set<String> entryNames = new HashSet<>();
            List<byte[]> contents = new ArrayList<>();
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entryNames.add(entry.getName());
                    contents.add(zip.readAllBytes());
                }
            }
            assertEquals(EXPECTED_TWO_INVOICES, entryNames.size());
            assertTrue(contents.contains(invoiceOne)
                    || Arrays.equals(contents.get(0), invoiceOne)
                    || Arrays.equals(contents.get(1), invoiceOne));
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void build_whenSplitIntoMultipleParts_decryptedPartsConcatToOriginalZip() throws Exception {
        // given — 3 invoices, force splitting with a small max chunk size
        List<byte[]> invoices = List.of(
                INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_THREE_XML.getBytes(StandardCharsets.UTF_8));

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — multiple parts produced, ordinals are 1-based and contiguous
        try {
            assertTrue(pkg.spec().parts().size() > 1, "expected multiple parts");
            for (int index = 0; index < pkg.spec().parts().size(); index++) {
                assertEquals(index + 1, pkg.spec().parts().get(index).ordinalNumber());
            }

            // decrypt each part and concat — should equal original unencrypted ZIP
            byte[] zipBytes = decryptAndConcat(pkg.partFiles());
            assertEquals(pkg.spec().fileSize(), zipBytes.length);
            assertArrayEquals(sha256(zipBytes), pkg.spec().fileHash());
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void build_partHashes_matchSha256OfEncryptedPartFiles() throws Exception {
        // given
        List<byte[]> invoices = List.of(
                INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8));

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — each spec.parts().fileHash matches sha256(encrypted part file content)
        try {
            for (int index = 0; index < pkg.partFiles().size(); index++) {
                byte[] content = Files.readAllBytes(pkg.partFiles().get(index));
                byte[] expectedHash = sha256(content);
                assertEquals(EXPECTED_HASH_BYTES, expectedHash.length);
                assertArrayEquals(expectedHash, pkg.spec().parts().get(index).fileHash());
                assertEquals(content.length, pkg.spec().parts().get(index).fileSize());
            }
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void build_specFileHash_matchesSha256OfUnencryptedZip() throws Exception {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());

        // then — fileHash describes the *unencrypted* ZIP (per KSeF spec)
        try {
            byte[] zipBytes = decryptAndConcat(pkg.partFiles());
            assertArrayEquals(sha256(zipBytes), pkg.spec().fileHash());
            assertEquals(zipBytes.length, pkg.spec().fileSize());
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void build_returnsPositionalPartOrdinals() {
        // given
        List<byte[]> invoices = List.of(
                INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8));

        // when — small part size forces multiple parts
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — ordinals start at 1
        try {
            assertEquals(FIRST_PART_ORDINAL, pkg.spec().parts().get(0).ordinalNumber());
            if (pkg.spec().parts().size() >= EXPECTED_TWO_INVOICES) {
                assertEquals(SECOND_PART_ORDINAL, pkg.spec().parts().get(1).ordinalNumber());
            }
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void cleanup_deletesPartFiles() {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());
        List<Path> partFiles = pkg.partFiles();
        for (Path part : partFiles) {
            assertTrue(Files.exists(part), "part file should exist before cleanup");
        }

        // when
        pkg.cleanup();

        // then
        for (Path part : partFiles) {
            assertFalse(Files.exists(part), "part file should be deleted after cleanup");
        }
    }

    @Test
    void cleanup_isIdempotent() {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());

        // when
        pkg.cleanup();
        pkg.cleanup();  // second call should not throw
        // then — no exception
    }
}
