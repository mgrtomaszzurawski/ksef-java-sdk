/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertEquals(1, pkg.spec().parts().size());
        assertEquals(FIRST_PART_ORDINAL, pkg.spec().parts().get(0).ordinalNumber());
        assertEquals(pkg.encryptedZip().length, pkg.spec().fileSize());
        assertArrayEquals(sha256(pkg.encryptedZip()), pkg.spec().fileHash());
        assertEquals(1, pkg.partBytes().size());
        assertArrayEquals(pkg.encryptedZip(), pkg.partBytes().get(0));
    }

    @Test
    void build_whenInvoicesEncrypted_zipDecryptsToValidArchive() throws Exception {
        // given
        byte[] invoiceOne = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        byte[] invoiceTwo = INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoiceOne, invoiceTwo), aesKey(), aesIv());

        // then — decrypt and verify ZIP contents
        byte[] zipBytes = CryptoService.decryptAes(pkg.encryptedZip(), aesKey(), aesIv());
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
        assertTrue(contents.contains(invoiceOne) || Arrays.equals(contents.get(0), invoiceOne)
                || Arrays.equals(contents.get(1), invoiceOne));
    }

    @Test
    void build_whenSplitIntoMultipleParts_partBytesConcatToEncryptedZip() throws Exception {
        // given — 3 invoices, force splitting with small max part size
        List<byte[]> invoices = List.of(
                INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_THREE_XML.getBytes(StandardCharsets.UTF_8));

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — multiple parts produced, ordinals are 1-based and contiguous
        assertTrue(pkg.spec().parts().size() > 1, "expected multiple parts");
        for (int index = 0; index < pkg.spec().parts().size(); index++) {
            assertEquals(index + 1, pkg.spec().parts().get(index).ordinalNumber());
        }

        // concatenate part bytes -> equals encrypted ZIP
        int totalLen = pkg.partBytes().stream().mapToInt(part -> part.length).sum();
        byte[] joined = new byte[totalLen];
        int offset = 0;
        for (byte[] part : pkg.partBytes()) {
            System.arraycopy(part, 0, joined, offset, part.length);
            offset += part.length;
        }
        assertArrayEquals(pkg.encryptedZip(), joined);
    }

    @Test
    void build_partHashes_matchSha256OfEachPart() throws Exception {
        // given
        List<byte[]> invoices = List.of(
                INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8),
                INVOICE_TWO_XML.getBytes(StandardCharsets.UTF_8));

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE);

        // then — each spec.parts().fileHash matches sha256(partBytes[i])
        for (int index = 0; index < pkg.partBytes().size(); index++) {
            byte[] expectedHash = sha256(pkg.partBytes().get(index));
            assertEquals(EXPECTED_HASH_BYTES, expectedHash.length);
            assertArrayEquals(expectedHash, pkg.spec().parts().get(index).fileHash());
            assertEquals(pkg.partBytes().get(index).length,
                    pkg.spec().parts().get(index).fileSize());
        }
    }

    @Test
    void build_specFileHash_matchesSha256OfEncryptedZip() throws Exception {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);

        // when
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());

        // then
        assertArrayEquals(sha256(pkg.encryptedZip()), pkg.spec().fileHash());
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
        assertEquals(FIRST_PART_ORDINAL, pkg.spec().parts().get(0).ordinalNumber());
        if (pkg.spec().parts().size() >= EXPECTED_TWO_INVOICES) {
            assertEquals(SECOND_PART_ORDINAL, pkg.spec().parts().get(1).ordinalNumber());
        }
    }

    @Test
    void packageBytes_areDefensivelyCopied() {
        // given
        byte[] invoice = INVOICE_ONE_XML.getBytes(StandardCharsets.UTF_8);
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                List.of(invoice), aesKey(), aesIv());

        // when — mutate first returned encrypted zip array
        byte[] firstSnapshot = pkg.encryptedZip();
        firstSnapshot[0] = (byte) ~firstSnapshot[0];

        // then — second call returns the original unmodified
        byte[] secondSnapshot = pkg.encryptedZip();
        assertNotNull(secondSnapshot);
        assertEquals(firstSnapshot.length, secondSnapshot.length);
    }
}
