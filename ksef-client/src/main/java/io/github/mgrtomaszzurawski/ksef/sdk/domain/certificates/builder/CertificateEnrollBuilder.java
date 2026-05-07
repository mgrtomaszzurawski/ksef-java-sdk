/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for KSeF certificate enrollment requests.
 *
 * <p>Required: certificateName, certificateType, csr. Optional: validFrom.
 *
 * <p><strong>CSR encoding:</strong> {@code csr} must be raw PKCS#10
 * bytes in <strong>DER</strong> form. PEM-encoded input (with
 * {@code -----BEGIN CERTIFICATE REQUEST-----} headers and base64
 * payload) is rejected by the server with a parse error. To convert
 * a PEM CSR to DER: {@code openssl req -outform DER -in csr.pem -out csr.der}.
 *
 * @since 1.0.0
 */
public final class CertificateEnrollBuilder {

    private static final String ERR_NULL_CERTIFICATE_NAME = "certificateName is required";
    private static final String ERR_NULL_CERTIFICATE_TYPE = "certificateType is required";
    private static final String ERR_NULL_CSR = "csr is required";
    private static final String ERR_CSR_EMPTY = "csr must not be empty";

    private final String certificateName;
    private final KsefCertificateType certificateType;
    private final byte[] csr;
    private @Nullable OffsetDateTime validFrom;

    private CertificateEnrollBuilder(String certificateName, KsefCertificateType certificateType, byte[] csr) {
        this.certificateName = Objects.requireNonNull(certificateName, ERR_NULL_CERTIFICATE_NAME);
        this.certificateType = Objects.requireNonNull(certificateType, ERR_NULL_CERTIFICATE_TYPE);
        Objects.requireNonNull(csr, ERR_NULL_CSR);
        if (csr.length == 0) {
            throw new IllegalArgumentException(ERR_CSR_EMPTY);
        }
        this.csr = csr.clone();
    }

    public static CertificateEnrollBuilder create(String certificateName,
                                                  KsefCertificateType certificateType,
                                                  byte[] csr) {
        return new CertificateEnrollBuilder(certificateName, certificateType, csr);
    }

    public CertificateEnrollBuilder validFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    public CertificateEnrollBuilder toBuilder() {
        CertificateEnrollBuilder copy = new CertificateEnrollBuilder(this.certificateName, this.certificateType, this.csr);
        copy.validFrom = this.validFrom;
        return copy;
    }

    public CertificateEnrollRequest build() {
        return new CertificateEnrollRequest(certificateName, certificateType, csr, validFrom);
    }
}
