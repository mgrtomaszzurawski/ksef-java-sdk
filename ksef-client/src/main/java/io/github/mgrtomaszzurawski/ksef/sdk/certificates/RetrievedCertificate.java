/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates;

import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesListItemRaw;

/**
 * A retrieved certificate with its details.
 *
 * @param certificate raw certificate bytes
 * @param certificateName certificate name
 * @param certificateSerialNumber serial number
 * @param certificateType certificate type (Authentication or Offline)
 */
public record RetrievedCertificate(
        byte[] certificate,
        String certificateName,
        String certificateSerialNumber,
        String certificateType) {

    public static RetrievedCertificate from(RetrieveCertificatesListItemRaw raw) {
        return new RetrievedCertificate(
                raw.getCertificate(),
                raw.getCertificateName(),
                raw.getCertificateSerialNumber(),
                raw.getCertificateType() != null ? raw.getCertificateType().getValue() : null);
    }
}
