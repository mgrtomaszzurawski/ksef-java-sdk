/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoicePackagePartRaw;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * A downloadable part of an invoice export package.
 *
 * @param ordinalNumber part sequence number
 * @param partName part file name
 * @param method HTTP method for download
 * @param url download URL
 * @param partSize part size in bytes
 * @param partHash part SHA-256 hash
 * @param encryptedPartSize encrypted part size in bytes
 * @param encryptedPartHash encrypted part SHA-256 hash
 * @param expirationDate when the download URL expires
 */
public record InvoicePackagePart(
        int ordinalNumber,
        String partName,
        String method,
        URI url,
        Long partSize,
        byte[] partHash,
        Long encryptedPartSize,
        byte[] encryptedPartHash,
        OffsetDateTime expirationDate) {

    public static InvoicePackagePart from(InvoicePackagePartRaw raw) {
        return new InvoicePackagePart(
                raw.getOrdinalNumber() != null ? raw.getOrdinalNumber() : 0,
                raw.getPartName(),
                raw.getMethod(),
                raw.getUrl(),
                raw.getPartSize(),
                raw.getPartHash(),
                raw.getEncryptedPartSize(),
                raw.getEncryptedPartHash(),
                raw.getExpirationDate());
    }
}
