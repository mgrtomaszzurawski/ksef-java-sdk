/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Internal helper that turns a list of raw invoice XML byte arrays into the wire-level
 * batch package — encrypted ZIP split into part files on disk, plus {@link BatchFileSpec}
 * metadata ready for the open-batch-session request.
 *
 * <p>The package is built using streaming I/O via temp files to support the full KSeF
 * limit of 5 GB total. Two sequential passes:
 * <ol>
 *   <li>Stream invoice bytes into a ZIP temp file (one entry per invoice, named by its
 *       SHA-256 hash).</li>
 *   <li>Stream the ZIP through AES-CBC encryption and split into part files of
 *       {@code maxPartSize} bytes, computing the full-file SHA-256 and a per-part SHA-256
 *       in the same pass.</li>
 * </ol>
 *
 * <p>The temporary ZIP file is deleted immediately after the encrypt-and-split pass.
 * Part files remain until the consumer (typically {@link KsefBatchSession#close()})
 * calls {@link BatchPackage#cleanup()}.
 */
final class BatchPackageBuilder {

    private static final String SHA_256 = "SHA-256";
    private static final String INVOICE_ENTRY_PREFIX = "invoice-";
    private static final String INVOICE_ENTRY_SUFFIX = ".xml";
    private static final long DEFAULT_MAX_PART_SIZE = 100L * 1024L * 1024L;
    private static final int MAX_PARTS = 50;
    private static final long MAX_FILE_SIZE = 5_000_000_000L;
    private static final int STREAM_BUFFER_BYTES = 64 * 1024;
    private static final String TEMP_PREFIX_ZIP = "ksef-batch-zip-";
    private static final String TEMP_PREFIX_PART = "ksef-batch-part-";
    private static final String TEMP_SUFFIX = ".bin";
    private static final String ERR_NULL_INVOICES = "invoices must not be null";
    private static final String ERR_EMPTY_INVOICES = "invoices must not be empty";
    private static final String ERR_NULL_INVOICE = "invoice bytes must not be null";
    private static final String ERR_EMPTY_INVOICE = "invoice bytes must not be empty";
    private static final String ERR_TOO_MANY_PARTS = "encrypted ZIP exceeds the 50-part limit";
    private static final String ERR_FILE_TOO_LARGE = "encrypted ZIP exceeds the 5GB size limit";
    private static final String ERR_HASH_ALGORITHM = "SHA-256 algorithm not available";
    private static final String ERR_AES_FINALIZE = "Failed to finalize AES encryption";
    private static final String ERR_BUILD = "Failed to build batch package";

    private BatchPackageBuilder() {
    }

    /**
     * Build the encrypted batch package with the default part size of 100 MB.
     */
    static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector) {
        return build(invoices, aesKey, initVector, DEFAULT_MAX_PART_SIZE);
    }

    /**
     * Build the encrypted batch package with a custom max part size (used by tests).
     *
     * @param invoices invoice XML byte arrays (one per ZIP entry)
     * @param aesKey 32-byte AES-256 key (session key)
     * @param initVector 16-byte AES-CBC IV
     * @param maxPartSize maximum size per encrypted part in bytes
     * @return the package — caller must call {@link BatchPackage#cleanup()} when done
     */
    static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector,
                              long maxPartSize) {
        Objects.requireNonNull(invoices, ERR_NULL_INVOICES);
        if (invoices.isEmpty()) {
            throw new IllegalArgumentException(ERR_EMPTY_INVOICES);
        }
        Path zipFile = null;
        List<Path> partFiles = new ArrayList<>();
        try {
            zipFile = writeZipToTempFile(invoices);
            EncryptResult enc = encryptAndSplit(zipFile, aesKey, initVector, maxPartSize, partFiles);
            return new BatchPackage(enc.spec, List.copyOf(partFiles));
        } catch (RuntimeException | IOException ex) {
            cleanupQuietly(partFiles);
            throw (ex instanceof RuntimeException re)
                    ? re
                    : new IllegalStateException(ERR_BUILD, ex);
        } finally {
            if (zipFile != null) {
                deleteQuietly(zipFile);
            }
        }
    }

    private static Path writeZipToTempFile(List<byte[]> invoices) throws IOException {
        Path zipFile = Files.createTempFile(TEMP_PREFIX_ZIP, TEMP_SUFFIX);
        zipFile.toFile().deleteOnExit();
        try (OutputStream out = Files.newOutputStream(zipFile);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            int ordinal = 1;
            for (byte[] invoice : invoices) {
                Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
                if (invoice.length == 0) {
                    throw new IllegalArgumentException(ERR_EMPTY_INVOICE);
                }
                ZipEntry entry = new ZipEntry(INVOICE_ENTRY_PREFIX + ordinal + INVOICE_ENTRY_SUFFIX);
                zip.putNextEntry(entry);
                zip.write(invoice);
                zip.closeEntry();
                ordinal++;
            }
        }
        return zipFile;
    }

    /**
     * Stream the ZIP through AES encryption, splitting the ciphertext into
     * {@code maxPartSize}-bounded files. Computes both the full-file SHA-256 and a
     * per-part SHA-256 in the same pass.
     */
    private static EncryptResult encryptAndSplit(Path zipFile, byte[] aesKey, byte[] initVector,
                                                 long maxPartSize, List<Path> partFiles)
            throws IOException {
        Cipher cipher = CryptoService.newAesEncryptCipher(aesKey, initVector);
        MessageDigest fullDigest = newSha256();

        long totalEncrypted = 0;
        List<BatchFileSpec.Part> partSpecs = new ArrayList<>();

        PartWriter partWriter = new PartWriter(maxPartSize, partFiles, partSpecs);
        try (InputStream zipIn = Files.newInputStream(zipFile)) {
            byte[] buffer = new byte[STREAM_BUFFER_BYTES];
            int read;
            while ((read = zipIn.read(buffer)) > 0) {
                byte[] encChunk = cipher.update(buffer, 0, read);
                if (encChunk != null && encChunk.length > 0) {
                    fullDigest.update(encChunk);
                    totalEncrypted += encChunk.length;
                    failIfTooLarge(totalEncrypted);
                    partWriter.write(encChunk);
                }
            }
            byte[] finalChunk;
            try {
                finalChunk = cipher.doFinal();
            } catch (BadPaddingException | IllegalBlockSizeException ex) {
                throw new KsefCryptoException(ERR_AES_FINALIZE, ex);
            }
            if (finalChunk != null && finalChunk.length > 0) {
                fullDigest.update(finalChunk);
                totalEncrypted += finalChunk.length;
                failIfTooLarge(totalEncrypted);
                partWriter.write(finalChunk);
            }
        }
        partWriter.finish();

        if (partSpecs.size() > MAX_PARTS) {
            throw new IllegalStateException(ERR_TOO_MANY_PARTS);
        }
        BatchFileSpec spec = new BatchFileSpec(totalEncrypted, fullDigest.digest(), partSpecs);
        return new EncryptResult(spec);
    }

    private static void failIfTooLarge(long totalEncrypted) {
        if (totalEncrypted > MAX_FILE_SIZE) {
            throw new IllegalStateException(ERR_FILE_TOO_LARGE);
        }
    }

    /**
     * Splits a stream of encrypted bytes into part temp files, computing per-part SHA-256.
     */
    private static final class PartWriter {

        private final long maxPartSize;
        private final List<Path> partFiles;
        private final List<BatchFileSpec.Part> partSpecs;
        private DigestOutputStream currentOut;
        private Path currentPath;
        private long currentSize;
        private int ordinal;

        PartWriter(long maxPartSize, List<Path> partFiles, List<BatchFileSpec.Part> partSpecs) {
            this.maxPartSize = maxPartSize;
            this.partFiles = partFiles;
            this.partSpecs = partSpecs;
        }

        void write(byte[] chunk) throws IOException {
            int offset = 0;
            while (offset < chunk.length) {
                if (currentOut == null) {
                    openNewPart();
                }
                long remaining = maxPartSize - currentSize;
                int writeLen = (int) Math.min(remaining, chunk.length - offset);
                currentOut.write(chunk, offset, writeLen);
                currentSize += writeLen;
                offset += writeLen;
                if (currentSize >= maxPartSize) {
                    finalizeCurrentPart();
                }
            }
        }

        void finish() throws IOException {
            if (currentOut != null) {
                finalizeCurrentPart();
            }
        }

        private void openNewPart() throws IOException {
            currentPath = Files.createTempFile(TEMP_PREFIX_PART, TEMP_SUFFIX);
            currentPath.toFile().deleteOnExit();
            OutputStream raw = Files.newOutputStream(currentPath);
            currentOut = new DigestOutputStream(raw, newSha256());
            currentSize = 0;
            ordinal++;
        }

        private void finalizeCurrentPart() throws IOException {
            currentOut.close();
            byte[] hash = currentOut.getMessageDigest().digest();
            partFiles.add(currentPath);
            partSpecs.add(new BatchFileSpec.Part(ordinal, currentSize, hash));
            currentOut = null;
            currentPath = null;
            currentSize = 0;
        }
    }

    private record EncryptResult(BatchFileSpec spec) { }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException ex) {
            throw new KsefCryptoException(ERR_HASH_ALGORITHM, ex);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort — temp file will be removed on JVM exit
        }
    }

    private static void cleanupQuietly(List<Path> paths) {
        for (Path path : paths) {
            deleteQuietly(path);
        }
    }

    /**
     * Result of a successful build — references on-disk part files plus the
     * {@link BatchFileSpec} for the open-batch-session request.
     *
     * <p>Caller is responsible for invoking {@link #cleanup()} once the parts have been
     * uploaded (typically from {@link KsefBatchSession#close()}). Part files are also
     * registered with {@link java.io.File#deleteOnExit()} as a safety net.
     */
    record BatchPackage(BatchFileSpec spec, List<Path> partFiles) {

        BatchPackage {
            Objects.requireNonNull(spec, "spec must not be null");
            Objects.requireNonNull(partFiles, "partFiles must not be null");
            partFiles = List.copyOf(partFiles);
        }

        /** Delete all part temp files. Idempotent — safe to call multiple times. */
        void cleanup() {
            cleanupQuietly(partFiles);
        }
    }
}
