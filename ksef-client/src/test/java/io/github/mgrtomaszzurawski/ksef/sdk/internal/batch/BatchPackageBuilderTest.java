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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        byte[] initVector = new byte[AES_IV_BYTES];
        Arrays.fill(initVector, AES_IV_FILL);
        return initVector;
    }

    private static byte[] sha256(byte[] data) throws Exception {
        return MessageDigest.getInstance(SHA_256).digest(data);
    }

    /**
     * Per KSeF spec: each part is independently encrypted. Decrypt each part separately,
     * concatenate the plaintext chunks → the original (unencrypted) ZIP.
     */
    private static byte[] decryptAndConcat(BatchPackageBuilder.BatchPackage pkg) throws Exception {
        ByteArrayOutputStream concatenated = new ByteArrayOutputStream();
        for (int index = 0; index < pkg.parts().size(); index++) {
            byte[] encrypted = pkg.readPartBytes(index);
            byte[] decrypted = CryptoService.decryptAes(encrypted, aesKey(), aesIv());
            concatenated.write(decrypted);
        }
        return concatenated.toByteArray();
    }

    @Test
    void build_whenNullInvoices_throwsNullPointerException() {
        // given
        byte[] sessionAesKey = aesKey();
        byte[] sessionInitVector = aesIv();
        // when / then
        assertThrows(NullPointerException.class,
                () -> BatchPackageBuilder.build(null, sessionAesKey, sessionInitVector));
    }

    @Test
    void build_whenEmptyInvoices_throwsIllegalArgumentException() {
        // given
        byte[] sessionAesKey = aesKey();
        byte[] sessionInitVector = aesIv();
        List<byte[]> empty = List.of();
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> BatchPackageBuilder.build(empty, sessionAesKey, sessionInitVector));
    }

    @Test
    void build_whenSingleInvoice_producesOnePart() throws Exception {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());

        // then
        try {
            assertEquals(1, batchPackage.spec().parts().size());
            assertEquals(FIRST_PART_ORDINAL, batchPackage.spec().parts().get(0).ordinalNumber());
            assertEquals(1, batchPackage.parts().size());

            // fileSize/fileHash describe the unencrypted ZIP — recover it by decrypting
            byte[] zipBytes = decryptAndConcat(batchPackage);
            assertEquals(zipBytes.length, batchPackage.spec().fileSize());
            assertArrayEquals(sha256(zipBytes), batchPackage.spec().fileHash());
        } finally {
            batchPackage.cleanup();
        }
    }

    @Test
    void build_whenInvoicesEncrypted_zipDecryptsToValidArchive() throws Exception {
        // given
        byte[] invoiceOne = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        byte[] invoiceTwo = INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                List.of(invoiceOne, invoiceTwo), aesKey(), aesIv());

        // then — decrypt each part, concat, parse as ZIP
        try {
            byte[] zipBytes = decryptAndConcat(batchPackage);

            Set<String> entryNames = new HashSet<>();
            List<byte[]> contents = new ArrayList<>();
            try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zipStream.getNextEntry()) != null) {
                    entryNames.add(entry.getName());
                    contents.add(zipStream.readAllBytes());
                }
            }
            assertEquals(EXPECTED_TWO_INVOICES, entryNames.size());
            assertTrue(contents.contains(invoiceOne)
                    || Arrays.equals(contents.get(0), invoiceOne)
                    || Arrays.equals(contents.get(1), invoiceOne));
        } finally {
            batchPackage.cleanup();
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
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — multiple parts produced, ordinals are 1-based and contiguous
        try {
            assertTrue(batchPackage.spec().parts().size() > 1, "expected multiple parts");
            for (int index = 0; index < batchPackage.spec().parts().size(); index++) {
                assertEquals(index + 1, batchPackage.spec().parts().get(index).ordinalNumber());
            }

            // decrypt each part and concat — should equal original unencrypted ZIP
            byte[] zipBytes = decryptAndConcat(batchPackage);
            assertEquals(batchPackage.spec().fileSize(), zipBytes.length);
            assertArrayEquals(sha256(zipBytes), batchPackage.spec().fileHash());
        } finally {
            batchPackage.cleanup();
        }
    }

    @Test
    void build_partHashes_matchSha256OfEncryptedPartFiles() throws Exception {
        // given
        List<byte[]> invoices = List.of(
                INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8));

        // when
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — each spec.parts().fileHash matches sha256(encrypted part file content)
        try {
            for (int index = 0; index < batchPackage.parts().size(); index++) {
                byte[] content = batchPackage.readPartBytes(index);
                byte[] expectedHash = sha256(content);
                assertEquals(EXPECTED_HASH_BYTES, expectedHash.length);
                assertArrayEquals(expectedHash, batchPackage.spec().parts().get(index).fileHash());
                assertEquals(content.length, batchPackage.spec().parts().get(index).fileSize());
            }
        } finally {
            batchPackage.cleanup();
        }
    }

    @Test
    void build_specFileHash_matchesSha256OfUnencryptedZip() throws Exception {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());

        // then — fileHash describes the *unencrypted* ZIP (per KSeF spec)
        try {
            byte[] zipBytes = decryptAndConcat(batchPackage);
            assertArrayEquals(sha256(zipBytes), batchPackage.spec().fileHash());
            assertEquals(zipBytes.length, batchPackage.spec().fileSize());
        } finally {
            batchPackage.cleanup();
        }
    }

    @Test
    void build_returnsPositionalPartOrdinals() {
        // given
        List<byte[]> invoices = List.of(
                INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8));

        // when — small part size forces multiple parts
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — ordinals start at 1
        try {
            assertEquals(FIRST_PART_ORDINAL, batchPackage.spec().parts().get(0).ordinalNumber());
            if (batchPackage.spec().parts().size() >= EXPECTED_TWO_INVOICES) {
                assertEquals(SECOND_PART_ORDINAL, batchPackage.spec().parts().get(1).ordinalNumber());
            }
        } finally {
            batchPackage.cleanup();
        }
    }

    @Test
    void cleanup_deletesPartFiles() {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());
        List<Path> partPaths = new ArrayList<>();
        for (var part : batchPackage.parts()) {
            assertTrue(part instanceof io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPart.OnDiskPart,
                    "default mode is on-disk");
            Path path = ((io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPart.OnDiskPart) part).path();
            partPaths.add(path);
            assertTrue(Files.exists(path), "part file should exist before cleanup");
        }

        // when
        batchPackage.cleanup();

        // then
        for (Path part : partPaths) {
            assertFalse(Files.exists(part), "part file should be deleted after cleanup");
        }
    }

    @Test
    void cleanup_isIdempotent() {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        BatchPackageBuilder.BatchPackage batchPackage = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());
        batchPackage.cleanup();

        // when / then — second call must not throw
        assertDoesNotThrow(batchPackage::cleanup);
    }
}
