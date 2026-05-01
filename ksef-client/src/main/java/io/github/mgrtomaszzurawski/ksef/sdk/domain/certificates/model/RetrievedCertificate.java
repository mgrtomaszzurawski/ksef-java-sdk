/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesListItemRaw;
import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RetrievedCertificate other)) {
            return false;
        }
        return Arrays.equals(certificate, other.certificate)
                && Objects.equals(certificateName, other.certificateName)
                && Objects.equals(certificateSerialNumber, other.certificateSerialNumber)
                && Objects.equals(certificateType, other.certificateType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(certificateName, certificateSerialNumber, certificateType);
        result = 31 * result + Arrays.hashCode(certificate);
        return result;
    }

    @Override
    public String toString() {
        return "RetrievedCertificate[certificate=byte[" + (certificate == null ? 0 : certificate.length) + "]"
                + ", certificateName=" + certificateName
                + ", certificateSerialNumber=" + certificateSerialNumber
                + ", certificateType=" + certificateType + "]";
    }
}
