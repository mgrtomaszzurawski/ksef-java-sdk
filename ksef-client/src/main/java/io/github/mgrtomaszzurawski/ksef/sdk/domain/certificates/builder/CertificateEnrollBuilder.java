/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Builder for KSeF certificate enrollment requests.
 * <p>
 * Required: certificateName, certificateType, csr (PKCS#10 in DER format).
 * Optional: validFrom.
 * <p>
 * Usage:
 * <pre>{@code
 * EnrollCertificateRequestRaw request = CertificateEnrollBuilder
 *     .create("My Auth Cert", KsefCertificateType.AUTHENTICATION, csrBytes)
 *     .validFrom(OffsetDateTime.now().plusDays(1))
 *     .build();
 * }</pre>
 */
public final class CertificateEnrollBuilder {

    private static final String ERR_NULL_CERTIFICATE_NAME = "certificateName is required";
    private static final String ERR_NULL_CERTIFICATE_TYPE = "certificateType is required";
    private static final String ERR_NULL_CSR = "csr is required";
    private static final String ERR_CSR_EMPTY = "csr must not be empty";

    private final String certificateName;
    private final KsefCertificateType certificateType;
    private final byte[] csr;
    private OffsetDateTime validFrom;

    private CertificateEnrollBuilder(String certificateName, KsefCertificateType certificateType, byte[] csr) {
        this.certificateName = Objects.requireNonNull(certificateName, ERR_NULL_CERTIFICATE_NAME);
        this.certificateType = Objects.requireNonNull(certificateType, ERR_NULL_CERTIFICATE_TYPE);
        Objects.requireNonNull(csr, ERR_NULL_CSR);
        if (csr.length == 0) {
            throw new IllegalArgumentException(ERR_CSR_EMPTY);
        }
        this.csr = csr.clone();
    }

    /**
     * Create a builder with required fields.
     *
     * @param certificateName human-readable certificate name
     * @param certificateType Authentication or Offline
     * @param csr PKCS#10 certificate signing request in DER format
     */
    public static CertificateEnrollBuilder create(String certificateName,
                                                   KsefCertificateType certificateType,
                                                   byte[] csr) {
        return new CertificateEnrollBuilder(certificateName, certificateType, csr);
    }

    /**
     * Set the optional validity start date. If not set, the certificate is valid from issuance.
     *
     * @param validFrom validity start date
     */
    public CertificateEnrollBuilder validFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public CertificateEnrollBuilder toBuilder() {
        CertificateEnrollBuilder copy = new CertificateEnrollBuilder(this.certificateName, this.certificateType, this.csr);
        copy.validFrom = this.validFrom;
        return copy;
    }

    /**
     * Build the enrollment request.
     *
     * @return the request ready to pass to {@code CertificateClient.enroll()}
     *
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public EnrollCertificateRequestRaw build() {
        EnrollCertificateRequestRaw request = new EnrollCertificateRequestRaw();
        request.setCertificateName(certificateName);
        request.setCertificateType(certificateType.toRaw());
        request.setCsr(csr.clone());
        if (validFrom != null) {
            request.setValidFrom(validFrom);
        }
        return request;
    }
}
