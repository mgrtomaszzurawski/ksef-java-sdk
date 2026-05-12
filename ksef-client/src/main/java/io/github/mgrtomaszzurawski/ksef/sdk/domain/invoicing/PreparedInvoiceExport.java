/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoiceDirectory;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackagePart;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionPollingTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle for an in-flight invoice export. Returned by
 * {@link InvoiceExport#prepare(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest, io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportScope)
 * client.invoices().export().prepare(...)}, this class:
 *
 * <ol>
 *   <li>retains the plaintext AES key and IV generated for the export so the
 *       returned package can be decrypted later;</li>
 *   <li>polls the server for the export status until terminal;</li>
 *   <li>downloads each {@link InvoicePackagePart}, verifies its SHA-256 hash
 *       against the server-supplied {@code partHash}, decrypts with the retained
 *       AES key + IV, concatenates the parts back into a ZIP archive, unzips,
 *       and exposes {@code _metadata.json} plus the per-invoice XML bytes.</li>
 * </ol>
 *
 * <p>The {@link #downloadAndDecrypt(InvoiceExportStatus)} helper hides the
 * end-to-end flow described in the official KSeF
 * <a href="https://github.com/CIRFMF/ksef-docs/blob/main/pobieranie-faktur/przyrostowe-pobieranie-faktur.md">incremental retrieval docs</a>.
 *
 * @since 1.0.0
 */
public final class PreparedInvoiceExport implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreparedInvoiceExport.class);

    private static final String SHA_256 = "SHA-256";
    private static final String METADATA_FILE = "_metadata.json";
    private static final String LOG_AWAIT = "[export {}] awaiting ready (terminal status >= 200)";
    private static final String LOG_DOWNLOAD = "[export {}] downloading {} parts";
    private static final String LOG_VERIFY = "[export {}] part {} hash verified";
    private static final String LOG_TEMP_CLEANUP_FAILED = "[export {}] failed to clean up temp file {}: {}";
    private static final String LOG_UNZIP_TMP_CLEANUP_FAILED = "[unzip] failed to clean up tmp file {}: {}";
    private static final int STATUS_POLL_DELAY_MS = 1500;
    private static final int STATUS_POLL_MAX_ATTEMPTS = 100;
    /** Lower bound (inclusive) for terminal export-status codes — anything {@code >= 200} is final. */
    private static final int STATUS_TERMINAL_FLOOR = 200;
    /** Export-status code reported by KSeF for a successful export. Other terminal codes are failures. */
    private static final int STATUS_CODE_OK = 200;
    /** HTTP status code expected on a successful package-part download. */
    private static final int HTTP_OK = 200;
    private static final int ZIP_BUFFER_BYTES = 8 * 1024;

    private static final String ERR_NULL_REFERENCE = "referenceNumber must not be null";
    private static final String ERR_NULL_AES_KEY = "aesKey must not be null";
    private static final String ERR_NULL_INIT_VECTOR = "initVector must not be null";
    private static final String ERR_NULL_INVOICE_EXPORT = "invoiceExport must not be null";
    private static final String ERR_NULL_HTTP_CLIENT = "httpClient must not be null";
    private static final String ERR_NULL_STATUS = "status must not be null";
    private static final String ERR_NULL_PARTS = "status.packageParts must not be null";
    private static final String ERR_HASH_MISMATCH = "Package part SHA-256 mismatch for ordinal %d";
    private static final String ERR_DOWNLOAD_FAILED = "Failed to download package part";
    private static final String ERR_ZIP_FAILED = "Failed to read decrypted ZIP archive";
    private static final String ERR_SHA256_UNAVAILABLE = SHA_256 + " unavailable on this JVM";
    private static final String ERR_INTERRUPTED_POLLING = "Interrupted while polling export status";
    private static final String ERR_INSECURE_PART_URL = "Refusing to download package part over non-HTTPS URL: ";
    private static final String ERR_UNSUPPORTED_METHOD = "Unsupported package-part download method (only GET is supported): ";
    private static final String HTTP_GET = "GET";
    private static final String ERR_DISPOSED = "PreparedInvoiceExport already disposed; AES key/IV have been zeroised";
    private static final String ERR_ENTRY_LIMIT = "ZIP archive exceeds entry-count cap (%d entries, max %d)";
    private static final String ERR_ENTRY_SIZE = "ZIP entry exceeds per-entry size cap (entry %s: %d bytes, max %d)";
    private static final String ERR_TOTAL_SIZE = "ZIP archive exceeds total uncompressed size cap (%d bytes, max %d)";
    private static final String ERR_NO_PART_PARENT_DIR = "Part file has no parent directory: ";
    private static final String SCHEME_HTTPS = "https";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);
    /** Conservative caps to defend against malformed/zip-bomb export packages. */
    private static final int MAX_ZIP_ENTRIES = 100_000;
    private static final long MAX_ZIP_TOTAL_BYTES = 4L * 1024 * 1024 * 1024; // 4 GiB
    private static final long MAX_ZIP_ENTRY_BYTES = 256L * 1024 * 1024;      // 256 MiB
    /** Subdirectory name (under outputDirectory) for staging decrypted parts during streaming unzip. */
    private static final String PARTS_TEMP_SUBDIR = ".parts";
    /** Filename prefix for plaintext part temp files written under {@link #PARTS_TEMP_SUBDIR}. */
    private static final String PART_FILE_PREFIX = "part-";
    /** Filename prefix for encrypted-download temp files (consumed during streaming decrypt). */
    private static final String ENCRYPTED_TEMP_PREFIX = "enc-";
    /** Extension for binary part temp files (both encrypted-download and plaintext). */
    private static final String PART_FILE_SUFFIX = ".bin";
    /** Suffix appended to in-flight target files; renamed to the final name only after verification. */
    private static final String IN_FLIGHT_SUFFIX = ".tmp";
    /** Bounded-copy buffer size for ZIP entry extraction (matches CipherInputStream default block buffer). */
    private static final int COPY_BUFFER_BYTES = 8 * 1024;

    private final InvoiceExport invoiceExport;
    private final HttpClient httpClient;
    private final String referenceNumber;
    private final byte[] aesKey;
    private final byte[] initVector;
    private volatile boolean disposed;

    /**
     * Package-private — Codex round-9 fresh review H3: prevents the
     * @{@code @apiNote Internal} comment from drifting from reality. The
     * AES key and IV must come from the matching server-issued export
     * prepare response — passing arbitrary bytes here is undefined
     * behaviour. Constructed via the same-package {@code SessionHandleConstructor} (internal bridge).
     */
    PreparedInvoiceExport(InvoiceExport invoiceExport,
                          HttpClient httpClient,
                          String referenceNumber,
                          byte[] aesKey,
                          byte[] initVector) {
        this.invoiceExport = Objects.requireNonNull(invoiceExport, ERR_NULL_INVOICE_EXPORT);
        this.httpClient = Objects.requireNonNull(httpClient, ERR_NULL_HTTP_CLIENT);
        this.referenceNumber = Objects.requireNonNull(referenceNumber, ERR_NULL_REFERENCE);
        Objects.requireNonNull(aesKey, ERR_NULL_AES_KEY);
        Objects.requireNonNull(initVector, ERR_NULL_INIT_VECTOR);
        this.aesKey = aesKey.clone();
        this.initVector = initVector.clone();
    }

    /** Server-assigned reference number for this export job. */
    public String referenceNumber() {
        return referenceNumber;
    }

    /**
     * Zeroise the retained AES key + IV. Idempotent. After {@code close()} the
     * handle cannot be used to decrypt further package parts. Use this in a
     * try-with-resources or call explicitly once the export package has been
     * consumed.
     */
    @Override
    public void close() {
        if (!disposed) {
            java.util.Arrays.fill(aesKey, (byte) 0);
            java.util.Arrays.fill(initVector, (byte) 0);
            disposed = true;
        }
    }

    private void requireNotDisposed() {
        if (disposed) {
            throw new IllegalStateException(ERR_DISPOSED);
        }
    }

    /**
     * Poll the export status until it reaches a terminal code (server status
     * code &ge; 200; KSeF reports {@code 200} for completion). Throws
     * {@link KsefSessionPollingTimeoutException} when no terminal status is
     * observed within {@link #STATUS_POLL_MAX_ATTEMPTS} attempts.
     *
     * @return the terminal {@link InvoiceExportStatus}
     */
    public InvoiceExportStatus awaitReady() {
        requireNotDisposed();
        LOGGER.debug(LOG_AWAIT, referenceNumber);
        Integer lastCode = null;
        for (int attempt = 0; attempt < STATUS_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(STATUS_POLL_DELAY_MS);
            InvoiceExportStatus status = invoiceExport.getStatus(referenceNumber);
            Integer code = status.status() != null ? status.status().code() : null;
            if (code != null && code >= STATUS_TERMINAL_FLOOR) {
                if (code == STATUS_CODE_OK) {
                    return status;
                }
                String description = status.status() != null ? status.status().description() : null;
                throw new KsefSessionTerminalFailureException(referenceNumber, code, description, null);
            }
            lastCode = code;
        }
        throw new KsefSessionPollingTimeoutException(referenceNumber, STATUS_POLL_MAX_ATTEMPTS, lastCode);
    }

    /**
     * Heap-buffered variant — convenience for small exports.
     *
     * <p>Downloads every package part referenced by {@code status}, verifies
     * each part's SHA-256 against {@link InvoicePackagePart#partHash()},
     * decrypts with the retained AES key + IV, concatenates the parts back
     * into a ZIP archive in heap, and unzips into an
     * {@link ExportedInvoicePackage}.
     *
     * <p><b>Memory characteristics:</b> the entire decrypted archive is
     * materialised in a {@link ByteArrayOutputStream}; total heap usage
     * scales linearly with package size. Use {@link #downloadAndDecryptTo}
     * instead for production-sized exports — the file-backed variant
     * holds at most one 8 KiB transfer buffer per part regardless of
     * archive size.
     *
     * @param status the terminal status returned by {@link #awaitReady()}
     * @return decrypted, unzipped invoice package
     */
    public ExportedInvoicePackage downloadAndDecrypt(InvoiceExportStatus status) {
        requireNotDisposed();
        Objects.requireNonNull(status, ERR_NULL_STATUS);
        Objects.requireNonNull(status.invoicePackage(), ERR_NULL_PARTS);
        Objects.requireNonNull(status.invoicePackage().parts(), ERR_NULL_PARTS);
        LOGGER.debug(LOG_DOWNLOAD, referenceNumber, status.invoicePackage().parts().size());

        ByteArrayOutputStream archiveBuffer = new ByteArrayOutputStream();
        for (InvoicePackagePart part : status.invoicePackage().parts()) {
            byte[] encryptedBytes = downloadPart(part);
            verifyEncryptedPartHash(part, encryptedBytes);
            byte[] decryptedBytes = CryptoService.decryptAes(encryptedBytes, aesKey, initVector);
            verifyPlaintextPartHash(part, decryptedBytes);
            try {
                archiveBuffer.write(decryptedBytes);
            } catch (IOException unreachable) {
                // ByteArrayOutputStream.write does not perform I/O.
                throw new KsefException(ERR_ZIP_FAILED, unreachable);
            }
        }
        return unzipPackage(archiveBuffer.toByteArray());
    }

    /**
     * File-streaming variant — required for production-sized exports.
     *
     * <p>Downloads each part, verifies SHA-256 (encrypted + plaintext),
     * decrypts via a streaming {@link java.security.DigestInputStream} →
     * {@link javax.crypto.CipherInputStream} → {@link java.security.DigestInputStream}
     * pipeline, then unzips entries with a bounded copy directly to
     * {@code outputDirectory}. The metadata file (if present) is written to
     * {@code outputDirectory/_metadata.json}; each invoice XML to
     * {@code outputDirectory/<entryName>}.
     *
     * <p><b>Memory characteristics:</b> heap usage is bounded by a single
     * 8 KiB transfer buffer per part, independent of part size or archive
     * size. Encrypted parts are streamed to a temp file then consumed via
     * {@link java.io.SequenceInputStream} so the full archive is never
     * concatenated in heap.
     *
     * <p><b>Hardening:</b> per-entry and total ZIP size caps
     * ({@value #MAX_ZIP_ENTRY_BYTES} bytes / {@value #MAX_ZIP_TOTAL_BYTES} bytes
     * respectively) are checked inline during the bounded copy — an oversize
     * entry is rejected before its bytes are written to disk. Plaintext part
     * files and ZIP entries are written to {@code *.tmp} companions and
     * atomic-moved into place only after hash verification (parts) /
     * size-cap checks (zip) succeed; on any failure the tmp is deleted in
     * a {@code finally} block so no half-written, unverified file persists.
     *
     * <p>The output directory is created if it does not exist.
     *
     * @param status the terminal status returned by {@link #awaitReady()}
     * @param outputDirectory directory where the package contents are written
     * @return file-backed handle with on-disk paths
     */
    public ExportedInvoiceDirectory downloadAndDecryptTo(InvoiceExportStatus status, Path outputDirectory) {
        requireNotDisposed();
        Objects.requireNonNull(status, ERR_NULL_STATUS);
        Objects.requireNonNull(status.invoicePackage(), ERR_NULL_PARTS);
        Objects.requireNonNull(status.invoicePackage().parts(), ERR_NULL_PARTS);
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        LOGGER.debug(LOG_DOWNLOAD, referenceNumber, status.invoicePackage().parts().size());

        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException ex) {
            throw new KsefException(ERR_ZIP_FAILED, ex);
        }

        // True streaming pipeline (Codex round-9 F5):
        //   1. per-part: download to a temporary encrypted file using
        //      BodyHandlers.ofFile (no byte[] in heap),
        //   2. stream the encrypted file through DigestInputStream(SHA-256)
        //      → CipherInputStream(AES-CBC decrypt) → DigestInputStream(SHA-256)
        //      → output plaintext file. Hashes are computed during the same
        //      single pass; no buffer holds the full part.
        //   3. delete encrypted file as soon as plaintext file is written.
        //   4. concatenate the plaintext files via SequenceInputStream and
        //      feed to ZipInputStream which writes each entry to outputDir.
        //   5. delete plaintext files (best-effort cleanup).
        //
        // Memory bound: a fixed 8 KiB transfer buffer per part, regardless of
        // part size (256 MiB cap or otherwise). The earlier implementation
        // held two full-part byte[] arrays simultaneously.
        Path partsDir;
        try {
            partsDir = Files.createDirectories(outputDirectory.resolve(PARTS_TEMP_SUBDIR));
        } catch (IOException ex) {
            throw new KsefException(ERR_ZIP_FAILED, ex);
        }
        java.util.List<Path> partFiles = new java.util.ArrayList<>();
        try {
            for (InvoicePackagePart part : status.invoicePackage().parts()) {
                Path partFile = partsDir.resolve(PART_FILE_PREFIX + part.ordinalNumber() + PART_FILE_SUFFIX);
                streamDownloadDecryptVerify(part, partFile);
                partFiles.add(partFile);
            }
            return unzipPackageStreamToDirectory(partFiles, outputDirectory);
        } finally {
            for (Path p : partFiles) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException cleanupFailure) {
                    LOGGER.debug(LOG_TEMP_CLEANUP_FAILED, referenceNumber, p, cleanupFailure.getMessage());
                }
            }
            try {
                Files.deleteIfExists(partsDir);
            } catch (IOException cleanupFailure) {
                LOGGER.debug(LOG_TEMP_CLEANUP_FAILED, referenceNumber, partsDir, cleanupFailure.getMessage());
            }
        }
    }

    /**
     * Stream-download {@code part} to a temporary encrypted file, then stream
     * that file through SHA-256 (encrypted hash) → AES-CBC decrypt → SHA-256
     * (plaintext hash) → {@code partFile}. Verifies both hashes once the
     * stream is exhausted, then deletes the encrypted temp file.
     *
     * <p>This is the F5 fix: prior implementation called
     * {@code BodyHandlers.ofByteArray()} which held the full encrypted part
     * (up to 256 MiB) in heap, and {@code decryptAes} doubled the heap
     * footprint. The streaming variant uses a single 8 KiB buffer per part.
     */
    private void streamDownloadDecryptVerify(InvoicePackagePart part, Path partFile) {
        URI url = part.url();
        if (url.getScheme() == null || !SCHEME_HTTPS.equalsIgnoreCase(url.getScheme())) {
            throw new KsefException(ERR_INSECURE_PART_URL + redactQuery(url), null);
        }
        if (part.method() != null && !HTTP_GET.equalsIgnoreCase(part.method())) {
            throw new KsefException(ERR_UNSUPPORTED_METHOD + part.method(), null);
        }
        Path tempDir = partFile.getParent();
        if (tempDir == null) {
            throw new KsefException(ERR_NO_PART_PARENT_DIR + partFile, null);
        }
        Path encryptedTemp;
        try {
            encryptedTemp = Files.createTempFile(tempDir, ENCRYPTED_TEMP_PREFIX, PART_FILE_SUFFIX);
        } catch (IOException ex) {
            throw new KsefException(ERR_ZIP_FAILED, ex);
        }
        // Decrypted plaintext is written to a *.tmp companion next to partFile,
        // then atomic-moved into place only after BOTH hashes verify (Codex
        // round-9 review: prevent leaving an unverified plaintext on disk if
        // the hash check throws).
        Path plaintextTemp = partFile.resolveSibling(partFile.getFileName() + IN_FLIGHT_SUFFIX);
        try {
            HttpRequest request = HttpRequest.newBuilder(url).GET().timeout(DOWNLOAD_TIMEOUT).build();
            HttpResponse<Path> response;
            try {
                response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofFile(encryptedTemp));
            } catch (IOException ioFailure) {
                throw new KsefNetworkException(ERR_DOWNLOAD_FAILED + " " + redactQuery(url), ioFailure);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new KsefNetworkException(ERR_DOWNLOAD_FAILED + " " + redactQuery(url), interrupted);
            }
            if (response.statusCode() != HTTP_OK) {
                throw new KsefException(ERR_DOWNLOAD_FAILED + " " + redactQuery(url)
                        + " status=" + response.statusCode(), null);
            }
            decryptAndVerifyToFile(part, encryptedTemp, plaintextTemp);
            atomicMove(plaintextTemp, partFile);
        } finally {
            deleteQuietly(encryptedTemp);
            deleteQuietly(plaintextTemp);
        }
    }

    private void decryptAndVerifyToFile(InvoicePackagePart part, Path encryptedTemp, Path plaintextTemp) {
        try {
            MessageDigest encryptedDigest = MessageDigest.getInstance(SHA_256);
            MessageDigest plaintextDigest = MessageDigest.getInstance(SHA_256);
            javax.crypto.Cipher cipher = CryptoService.newAesDecryptCipher(aesKey, initVector);
            try (java.io.InputStream encrypted = Files.newInputStream(encryptedTemp);
                 java.security.DigestInputStream digestEncrypted =
                         new java.security.DigestInputStream(encrypted, encryptedDigest);
                 javax.crypto.CipherInputStream cipherIn =
                         new javax.crypto.CipherInputStream(digestEncrypted, cipher);
                 java.security.DigestInputStream digestPlain =
                         new java.security.DigestInputStream(cipherIn, plaintextDigest);
                 java.io.OutputStream out = Files.newOutputStream(plaintextTemp)) {
                digestPlain.transferTo(out);
            }
            verifyHashOrThrow(part.encryptedPartHash(), encryptedDigest, part.ordinalNumber());
            if (part.encryptedPartHash() != null) {
                LOGGER.debug(LOG_VERIFY, referenceNumber, part.ordinalNumber());
            }
            verifyHashOrThrow(part.partHash(), plaintextDigest, part.ordinalNumber());
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new KsefException(ERR_SHA256_UNAVAILABLE, missingAlgorithm);
        } catch (IOException ioFailure) {
            throw new KsefException(ERR_ZIP_FAILED, ioFailure);
        }
    }

    /**
     * Throw {@link KsefException} if {@code expected} is non-null and does not
     * equal the freshly-{@code .digest()}-ed accumulator.
     */
    private static void verifyHashOrThrow(byte @Nullable [] expected, MessageDigest actualDigest, int ordinal) {
        if (expected == null) {
            return;
        }
        byte[] actual = actualDigest.digest();
        if (!java.util.Arrays.equals(expected, actual)) {
            throw new KsefException(
                    String.format(Locale.ROOT, ERR_HASH_MISMATCH, ordinal),
                    null);
        }
    }

    /**
     * Atomic-move {@code source} to {@code target}; falls back to a non-atomic
     * replace if the underlying filesystem does not support
     * {@link java.nio.file.StandardCopyOption#ATOMIC_MOVE} (rare on a single
     * directory in normal usage).
     */
    private static void atomicMove(Path source, Path target) {
        try {
            Files.move(source, target,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException atomicNotSupported) {
            try {
                Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackFailure) {
                throw new KsefException(ERR_ZIP_FAILED, fallbackFailure);
            }
        } catch (IOException moveFailure) {
            throw new KsefException(ERR_ZIP_FAILED, moveFailure);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupFailure) {
            LOGGER.debug(LOG_TEMP_CLEANUP_FAILED, referenceNumber, path, cleanupFailure.getMessage());
        }
    }

    private static ExportedInvoiceDirectory unzipPackageStreamToDirectory(
            java.util.List<Path> partFiles, Path outputDir) {
        // Build SequenceInputStream over per-part FileInputStream lazily —
        // feeds the ZipInputStream a continuous stream without ever loading
        // the full archive into heap. Each part stream is opened on demand
        // and closed by the SequenceInputStream as it advances, so we don't
        // need a separate cleanup loop.
        java.util.Iterator<Path> pathIterator = partFiles.iterator();
        java.util.Enumeration<java.io.InputStream> streams = new java.util.Enumeration<>() {
            @Override public boolean hasMoreElements() { return pathIterator.hasNext(); }
            @Override public java.io.InputStream nextElement() {
                try {
                    return Files.newInputStream(pathIterator.next());
                } catch (IOException openFailure) {
                    throw new KsefException(ERR_ZIP_FAILED, openFailure);
                }
            }
        };
        try (java.io.SequenceInputStream concat = new java.io.SequenceInputStream(streams);
             ZipInputStream zip = new ZipInputStream(concat)) {
            return readEntriesIntoDirectory(zip, outputDir);
        } catch (IOException zipFailure) {
            throw new KsefException(ERR_ZIP_FAILED, zipFailure);
        }
    }

    private static ExportedInvoiceDirectory readEntriesIntoDirectory(ZipInputStream zip, Path outputDir)
            throws IOException {
        Map<String, Path> invoiceXmls = new HashMap<>();
        Path metadataPath = null;
        long totalBytes = 0L;
        int entryCount = 0;
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {
            if (!entry.isDirectory()) {
                entryCount++;
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw new KsefException(
                            String.format(Locale.ROOT, ERR_ENTRY_LIMIT, entryCount, MAX_ZIP_ENTRIES), null);
                }
                Path target = resolveEntryTarget(outputDir, entry);
                long entrySize = writeEntryToTarget(zip, target, entry.getName(), totalBytes);
                totalBytes += entrySize;
                if (METADATA_FILE.equals(entry.getName())) {
                    metadataPath = target;
                } else {
                    invoiceXmls.put(entry.getName(), target);
                }
            }
            entry = zip.getNextEntry();
        }
        return new ExportedInvoiceDirectory(outputDir, metadataPath, invoiceXmls);
    }

    private static Path resolveEntryTarget(Path outputDir, ZipEntry entry) throws IOException {
        String entryName = entry.getName();
        if (entryName == null) {
            throw new KsefException("ZIP entry has null name", null);
        }
        Path target = outputDir.resolve(entryName).normalize();
        if (!target.startsWith(outputDir.normalize())) {
            throw new KsefException("ZIP entry escapes output directory: " + entryName, null);
        }
        Path parent = target.getParent();
        Files.createDirectories(parent == null ? outputDir : parent);
        return target;
    }

    /**
     * Bounded-copy ZIP entry bytes into a {@code *.tmp} companion of {@code target},
     * checking per-entry and running-total caps inline against {@link #MAX_ZIP_ENTRY_BYTES}
     * and {@link #MAX_ZIP_TOTAL_BYTES} BEFORE writing each chunk. On success,
     * atomic-move the tmp into place. Codex round-9 review (F2): the previous
     * {@code Files.copy(zip, target, REPLACE_EXISTING)} would fully materialise
     * an oversize entry to disk before the cap check, defeating the
     * zip-bomb-hardening claim.
     *
     * @param totalBytesSoFar bytes already written by previous entries in this
     *                         archive — used for inline running-total enforcement
     * @return number of bytes written for this entry (≤ {@link #MAX_ZIP_ENTRY_BYTES})
     */
    private static long writeEntryToTarget(ZipInputStream zip, Path target, String entryName,
                                            long totalBytesSoFar) throws IOException {
        Path tempTarget = target.resolveSibling(target.getFileName() + IN_FLIGHT_SUFFIX);
        long entrySize = 0L;
        boolean writeSucceeded = false;
        try {
            try (java.io.OutputStream out = Files.newOutputStream(tempTarget)) {
                byte[] buffer = new byte[COPY_BUFFER_BYTES];
                while (true) {
                    int chunkLength = zip.read(buffer);
                    if (chunkLength < 0) {
                        break;
                    }
                    entrySize += chunkLength;
                    if (entrySize > MAX_ZIP_ENTRY_BYTES) {
                        throw new KsefException(
                                String.format(Locale.ROOT, ERR_ENTRY_SIZE, entryName, entrySize, MAX_ZIP_ENTRY_BYTES),
                                null);
                    }
                    long projectedTotal = totalBytesSoFar + entrySize;
                    if (projectedTotal > MAX_ZIP_TOTAL_BYTES) {
                        throw new KsefException(
                                String.format(Locale.ROOT, ERR_TOTAL_SIZE, projectedTotal, MAX_ZIP_TOTAL_BYTES),
                                null);
                    }
                    out.write(buffer, 0, chunkLength);
                }
            }
            atomicMove(tempTarget, target);
            writeSucceeded = true;
            return entrySize;
        } finally {
            if (!writeSucceeded) {
                try {
                    Files.deleteIfExists(tempTarget);
                } catch (IOException cleanupFailure) {
                    LOGGER.debug(LOG_UNZIP_TMP_CLEANUP_FAILED, tempTarget, cleanupFailure.getMessage());
                }
            }
        }
    }

    private byte[] downloadPart(InvoicePackagePart part) {
        URI url = part.url();
        if (url.getScheme() == null || !SCHEME_HTTPS.equalsIgnoreCase(url.getScheme())) {
            throw new KsefException(ERR_INSECURE_PART_URL + redactQuery(url), null);
        }
        if (part.method() != null && !HTTP_GET.equalsIgnoreCase(part.method())) {
            throw new KsefException(ERR_UNSUPPORTED_METHOD + part.method(), null);
        }
        HttpRequest request = HttpRequest.newBuilder(url).GET().timeout(DOWNLOAD_TIMEOUT).build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != HTTP_OK) {
                throw new KsefException(ERR_DOWNLOAD_FAILED + " " + redactQuery(url)
                        + " status=" + response.statusCode(), null);
            }
            return response.body();
        } catch (IOException ioFailure) {
            throw new KsefNetworkException(ERR_DOWNLOAD_FAILED + " " + redactQuery(url), ioFailure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(ERR_DOWNLOAD_FAILED + " " + redactQuery(url), interrupted);
        }
    }

    /**
     * Delegate to shared {@code UriRedaction.redactQuery(URI)} (internal helper)
     * so this class and the batch submission flow share the same redaction policy
     * (Codex round-9 fresh review H1).
     */
    private static String redactQuery(URI url) {
        return io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.UriRedaction.redactQuery(url);
    }

    private void verifyEncryptedPartHash(InvoicePackagePart part, byte[] encryptedBytes) {
        byte[] expectedHash = part.encryptedPartHash();
        if (expectedHash == null) {
            // KSeF guarantees encryptedPartHash on encrypted exports; nothing to verify on the
            // ciphertext when only the plaintext partHash is present (handled in verifyPlaintextHash).
            return;
        }
        try {
            byte[] actualHash = MessageDigest.getInstance(SHA_256).digest(encryptedBytes);
            if (!java.util.Arrays.equals(expectedHash, actualHash)) {
                throw new KsefException(String.format(Locale.ROOT, ERR_HASH_MISMATCH, part.ordinalNumber()), null);
            }
            LOGGER.debug(LOG_VERIFY, referenceNumber, part.ordinalNumber());
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new KsefException(ERR_SHA256_UNAVAILABLE, missingAlgorithm);
        }
    }

    private void verifyPlaintextPartHash(InvoicePackagePart part, byte[] decryptedBytes) {
        byte[] expectedHash = part.partHash();
        if (expectedHash == null) {
            return;
        }
        try {
            byte[] actualHash = MessageDigest.getInstance(SHA_256).digest(decryptedBytes);
            if (!java.util.Arrays.equals(expectedHash, actualHash)) {
                throw new KsefException(String.format(Locale.ROOT, ERR_HASH_MISMATCH, part.ordinalNumber()), null);
            }
            LOGGER.debug(LOG_VERIFY, referenceNumber, part.ordinalNumber());
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new KsefException(ERR_SHA256_UNAVAILABLE, missingAlgorithm);
        }
    }

    private static ExportedInvoicePackage unzipPackage(byte[] zipBytes) {
        Map<String, byte[]> invoiceXmls = new HashMap<>();
        byte @Nullable [] metadataJson = null;
        long totalBytes = 0L;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    entryCount++;
                    if (entryCount > MAX_ZIP_ENTRIES) {
                        throw new KsefException(
                                String.format(Locale.ROOT, ERR_ENTRY_LIMIT, entryCount, MAX_ZIP_ENTRIES), null);
                    }
                    byte[] entryBytes = readEntryBytes(zip, entry.getName());
                    totalBytes += entryBytes.length;
                    if (totalBytes > MAX_ZIP_TOTAL_BYTES) {
                        throw new KsefException(
                                String.format(Locale.ROOT, ERR_TOTAL_SIZE, totalBytes, MAX_ZIP_TOTAL_BYTES), null);
                    }
                    if (METADATA_FILE.equals(entry.getName())) {
                        metadataJson = entryBytes;
                    } else {
                        invoiceXmls.put(entry.getName(), entryBytes);
                    }
                }
                entry = zip.getNextEntry();
            }
        } catch (IOException zipFailure) {
            throw new KsefException(ERR_ZIP_FAILED, zipFailure);
        }
        return new ExportedInvoicePackage(metadataJson, invoiceXmls);
    }

    private static byte[] readEntryBytes(ZipInputStream zip, String entryName) throws IOException {
        ByteArrayOutputStream entryBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[ZIP_BUFFER_BYTES];
        long entrySize = 0L;
        int read = zip.read(buffer);
        while (read > 0) {
            entrySize += read;
            if (entrySize > MAX_ZIP_ENTRY_BYTES) {
                throw new KsefException(
                        String.format(Locale.ROOT, ERR_ENTRY_SIZE, entryName, entrySize, MAX_ZIP_ENTRY_BYTES),
                        null);
            }
            entryBuffer.write(buffer, 0, read);
            read = zip.read(buffer);
        }
        return entryBuffer.toByteArray();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(ERR_INTERRUPTED_POLLING, interrupted);
        }
    }
}
