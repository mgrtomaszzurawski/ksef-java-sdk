/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Internal helper that turns a list of raw invoice XML byte arrays into the wire-level
 * batch package — encrypted ZIP, file/part metadata and the per-part byte buffers ready
 * to upload to the URLs returned by the open batch session response.
 *
 * <p>The package is built fully in memory. Splitting is performed after encryption, so
 * each "part" boundary may fall inside an encrypted block — that matches the KSeF
 * requirement that the server reassembles the parts byte-for-byte before decrypting.
 */
final class BatchPackageBuilder {

    private static final String SHA_256 = "SHA-256";
    private static final String INVOICE_ENTRY_SUFFIX = ".xml";
    private static final long DEFAULT_MAX_PART_SIZE = 100L * 1024L * 1024L;
    private static final int MAX_PARTS = 50;
    private static final long MAX_FILE_SIZE = 5_000_000_000L;
    private static final String ERR_NULL_INVOICES = "invoices must not be null";
    private static final String ERR_EMPTY_INVOICES = "invoices must not be empty";
    private static final String ERR_NULL_INVOICE = "invoice bytes must not be null";
    private static final String ERR_EMPTY_INVOICE = "invoice bytes must not be empty";
    private static final String ERR_TOO_MANY_PARTS = "encrypted ZIP exceeds the 50-part limit";
    private static final String ERR_FILE_TOO_LARGE = "encrypted ZIP exceeds the 5GB size limit";
    private static final String ERR_HASH = "SHA-256 hash failed";
    private static final String ERR_ZIP = "Failed to build batch ZIP package";

    private BatchPackageBuilder() {
    }

    /**
     * Build the encrypted batch package.
     *
     * @param invoices invoice XML byte arrays (one per entry in the ZIP)
     * @param aesKey 32-byte AES-256 key (session key)
     * @param initVector 16-byte AES-CBC IV (session IV)
     * @return the package: encrypted ZIP, {@link BatchFileSpec} for the open-session
     *         request, and the actual encrypted byte arrays for each part
     */
    static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector) {
        return build(invoices, aesKey, initVector, DEFAULT_MAX_PART_SIZE);
    }

    /**
     * Build the encrypted batch package with a custom max part size (used by tests).
     *
     * @param invoices invoice XML byte arrays
     * @param aesKey 32-byte AES-256 key
     * @param initVector 16-byte AES-CBC IV
     * @param maxPartSize maximum size per encrypted part in bytes
     * @return the package
     */
    static BatchPackage build(List<byte[]> invoices, byte[] aesKey, byte[] initVector,
                              long maxPartSize) {
        Objects.requireNonNull(invoices, ERR_NULL_INVOICES);
        if (invoices.isEmpty()) {
            throw new IllegalArgumentException(ERR_EMPTY_INVOICES);
        }
        byte[] zipBytes = buildZip(invoices);
        byte[] encrypted = CryptoService.encryptAes(zipBytes, aesKey, initVector);

        if ((long) encrypted.length > MAX_FILE_SIZE) {
            throw new IllegalStateException(ERR_FILE_TOO_LARGE);
        }
        byte[] fullHash = sha256(encrypted);

        List<byte[]> partBytes = splitIntoParts(encrypted, maxPartSize);
        if (partBytes.size() > MAX_PARTS) {
            throw new IllegalStateException(ERR_TOO_MANY_PARTS);
        }
        List<BatchFileSpec.Part> partSpecs = new ArrayList<>(partBytes.size());
        for (int index = 0; index < partBytes.size(); index++) {
            byte[] part = partBytes.get(index);
            partSpecs.add(new BatchFileSpec.Part(index + 1, part.length, sha256(part)));
        }
        BatchFileSpec spec = new BatchFileSpec(encrypted.length, fullHash, partSpecs);
        return new BatchPackage(encrypted, spec, List.copyOf(partBytes));
    }

    private static byte[] buildZip(List<byte[]> invoices) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (byte[] invoice : invoices) {
                Objects.requireNonNull(invoice, ERR_NULL_INVOICE);
                if (invoice.length == 0) {
                    throw new IllegalArgumentException(ERR_EMPTY_INVOICE);
                }
                String entryName = entryNameFor(invoice);
                ZipEntry entry = new ZipEntry(entryName);
                zip.putNextEntry(entry);
                zip.write(invoice);
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(ERR_ZIP, exception);
        }
    }

    private static String entryNameFor(byte[] invoice) {
        byte[] hash = sha256(invoice);
        // URL-safe Base64 (no '/' or '+') so the filename never collides with ZIP path
        // separators.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash) + INVOICE_ENTRY_SUFFIX;
    }

    private static List<byte[]> splitIntoParts(byte[] data, long maxPartSize) {
        List<byte[]> parts = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int end = (int) Math.min(offset + maxPartSize, data.length);
            parts.add(Arrays.copyOfRange(data, offset, end));
            offset = end;
        }
        return parts;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance(SHA_256).digest(data);
        } catch (NoSuchAlgorithmException exception) {
            throw new KsefCryptoException(ERR_HASH, exception);
        }
    }

    /**
     * Result of the package build — the encrypted ZIP, the spec to send in the open
     * batch session request, and the byte buffers to upload to each part URL.
     *
     * @param encryptedZip full encrypted ZIP payload
     * @param spec batch-file spec to attach to the open-session request
     * @param partBytes immutable list of encrypted byte arrays per part (same order as
     *                  {@code spec.parts()})
     */
    record BatchPackage(byte[] encryptedZip, BatchFileSpec spec, List<byte[]> partBytes) {

        /** Defensive-copy compact constructor — protects encryptedZip and partBytes. */
        BatchPackage {
            Objects.requireNonNull(encryptedZip, "encryptedZip must not be null");
            Objects.requireNonNull(spec, "spec must not be null");
            Objects.requireNonNull(partBytes, "partBytes must not be null");
            encryptedZip = encryptedZip.clone();
            List<byte[]> copy = new ArrayList<>(partBytes.size());
            for (byte[] part : partBytes) {
                copy.add(part.clone());
            }
            partBytes = List.copyOf(copy);
        }

        /** Returns a defensive copy of the encrypted ZIP. */
        @Override
        public byte[] encryptedZip() {
            return encryptedZip.clone();
        }

        /** Returns defensive copies of each part's bytes. */
        @Override
        public List<byte[]> partBytes() {
            List<byte[]> copy = new ArrayList<>(partBytes.size());
            for (byte[] part : partBytes) {
                copy.add(part.clone());
            }
            return List.copyOf(copy);
        }
    }
}
