/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ExportedInvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceExportStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackagePart;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNetworkException;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionPollingTimeoutException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle for an in-flight invoice export. Returned by
 * {@link InvoiceClient#prepareExport(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder, boolean)
 * client.invoices().prepareExport(...)}, this class:
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
 */
public final class PreparedInvoiceExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreparedInvoiceExport.class);

    private static final String SHA_256 = "SHA-256";
    private static final String METADATA_FILE = "_metadata.json";
    private static final String LOG_AWAIT = "[export {}] awaiting ready (terminal status >= 200)";
    private static final String LOG_DOWNLOAD = "[export {}] downloading {} parts";
    private static final String LOG_VERIFY = "[export {}] part {} hash verified";
    private static final int STATUS_POLL_DELAY_MS = 1500;
    private static final int STATUS_POLL_MAX_ATTEMPTS = 100;
    private static final int STATUS_TERMINAL_FLOOR = 200;
    private static final int HTTP_OK = 200;
    private static final int ZIP_BUFFER_BYTES = 8 * 1024;

    private static final String ERR_NULL_REFERENCE = "referenceNumber must not be null";
    private static final String ERR_NULL_AES_KEY = "aesKey must not be null";
    private static final String ERR_NULL_INIT_VECTOR = "initVector must not be null";
    private static final String ERR_NULL_INVOICES = "invoices must not be null";
    private static final String ERR_NULL_HTTP_CLIENT = "httpClient must not be null";
    private static final String ERR_NULL_STATUS = "status must not be null";
    private static final String ERR_NULL_PARTS = "status.packageParts must not be null";
    private static final String ERR_HASH_MISMATCH = "Package part SHA-256 mismatch for ordinal %d";
    private static final String ERR_DOWNLOAD_FAILED = "Failed to download package part";
    private static final String ERR_ZIP_FAILED = "Failed to read decrypted ZIP archive";
    private static final String ERR_SHA256_UNAVAILABLE = SHA_256 + " unavailable on this JVM";
    private static final String ERR_INTERRUPTED_POLLING = "Interrupted while polling export status";
    private static final String ERR_INSECURE_PART_URL = "Refusing to download package part over non-HTTPS URL: ";
    private static final String SCHEME_HTTPS = "https";
    private static final java.time.Duration DOWNLOAD_TIMEOUT = java.time.Duration.ofMinutes(5);

    private final InvoiceClient invoices;
    private final HttpClient httpClient;
    private final String referenceNumber;
    private final byte[] aesKey;
    private final byte[] initVector;

    /**
     * @apiNote Internal — constructed by {@code InvoiceClientImpl.prepareExport(...)}.
     * @deprecated For SDK-internal construction only.
     */
    @Deprecated(since = "0.1.0")
    public PreparedInvoiceExport(InvoiceClient invoices,
                                 HttpClient httpClient,
                                 String referenceNumber,
                                 byte[] aesKey,
                                 byte[] initVector) {
        this.invoices = Objects.requireNonNull(invoices, ERR_NULL_INVOICES);
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
     * Poll the export status until it reaches a terminal code (server status
     * code &ge; 200; KSeF reports {@code 200} for completion). Throws
     * {@link KsefSessionPollingTimeoutException} when no terminal status is
     * observed within {@link #STATUS_POLL_MAX_ATTEMPTS} attempts.
     *
     * @return the terminal {@link InvoiceExportStatus}
     */
    public InvoiceExportStatus awaitReady() {
        LOGGER.debug(LOG_AWAIT, referenceNumber);
        Integer lastCode = null;
        for (int attempt = 0; attempt < STATUS_POLL_MAX_ATTEMPTS; attempt++) {
            sleep(STATUS_POLL_DELAY_MS);
            InvoiceExportStatus status = invoices.getExportStatus(referenceNumber);
            Integer code = status.status() != null ? status.status().code() : null;
            if (code != null && code >= STATUS_TERMINAL_FLOOR) {
                return status;
            }
            lastCode = code;
        }
        throw new KsefSessionPollingTimeoutException(referenceNumber, STATUS_POLL_MAX_ATTEMPTS, lastCode);
    }

    /**
     * Download every package part referenced by {@code status}, verify each
     * part's SHA-256 against {@link InvoicePackagePart#partHash()}, decrypt
     * with the retained AES key + IV, concatenate the parts back into a ZIP
     * archive, and unzip into an {@link ExportedInvoicePackage}.
     *
     * @param status the terminal status returned by {@link #awaitReady()}
     * @return decrypted, unzipped invoice package
     */
    public ExportedInvoicePackage downloadAndDecrypt(InvoiceExportStatus status) {
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

    private byte[] downloadPart(InvoicePackagePart part) {
        URI url = part.url();
        if (url.getScheme() == null || !SCHEME_HTTPS.equalsIgnoreCase(url.getScheme())) {
            throw new KsefException(ERR_INSECURE_PART_URL + url, null);
        }
        HttpRequest request = HttpRequest.newBuilder(url).GET().timeout(DOWNLOAD_TIMEOUT).build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != HTTP_OK) {
                throw new KsefException(ERR_DOWNLOAD_FAILED + " " + url + " status=" + response.statusCode(), null);
            }
            return response.body();
        } catch (IOException ioFailure) {
            throw new KsefNetworkException(ERR_DOWNLOAD_FAILED + " " + url, ioFailure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new KsefNetworkException(ERR_DOWNLOAD_FAILED + " " + url, interrupted);
        }
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
        byte[] metadataJson = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    byte[] entryBytes = readEntryBytes(zip);
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

    private static byte[] readEntryBytes(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream entryBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[ZIP_BUFFER_BYTES];
        int read = zip.read(buffer);
        while (read > 0) {
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
