/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchFileSpec;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
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
 * <p>Per KSeF spec ({@code sesja-wsadowa.md}):
 * <ul>
 *   <li>The original ZIP is split into chunks of at most {@code maxPartSize} bytes
 *       <em>before encryption</em>.</li>
 *   <li>Each chunk is encrypted independently with AES-256-CBC + PKCS#7 padding using
 *       the session AES key/IV. Encryption of each chunk is a fresh {@code Cipher.doFinal()}
 *       — every part has its own padding.</li>
 *   <li>{@code BatchFileSpec.fileSize} and {@code fileHash} describe the <em>original</em>
 *       (unencrypted) ZIP. KSeF reassembles parts after decrypting each one and verifies
 *       the resulting bytes match these values.</li>
 *   <li>Per-part {@code fileSize}/{@code fileHash} describe the <em>encrypted</em> part
 *       bytes (so KSeF can verify each upload before reassembly).</li>
 * </ul>
 *
 * <p>Two sequential streaming passes via temp files:
 * <ol>
 *   <li>Stream invoice bytes into a ZIP temp file.</li>
 *   <li>Stream the unencrypted ZIP, splitting it into chunks of {@code maxPartSize}
 *       bytes; encrypt each chunk separately, write the encrypted bytes to a part
 *       temp file, and compute both the full plaintext SHA-256 and the per-part
 *       encrypted SHA-256 in the same pass.</li>
 * </ol>
 *
 * <p>The temporary ZIP file is deleted immediately after the encrypt-and-split pass.
 * Part files remain until {@link BatchPackage#cleanup()} is called.
 *
 * @apiNote Module-internal — declared {@code public} only so {@code KsefClient}
 * (in package {@code sdk}) can call into it. Its package
 * {@code sdk.internal.batch} is intentionally not exported via {@code module-info.java};
 * consumers cannot reach this type.
 */
public final class BatchPackageBuilder {

    private static final String SHA_256 = "SHA-256";
    private static final String INVOICE_ENTRY_PREFIX = "invoice-";
    private static final String INVOICE_ENTRY_SUFFIX = ".xml";
    /** Default max chunk size <em>before encryption</em>: 100 MB (per KSeF spec). */
    private static final long DEFAULT_MAX_PART_SIZE = 100L * 1024L * 1024L;
    /** REQ-SESS-41 — KSeF caps a single session at 10,000 invoices (online or batch). */
    private static final int MAX_INVOICES_PER_SESSION = 10_000;
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
    private static final String ERR_TOO_MANY_PARTS = "ZIP exceeds the 50-part limit";
    private static final String ERR_FILE_TOO_LARGE = "ZIP exceeds the 5GB size limit";
    private static final String ERR_HASH_ALGORITHM = "SHA-256 algorithm not available";
    private static final String ERR_BUILD = "Failed to build batch package";

    private BatchPackageBuilder() {
    }

    /**
     * Build the encrypted batch package with the default chunk size (100 MB before encryption).
     */
    public static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector) {
        return build(invoices, aesKey, initVector, DEFAULT_MAX_PART_SIZE);
    }

    /**
     * File-streaming variant — Codex round-9 manual-validation A.4.2.
     */
    public static BatchPackage buildFromFiles(List<Path> invoiceFiles, byte[] aesKey, byte[] initVector) {
        return buildFromFiles(invoiceFiles, aesKey, initVector, DEFAULT_MAX_PART_SIZE);
    }

    /**
     * Build the encrypted batch package with a custom max chunk size (used by tests).
     *
     * @param invoices invoice XML byte arrays (one per ZIP entry)
     * @param aesKey 32-byte AES-256 key (session key)
     * @param initVector 16-byte AES-CBC IV
     * @param maxPartSize maximum size per chunk <em>before encryption</em>, in bytes
     * @return the package — caller must call {@link BatchPackage#cleanup()} when done
     */
    public static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector,
                              long maxPartSize) {
        Objects.requireNonNull(invoices, ERR_NULL_INVOICES);
        if (invoices.isEmpty()) {
            throw new IllegalArgumentException(ERR_EMPTY_INVOICES);
        }
        // REQ-SESS-41 — KSeF enforces a 10,000-invoice-per-session cap per
        // ksef-docs/faktury/weryfikacja-faktury.md:50. Fail-fast in the SDK
        // so the caller doesn't waste an upload that the server would
        // reject anyway.
        if (invoices.size() > MAX_INVOICES_PER_SESSION) {
            throw new IllegalArgumentException(
                    "Batch contains " + invoices.size() + " invoices but KSeF caps a single session at "
                            + MAX_INVOICES_PER_SESSION + " (REQ-SESS-41)");
        }
        Path zipFile = null;
        List<Path> partFiles = new ArrayList<>();
        try {
            zipFile = writeZipToTempFile(invoices);
            BatchFileSpec spec = splitAndEncrypt(zipFile, aesKey, initVector, maxPartSize, partFiles);
            return new BatchPackage(spec, List.copyOf(partFiles));
        } catch (RuntimeException | IOException buildFailure) {
            cleanupQuietly(partFiles);
            throw (buildFailure instanceof RuntimeException runtimeFailure)
                    ? runtimeFailure
                    : new IllegalStateException(ERR_BUILD, buildFailure);
        } finally {
            if (zipFile != null) {
                deleteQuietly(zipFile);
            }
        }
    }

    /**
     * File-streaming overload — Codex round-9 manual-validation A.4.2.
     * Each invoice is streamed straight from disk into the ZIP, avoiding
     * an in-heap {@code byte[]} per file. Use this for large batches
     * (up to 10 000 invoices per session, REQ-SESS-41) so peak heap stays
     * bounded by the chunk-encryption buffer rather than scaling with the
     * total payload size.
     */
    public static BatchPackage buildFromFiles(List<Path> invoiceFiles, byte[] aesKey, byte[] initVector,
                                                long maxPartSize) {
        Objects.requireNonNull(invoiceFiles, ERR_NULL_INVOICES);
        if (invoiceFiles.isEmpty()) {
            throw new IllegalArgumentException(ERR_EMPTY_INVOICES);
        }
        if (invoiceFiles.size() > MAX_INVOICES_PER_SESSION) {
            throw new IllegalArgumentException(
                    "Batch contains " + invoiceFiles.size() + " invoices but KSeF caps a single session at "
                            + MAX_INVOICES_PER_SESSION + " (REQ-SESS-41)");
        }
        Path zipFile = null;
        List<Path> partFiles = new ArrayList<>();
        try {
            zipFile = writeZipFromFilesToTempFile(invoiceFiles);
            BatchFileSpec spec = splitAndEncrypt(zipFile, aesKey, initVector, maxPartSize, partFiles);
            return new BatchPackage(spec, List.copyOf(partFiles));
        } catch (RuntimeException | IOException buildFailure) {
            cleanupQuietly(partFiles);
            throw (buildFailure instanceof RuntimeException runtimeFailure)
                    ? runtimeFailure
                    : new IllegalStateException(ERR_BUILD, buildFailure);
        } finally {
            if (zipFile != null) {
                deleteQuietly(zipFile);
            }
        }
    }

    private static Path writeZipFromFilesToTempFile(List<Path> invoiceFiles) throws IOException {
        Path zipFile = Files.createTempFile(TEMP_PREFIX_ZIP, TEMP_SUFFIX);
        zipFile.toFile().deleteOnExit();
        try (OutputStream out = Files.newOutputStream(zipFile);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            int ordinal = 1;
            for (Path invoiceFile : invoiceFiles) {
                Objects.requireNonNull(invoiceFile, ERR_NULL_INVOICE);
                long size = Files.size(invoiceFile);
                if (size == 0) {
                    throw new IllegalArgumentException(ERR_EMPTY_INVOICE);
                }
                ZipEntry entry = new ZipEntry(INVOICE_ENTRY_PREFIX + ordinal + INVOICE_ENTRY_SUFFIX);
                zip.putNextEntry(entry);
                Files.copy(invoiceFile, zip);
                zip.closeEntry();
                ordinal++;
            }
        }
        return zipFile;
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
     * Read the ZIP file in chunks of {@code maxPartSize} bytes; for each chunk: compute
     * the plaintext SHA-256 (combined across all chunks), encrypt the chunk independently,
     * and write the encrypted bytes to a part temp file with their own per-part SHA-256.
     */
    private static BatchFileSpec splitAndEncrypt(Path zipFile, byte[] aesKey, byte[] initVector,
                                                 long maxPartSize, List<Path> partFiles)
            throws IOException {
        long totalRawSize = Files.size(zipFile);
        if (totalRawSize > MAX_FILE_SIZE) {
            throw new IllegalStateException(ERR_FILE_TOO_LARGE);
        }
        MessageDigest fullDigest = newSha256();
        List<BatchFileSpec.Part> partSpecs = new ArrayList<>();

        try (InputStream zipIn = Files.newInputStream(zipFile)) {
            byte[] readBuffer = new byte[STREAM_BUFFER_BYTES];
            int ordinal = 0;
            byte[] chunkBuffer = new byte[(int) Math.min(maxPartSize, Integer.MAX_VALUE)];
            int chunkLen = 0;

            while (true) {
                int wanted = chunkBuffer.length - chunkLen;
                int read = zipIn.read(chunkBuffer, chunkLen, Math.min(wanted, readBuffer.length));
                if (read < 0) {
                    break;
                }
                chunkLen += read;
                fullDigest.update(chunkBuffer, chunkLen - read, read);
                if (chunkLen == chunkBuffer.length) {
                    ordinal++;
                    partSpecs.add(encryptChunkToPart(chunkBuffer, chunkLen, aesKey, initVector,
                            ordinal, partFiles));
                    chunkLen = 0;
                }
            }
            // final partial chunk (could be empty if total raw size is multiple of maxPartSize)
            if (chunkLen > 0) {
                ordinal++;
                partSpecs.add(encryptChunkToPart(chunkBuffer, chunkLen, aesKey, initVector,
                        ordinal, partFiles));
            }
        }

        if (partSpecs.size() > MAX_PARTS) {
            throw new IllegalStateException(ERR_TOO_MANY_PARTS);
        }
        // BatchFileSpec.fileSize/fileHash describe the *unencrypted* ZIP (per KSeF spec)
        return new BatchFileSpec(totalRawSize, fullDigest.digest(), partSpecs);
    }

    /**
     * Encrypt a single chunk independently and write the ciphertext to a part temp file,
     * computing the per-part SHA-256 of the encrypted bytes.
     */
    private static BatchFileSpec.Part encryptChunkToPart(byte[] chunk, int chunkLen,
                                                        byte[] aesKey, byte[] initVector,
                                                        int ordinal, List<Path> partFiles)
            throws IOException {
        byte[] encrypted = CryptoService.encryptAes(
                chunkLen == chunk.length ? chunk : java.util.Arrays.copyOf(chunk, chunkLen),
                aesKey, initVector);
        Path partPath = Files.createTempFile(TEMP_PREFIX_PART, TEMP_SUFFIX);
        partPath.toFile().deleteOnExit();
        MessageDigest partDigest = newSha256();
        try (OutputStream raw = Files.newOutputStream(partPath);
             DigestOutputStream out = new DigestOutputStream(raw, partDigest)) {
            out.write(encrypted);
        }
        partFiles.add(partPath);
        return new BatchFileSpec.Part(ordinal, encrypted.length, partDigest.digest());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new KsefCryptoException(ERR_HASH_ALGORITHM, missingAlgorithm);
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
     * Result of a successful build — references on-disk encrypted part files plus the
     * {@link BatchFileSpec} describing the <em>unencrypted</em> ZIP, ready for the
     * open-batch-session request.
     *
     * <p>Caller must invoke {@link #cleanup()} once parts have been uploaded
     * (typically from {@link KsefBatchSession#close()}).
     */
    public record BatchPackage(BatchFileSpec spec, List<Path> partFiles) {

        public BatchPackage {
            Objects.requireNonNull(spec, "spec must not be null");
            Objects.requireNonNull(partFiles, "partFiles must not be null");
            partFiles = List.copyOf(partFiles);
        }

        /** Delete all part temp files. Idempotent — safe to call multiple times. */
        public void cleanup() {
            cleanupQuietly(partFiles);
        }
    }
}
