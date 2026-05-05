/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * A retrieved certificate with its details.
 *
 * @param certificate raw certificate bytes
 * @param certificateName certificate name
 * @param certificateSerialNumber serial number
 * @param certificateType certificate type (Authentication or Offline)
 *
 * @since 1.0.0
 */
public record RetrievedCertificate(
        byte[] certificate,
        String certificateName,
        String certificateSerialNumber,
        String certificateType) {

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
        return Objects.hash(certificateName, certificateSerialNumber, certificateType, Arrays.hashCode(certificate));
    }

    @Override
    public String toString() {
        return "RetrievedCertificate[certificate=byte[" + (certificate == null ? 0 : certificate.length) + "]"
                + ", certificateName=" + certificateName
                + ", certificateSerialNumber=" + certificateSerialNumber
                + ", certificateType=" + certificateType + "]";
    }
}
