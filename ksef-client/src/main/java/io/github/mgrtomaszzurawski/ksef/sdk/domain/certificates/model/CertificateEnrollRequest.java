/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request payload for {@code CertificateClient.enroll(...)}.
 *
 * @param certificateName human-readable certificate name
 * @param certificateType Authentication or Offline
 * @param csr PKCS#10 certificate signing request in DER format (cloned defensively)
 * @param validFrom optional validity start date (null → valid from issuance)
 */
public record CertificateEnrollRequest(
        String certificateName,
        KsefCertificateType certificateType,
        byte[] csr,
        @Nullable OffsetDateTime validFrom) {

    public CertificateEnrollRequest {
        Objects.requireNonNull(certificateName, "certificateName");
        Objects.requireNonNull(certificateType, "certificateType");
        Objects.requireNonNull(csr, "csr");
        csr = csr.clone();
    }

    @Override
    public byte[] csr() { return csr.clone(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificateEnrollRequest other)) {
            return false;
        }
        return certificateName.equals(other.certificateName)
                && certificateType == other.certificateType
                && Arrays.equals(csr, other.csr)
                && Objects.equals(validFrom, other.validFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificateName, certificateType, validFrom, Arrays.hashCode(csr));
    }
}
