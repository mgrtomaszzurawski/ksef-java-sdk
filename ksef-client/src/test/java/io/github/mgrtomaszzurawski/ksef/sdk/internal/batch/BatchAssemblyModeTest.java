/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchAssemblyMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchPart;
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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E coverage for the three {@link BatchAssemblyMode} variants. Walks
 * the full assembly pipeline (zip → digest → chunk → encrypt) for each
 * mode, verifies the encrypted parts decrypt back to the original ZIP,
 * and asserts the mode-specific properties (file paths created in the
 * supplied directory, in-memory cap fail-fast).
 */
class BatchAssemblyModeTest {

    private static final byte AES_KEY_FILL = (byte) 0xAA;
    private static final byte AES_IV_FILL = (byte) 0xBB;
    private static final int AES_KEY_BYTES = 32;
    private static final int AES_IV_BYTES = 16;
    private static final int SHA_256_BYTES = 32;
    private static final long SMALL_PART_SIZE = 64L;
    private static final String INVOICE_A = "<Invoice><Number>A</Number></Invoice>";
    private static final String INVOICE_B = "<Invoice><Number>B</Number></Invoice>";
    private static final String INVOICE_C = "<Invoice><Number>C</Number></Invoice>";

    @Test
    void onDiskDefault_writesPartsToJavaIoTmpdir() throws Exception {
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                threeInvoices(), aesKey(), aesIv(),
                SMALL_PART_SIZE, BatchAssemblyMode.onDisk());
        try {
            Path tmpdir = Path.of(System.getProperty("java.io.tmpdir"));
            for (BatchPart part : pkg.parts()) {
                BatchPart.OnDiskPart onDisk = assertInstanceOf(BatchPart.OnDiskPart.class, part);
                assertEquals(tmpdir.toAbsolutePath(), onDisk.path().getParent().toAbsolutePath());
                assertTrue(Files.exists(onDisk.path()), "part file must exist before cleanup");
            }
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void onDiskCustomPath_writesPartsToSuppliedDirectory(@TempDir Path customDir) throws Exception {
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                threeInvoices(), aesKey(), aesIv(),
                SMALL_PART_SIZE, BatchAssemblyMode.onDisk(customDir));
        try {
            for (BatchPart part : pkg.parts()) {
                BatchPart.OnDiskPart onDisk = assertInstanceOf(BatchPart.OnDiskPart.class, part);
                assertEquals(customDir.toAbsolutePath(), onDisk.path().getParent().toAbsolutePath());
            }
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void onDisk_cleanup_deletesAllPartFiles(@TempDir Path customDir) {
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                threeInvoices(), aesKey(), aesIv(),
                SMALL_PART_SIZE, BatchAssemblyMode.onDisk(customDir));
        List<Path> paths = new ArrayList<>();
        for (BatchPart part : pkg.parts()) {
            paths.add(((BatchPart.OnDiskPart) part).path());
        }

        pkg.cleanup();

        for (Path path : paths) {
            assertFalse(Files.exists(path), "cleanup must delete part file: " + path);
        }
    }

    @Test
    void inMemory_holdsBytesInHeap_andDecryptsToOriginalZip() throws Exception {
        BatchPackageBuilder.BatchPackage pkg = BatchPackageBuilder.build(
                threeInvoices(), aesKey(), aesIv(),
                SMALL_PART_SIZE, BatchAssemblyMode.inMemory(10_000_000));
        try {
            for (BatchPart part : pkg.parts()) {
                BatchPart.InMemoryPart inMem = assertInstanceOf(BatchPart.InMemoryPart.class, part);
                assertEquals(part.sizeBytes(), inMem.bytes().length);
            }
            byte[] zip = decryptAndConcat(pkg);
            assertEquals(zip.length, pkg.spec().fileSize());
            assertArrayEquals(sha256(zip), pkg.spec().fileHash());

            // Recover the entries to confirm the pipeline is byte-equivalent.
            List<String> recovered = new ArrayList<>();
            try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zip))) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    recovered.add(new String(zin.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            assertEquals(3, recovered.size());
            assertTrue(recovered.contains(INVOICE_A));
            assertTrue(recovered.contains(INVOICE_B));
            assertTrue(recovered.contains(INVOICE_C));
        } finally {
            pkg.cleanup();
        }
    }

    @Test
    void inMemory_capExceeded_failsFast() {
        // Tiny cap + small chunk size guarantees the second chunk's emit
        // pushes total above the cap.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> BatchPackageBuilder.build(
                        threeInvoices(), aesKey(), aesIv(),
                        SMALL_PART_SIZE, BatchAssemblyMode.inMemory(50)));
        assertTrue(ex.getMessage().contains("in-memory assembly cap"),
                "exception must explain the cap, was: " + ex.getMessage());
    }

    @Test
    void inMemory_negativeCap_rejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> BatchAssemblyMode.inMemory(-1));
        assertThrows(IllegalArgumentException.class,
                () -> BatchAssemblyMode.inMemory(0));
    }

    @Test
    void onDisk_nullPath_rejectedAtConstruction() {
        assertThrows(NullPointerException.class,
                () -> BatchAssemblyMode.onDisk(null));
    }

    @Test
    void specHashes_areIdenticalAcrossModes() throws Exception {
        // Stream-through pipeline must produce byte-identical results
        // regardless of where the encrypted parts land. Hashes anchored
        // to plaintext (full-zip + per-part-ciphertext) cannot drift
        // between OnDisk and InMemory.
        List<byte[]> invoices = threeInvoices();
        BatchPackageBuilder.BatchPackage diskPkg = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE, BatchAssemblyMode.onDisk());
        BatchPackageBuilder.BatchPackage memPkg = BatchPackageBuilder.build(
                invoices, aesKey(), aesIv(), SMALL_PART_SIZE, BatchAssemblyMode.inMemory(10_000_000));
        try {
            assertArrayEquals(diskPkg.spec().fileHash(), memPkg.spec().fileHash());
            assertEquals(diskPkg.spec().fileSize(), memPkg.spec().fileSize());
            assertEquals(diskPkg.parts().size(), memPkg.parts().size());
            for (int i = 0; i < diskPkg.parts().size(); i++) {
                assertArrayEquals(
                        diskPkg.parts().get(i).hash(),
                        memPkg.parts().get(i).hash(),
                        "part " + i + " hash must match across modes");
                assertEquals(
                        diskPkg.parts().get(i).sizeBytes(),
                        memPkg.parts().get(i).sizeBytes(),
                        "part " + i + " size must match across modes");
            }
        } finally {
            diskPkg.cleanup();
            memPkg.cleanup();
        }
    }

    private static List<byte[]> threeInvoices() {
        return List.of(
                INVOICE_A.getBytes(StandardCharsets.UTF_8),
                INVOICE_B.getBytes(StandardCharsets.UTF_8),
                INVOICE_C.getBytes(StandardCharsets.UTF_8));
    }

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
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    private static byte[] decryptAndConcat(BatchPackageBuilder.BatchPackage pkg) throws Exception {
        ByteArrayOutputStream concatenated = new ByteArrayOutputStream();
        for (int i = 0; i < pkg.parts().size(); i++) {
            byte[] encrypted = pkg.readPartBytes(i);
            byte[] decrypted = CryptoService.decryptAes(encrypted, aesKey(), aesIv());
            concatenated.write(decrypted);
        }
        return concatenated.toByteArray();
    }
}
