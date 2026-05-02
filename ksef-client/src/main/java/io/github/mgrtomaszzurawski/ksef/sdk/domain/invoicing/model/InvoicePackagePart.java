/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoicePackagePartRaw;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;

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
                raw.getOrdinalNumber(),
                raw.getPartName(),
                raw.getMethod(),
                raw.getUrl(),
                raw.getPartSize(),
                raw.getPartHash(),
                raw.getEncryptedPartSize(),
                raw.getEncryptedPartHash(),
                raw.getExpirationDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvoicePackagePart other)) {
            return false;
        }
        return ordinalNumber == other.ordinalNumber
                && Objects.equals(partName, other.partName)
                && Objects.equals(method, other.method)
                && Objects.equals(url, other.url)
                && Objects.equals(partSize, other.partSize)
                && Arrays.equals(partHash, other.partHash)
                && Objects.equals(encryptedPartSize, other.encryptedPartSize)
                && Arrays.equals(encryptedPartHash, other.encryptedPartHash)
                && Objects.equals(expirationDate, other.expirationDate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(ordinalNumber, partName, method, url, partSize, encryptedPartSize, expirationDate);
        result = 31 * result + Arrays.hashCode(partHash);
        result = 31 * result + Arrays.hashCode(encryptedPartHash);
        return result;
    }

    @Override
    public String toString() {
        return "InvoicePackagePart[ordinalNumber=" + ordinalNumber
                + ", partName=" + partName
                + ", method=" + method
                + ", url=" + url
                + ", partSize=" + partSize
                + ", partHash=byte[" + (partHash == null ? 0 : partHash.length) + "]"
                + ", encryptedPartSize=" + encryptedPartSize
                + ", encryptedPartHash=byte[" + (encryptedPartHash == null ? 0 : encryptedPartHash.length) + "]"
                + ", expirationDate=" + expirationDate + "]";
    }
}
