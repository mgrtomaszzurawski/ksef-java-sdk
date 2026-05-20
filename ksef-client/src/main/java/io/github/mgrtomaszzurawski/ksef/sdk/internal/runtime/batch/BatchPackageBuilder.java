/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.Nullable;

/**
 * Single-pass stream-through builder: invoice bytes → ZIP → SHA-256
 * (full) → fixed-size chunk rotator → AES-256-CBC encrypt per chunk
 * → SHA-256 (per-part) → part sink ({@link BatchPart.OnDiskPart} or
 * {@link BatchPart.InMemoryPart}).
 *
 * <p>No intermediate ZIP file. Peak disk usage equals the size of the
 * encrypted parts (≈ original batch size + AES padding, ≤16 bytes per
 * chunk), not 2× as in the previous two-pass implementation.
 *
 * <p>Per KSeF spec ({@code sesja-wsadowa.md}):
 * <ul>
 *   <li>Original ZIP split into chunks ≤ {@code maxPartSize} bytes
 *       <em>before</em> encryption.</li>
 *   <li>Each chunk encrypted independently with AES-256-CBC + PKCS#7
 *       (fresh {@code Cipher.doFinal()}).</li>
 *   <li>{@code BatchFileSpec.fileSize/fileHash} describe the
 *       <em>unencrypted</em> ZIP. Per-part {@code fileSize/fileHash}
 *       describe the <em>encrypted</em> bytes.</li>
 * </ul>
 *
 * @apiNote Module-internal — declared {@code public} only so
 * {@code KsefClient} can call into it. Package not exported via JPMS.
 *
 * @since 1.0.0
 */
public final class BatchPackageBuilder {

    private static final String SHA_256 = "SHA-256";
    private static final String INVOICE_ENTRY_PREFIX = "invoice-";
    private static final String INVOICE_ENTRY_SUFFIX = ".xml";
    /** Default max chunk size <em>before encryption</em>: 100 MB (per KSeF spec). */
    private static final long DEFAULT_MAX_PART_SIZE = 100L * 1024L * 1024L;
    /** REQ-SESS-41 — KSeF caps a single session at 10,000 invoices (online or batch). */
    private static final int MAX_INVOICES_PER_SESSION = KsefLimits.MAX_SESSION_INVOICES;
    private static final int MAX_PARTS = KsefLimits.MAX_BATCH_PARTS;
    private static final long MAX_FILE_SIZE = KsefLimits.MAX_BATCH_TOTAL_BYTES;
    private static final String TEMP_PREFIX_PART = "ksef-batch-part-";
    /** Public so orphan-cleanup at startup can match the prefix. */
    public static final String TEMP_PART_PREFIX = TEMP_PREFIX_PART;
    private static final String TEMP_SUFFIX = ".bin";
    private static final String ERR_NULL_INVOICES = "invoices must not be null";
    private static final String ERR_EMPTY_INVOICES = "invoices must not be empty";
    private static final String ERR_NULL_INVOICE = "invoice bytes must not be null";
    private static final String ERR_EMPTY_INVOICE = "invoice bytes must not be empty";
    private static final String ERR_TOO_MANY_PARTS = "ZIP exceeds the 50-part limit";
    private static final String ERR_FILE_TOO_LARGE = "ZIP exceeds the 5GB size limit";
    private static final String ERR_HASH_ALGORITHM = "SHA-256 algorithm not available";
    private static final String ERR_BUILD = "Failed to build batch package";
    private static final String ERR_INMEMORY_CAP =
            "Batch payload exceeds the in-memory assembly cap (limit=%d bytes)";
    private static final String ERR_NULL_MODE = "assemblyMode must not be null";

    private BatchPackageBuilder() { }

    /**
     * Build the encrypted batch package from in-heap invoice bytes
     * with the default {@link BatchAssemblyMode#onDisk()} mode.
     */
    public static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector) {
        return build(invoices, aesKey, initVector, DEFAULT_MAX_PART_SIZE, BatchAssemblyMode.onDisk());
    }

    /**
     * Build the encrypted batch package from in-heap invoice bytes
     * with the supplied {@link BatchAssemblyMode}.
     */
    public static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector,
                                      BatchAssemblyMode assemblyMode) {
        return build(invoices, aesKey, initVector, DEFAULT_MAX_PART_SIZE, assemblyMode);
    }

    /** Custom-chunk-size convenience overload defaulting to on-disk assembly. */
    public static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector,
                                      long maxPartSize) {
        return build(invoices, aesKey, initVector, maxPartSize, BatchAssemblyMode.onDisk());
    }

    /**
     * Build the encrypted batch package, streaming each invoice from
     * disk, with the default {@link BatchAssemblyMode#onDisk()} mode.
     */
    public static BatchPackage buildFromFiles(List<Path> invoiceFiles, byte[] aesKey, byte[] initVector) {
        return buildFromFiles(invoiceFiles, aesKey, initVector, DEFAULT_MAX_PART_SIZE, BatchAssemblyMode.onDisk());
    }

    /**
     * File-streaming variant with the supplied {@link BatchAssemblyMode}.
     */
    public static BatchPackage buildFromFiles(List<Path> invoiceFiles, byte[] aesKey, byte[] initVector,
                                                BatchAssemblyMode assemblyMode) {
        return buildFromFiles(invoiceFiles, aesKey, initVector, DEFAULT_MAX_PART_SIZE, assemblyMode);
    }

    /**
     * Build the encrypted batch package with a custom chunk size.
     */
    public static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector,
                                      long maxPartSize, BatchAssemblyMode assemblyMode) {
        Objects.requireNonNull(invoices, ERR_NULL_INVOICES);
        Objects.requireNonNull(assemblyMode, ERR_NULL_MODE);
        if (invoices.isEmpty()) {
            throw new IllegalArgumentException(ERR_EMPTY_INVOICES);
        }
        rejectOversizeInvoiceCount(invoices.size());
        return runPipeline(zip -> writeInvoiceBytes(zip, invoices),
                aesKey, initVector, maxPartSize, assemblyMode);
    }

    /**
     * File-streaming overload with custom chunk size.
     */
    public static BatchPackage buildFromFiles(List<Path> invoiceFiles, byte[] aesKey, byte[] initVector,
                                                long maxPartSize, BatchAssemblyMode assemblyMode) {
        Objects.requireNonNull(invoiceFiles, ERR_NULL_INVOICES);
        Objects.requireNonNull(assemblyMode, ERR_NULL_MODE);
        if (invoiceFiles.isEmpty()) {
            throw new IllegalArgumentException(ERR_EMPTY_INVOICES);
        }
        rejectOversizeInvoiceCount(invoiceFiles.size());
        return runPipeline(zip -> writeInvoiceFiles(zip, invoiceFiles),
                aesKey, initVector, maxPartSize, assemblyMode);
    }

    private static void rejectOversizeInvoiceCount(int count) {
        if (count > MAX_INVOICES_PER_SESSION) {
            throw new IllegalArgumentException(
                    "Batch contains " + count + " invoices but KSeF caps a single session at "
                            + MAX_INVOICES_PER_SESSION + " (REQ-SESS-41)");
        }
    }

    @FunctionalInterface
    private interface ZipWriter {
        void writeTo(ZipOutputStream zip) throws IOException;
    }

    private static BatchPackage runPipeline(ZipWriter writer, byte[] aesKey, byte[] initVector,
                                             long maxPartSize, BatchAssemblyMode mode) {
        MessageDigest fullDigest = newSha256();
        ChunkSink chunkSink = new ChunkSink(maxPartSize, aesKey, initVector, mode);
        try {
            try (DigestOutputStream digestStream = new DigestOutputStream(chunkSink, fullDigest);
                 ZipOutputStream zip = new ZipOutputStream(digestStream)) {
                writer.writeTo(zip);
            }
            chunkSink.flushFinalChunk();
            long totalRawSize = chunkSink.totalRawBytes();
            if (totalRawSize > MAX_FILE_SIZE) {
                chunkSink.cleanupAll();
                throw new IllegalStateException(ERR_FILE_TOO_LARGE);
            }
            List<BatchPart> parts = chunkSink.parts();
            if (parts.size() > MAX_PARTS) {
                chunkSink.cleanupAll();
                throw new IllegalStateException(ERR_TOO_MANY_PARTS);
            }
            List<BatchFileSpec.Part> partSpecs = new ArrayList<>(parts.size());
            for (BatchPart part : parts) {
                partSpecs.add(new BatchFileSpec.Part(part.ordinalNumber(), part.sizeBytes(), part.hash()));
            }
            BatchFileSpec spec = new BatchFileSpec(totalRawSize, fullDigest.digest(), partSpecs);
            return new BatchPackage(spec, parts);
        } catch (RuntimeException | IOException buildFailure) {
            chunkSink.cleanupAll();
            throw (buildFailure instanceof RuntimeException runtimeFailure)
                    ? runtimeFailure
                    : new IllegalStateException(ERR_BUILD, buildFailure);
        }
    }

    private static void writeInvoiceBytes(ZipOutputStream zip, List<byte[]> invoices) throws IOException {
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

    private static void writeInvoiceFiles(ZipOutputStream zip, List<Path> invoiceFiles) throws IOException {
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

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new KsefCryptoException(ERR_HASH_ALGORITHM, missingAlgorithm);
        }
    }

    /**
     * Pipeline tail: collects bytes into a fixed-size buffer; on each
     * full buffer encrypt + per-part hash + emit one {@link BatchPart}.
     */
    private static final class ChunkSink extends OutputStream {

        private final int chunkBufferSize;
        private byte @Nullable [] chunkBuffer;
        private final byte[] aesKey;
        private final byte[] initVector;
        private final BatchAssemblyMode mode;
        private final List<BatchPart> emitted = new ArrayList<>();
        private int chunkLen;
        private long totalRawBytes;
        private long totalInMemoryBytes;
        private int nextOrdinal = 1;
        private boolean closed;

        ChunkSink(long maxPartSize, byte[] aesKey, byte[] initVector, BatchAssemblyMode mode) {
            // Cap the chunking buffer against the in-memory cap so callers
            // who pick inMemory(50MB) do not get a 100MB-default chunk
            // buffer allocated regardless. For OnDisk mode the buffer
            // stays at maxPartSize (default 100 MB).
            //
            // Lazy allocation: chunkBuffer is allocated on the first write
            // (see ensureChunkBuffer) so a tiny ZIP that fits in << 100MB
            // never pays the 100MB allocation cost.
            long bufferSize = maxPartSize;
            if (mode instanceof BatchAssemblyMode.InMemory inMem) {
                bufferSize = Math.min(maxPartSize, inMem.maxBytes());
            }
            this.chunkBufferSize = (int) Math.min(bufferSize, Integer.MAX_VALUE);
            this.aesKey = aesKey.clone();
            this.initVector = initVector.clone();
            this.mode = mode;
        }

        private byte[] ensureChunkBuffer() {
            byte[] buffer = chunkBuffer;
            if (buffer == null) {
                buffer = new byte[chunkBufferSize];
                chunkBuffer = buffer;
            }
            return buffer;
        }

        @Override
        public void write(int unsignedByte) throws IOException {
            byte[] singleByteBuffer = { (byte) unsignedByte };
            write(singleByteBuffer, 0, 1);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            byte[] activeBuffer = ensureChunkBuffer();
            int remaining = length;
            int from = offset;
            while (remaining > 0) {
                int free = activeBuffer.length - chunkLen;
                int copy = Math.min(free, remaining);
                System.arraycopy(buffer, from, activeBuffer, chunkLen, copy);
                chunkLen += copy;
                totalRawBytes += copy;
                from += copy;
                remaining -= copy;
                if (chunkLen == activeBuffer.length) {
                    emitChunk();
                }
            }
        }

        void flushFinalChunk() throws IOException {
            if (chunkLen > 0) {
                emitChunk();
            }
        }

        private void emitChunk() throws IOException {
            byte[] activeBuffer = ensureChunkBuffer();
            byte[] plaintext = chunkLen == activeBuffer.length
                    ? activeBuffer
                    : Arrays.copyOf(activeBuffer, chunkLen);
            byte[] ciphertext = CryptoService.encryptAes(plaintext, aesKey, initVector);
            byte[] partHash = hashOf(ciphertext);
            BatchPart part;
            if (mode instanceof BatchAssemblyMode.OnDisk onDisk) {
                part = writeToDisk(onDisk, ciphertext, partHash);
            } else if (mode instanceof BatchAssemblyMode.InMemory inMemory) {
                part = heldInMemory(inMemory, ciphertext, partHash);
            } else {
                throw new IllegalStateException("Unknown BatchAssemblyMode: " + mode.getClass());
            }
            emitted.add(part);
            chunkLen = 0;
        }

        private BatchPart writeToDisk(BatchAssemblyMode.OnDisk onDisk, byte[] ciphertext, byte[] partHash)
                throws IOException {
            Path partPath = createOwnerOnlyTempFile(onDisk.tempDirectory());
            partPath.toFile().deleteOnExit();
            Files.write(partPath, ciphertext);
            BatchPart.OnDiskPart part = new BatchPart.OnDiskPart(nextOrdinal, ciphertext.length, partHash, partPath);
            nextOrdinal++;
            return part;
        }

        private BatchPart heldInMemory(BatchAssemblyMode.InMemory inMemory, byte[] ciphertext, byte[] partHash) {
            totalInMemoryBytes += ciphertext.length;
            if (totalInMemoryBytes > inMemory.maxBytes()) {
                throw new IllegalStateException(String.format(ERR_INMEMORY_CAP, inMemory.maxBytes()));
            }
            BatchPart.InMemoryPart part = new BatchPart.InMemoryPart(nextOrdinal, partHash, ciphertext);
            nextOrdinal++;
            return part;
        }

        private static byte[] hashOf(byte[] bytes) {
            MessageDigest digest = newSha256();
            digest.update(bytes);
            return digest.digest();
        }

        /**
         * Create a temp file with owner-only POSIX permissions where supported.
         * Files hold AES-CBC ciphertext only — keys never on disk — but
         * defense-in-depth: prevent accidental disclosure on multi-user hosts.
         * Falls back to default permissions on non-POSIX filesystems (Windows).
         */
        private static Path createOwnerOnlyTempFile(@Nullable Path tempDirectory) throws IOException {
            try {
                java.nio.file.attribute.FileAttribute<?> ownerOnly =
                        java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                                java.util.EnumSet.of(
                                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
                if (tempDirectory == null) {
                    return Files.createTempFile(TEMP_PREFIX_PART, TEMP_SUFFIX, ownerOnly);
                }
                return Files.createTempFile(tempDirectory, TEMP_PREFIX_PART, TEMP_SUFFIX, ownerOnly);
            } catch (UnsupportedOperationException nonPosix) {
                if (tempDirectory == null) {
                    return Files.createTempFile(TEMP_PREFIX_PART, TEMP_SUFFIX);
                }
                return Files.createTempFile(tempDirectory, TEMP_PREFIX_PART, TEMP_SUFFIX);
            }
        }

        long totalRawBytes() {
            return totalRawBytes;
        }

        List<BatchPart> parts() {
            return List.copyOf(emitted);
        }

        void cleanupAll() {
            for (BatchPart part : emitted) {
                part.cleanup();
            }
            emitted.clear();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            // Final-chunk flush is driven by the caller via flushFinalChunk()
            // so we do not auto-flush here (avoids double-flush when the
            // ZipOutputStream above us also closes).
        }
    }

    /**
     * Result of a successful build — references encrypted parts in
     * one of the {@link BatchAssemblyMode}s plus the {@link BatchFileSpec}
     * describing the unencrypted ZIP.
     *
     * <p>Caller must invoke {@link #cleanup()} once parts have been
     * uploaded — typically from the internal batch submission flow.
     */
    public record BatchPackage(BatchFileSpec spec, List<BatchPart> parts) {

        public BatchPackage {
            Objects.requireNonNull(spec, "spec must not be null");
            Objects.requireNonNull(parts, "parts must not be null");
            parts = List.copyOf(parts);
        }

        /** Delete all on-disk part files and release in-memory part references. Idempotent. */
        public void cleanup() {
            for (BatchPart part : parts) {
                part.cleanup();
            }
        }

        /**
         * Read the encrypted bytes for part {@code index}, regardless of
         * whether the part lives on disk or in heap. Defensive copy on
         * the in-memory path.
         *
         * <p><strong>Test-only helper.</strong> Production batch upload
         * uses {@code BodyPublishers.ofFile(...)} which streams from disk
         * without materialising the full part. This method exists only
         * for unit-test assertions on small fixture parts; do not call it
         * from production code paths against real 100 MiB parts.
         */
        public byte[] readPartBytes(int index) throws IOException {
            BatchPart part = parts.get(index);
            if (part instanceof BatchPart.OnDiskPart onDisk) {
                return Files.readAllBytes(onDisk.path());
            }
            if (part instanceof BatchPart.InMemoryPart inMem) {
                return inMem.bytes();
            }
            throw new IllegalStateException("Unknown BatchPart subtype: " + part.getClass());
        }
    }
}
