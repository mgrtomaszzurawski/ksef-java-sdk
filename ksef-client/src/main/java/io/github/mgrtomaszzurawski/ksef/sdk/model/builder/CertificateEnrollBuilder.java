/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.KsefCertificateTypeRaw;

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
 *     .create("My Auth Cert", KsefCertificateTypeRaw.AUTHENTICATION, csrBytes)
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
    private final KsefCertificateTypeRaw certificateType;
    private final byte[] csr;
    private OffsetDateTime validFrom;

    private CertificateEnrollBuilder(String certificateName, KsefCertificateTypeRaw certificateType, byte[] csr) {
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
                                                   KsefCertificateTypeRaw certificateType,
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
     * Build the enrollment request.
     *
     * @return the request ready to pass to {@code CertificateClient.enroll()}
     */
    public EnrollCertificateRequestRaw build() {
        EnrollCertificateRequestRaw request = new EnrollCertificateRequestRaw();
        request.setCertificateName(certificateName);
        request.setCertificateType(certificateType);
        request.setCsr(csr.clone());
        if (validFrom != null) {
            request.setValidFrom(validFrom);
        }
        return request;
    }
}
