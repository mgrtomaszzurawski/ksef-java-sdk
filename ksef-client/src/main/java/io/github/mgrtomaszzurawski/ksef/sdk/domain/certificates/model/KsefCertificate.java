/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Holder for a KSeF Offline certificate (X.509 + private key) used to
 * sign KOD II QR payloads on offline-mode invoices.
 *
 * <p>Distinct from
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCertificateCredentials}
 * (authentication credentials with extra context like
 * {@code identifier}, {@code subjectIdentifier}, {@code signingOptions})
 * — this type carries only the material needed to compute and embed a
 * KOD II signature on an offline invoice. A consumer normally builds
 * one of these from a PKCS#12 keystore loaded on app start and reuses
 * it across all offline issuances.
 *
 * <p>Spec citation: REQ-OFFLINE-006 (KSeF Offline certificate
 * requirement); ADR-019 (KOD II signing scheme).
 *
 * @since 1.0.0
 */
public final class KsefCertificate {

    private static final String ERR_NULL_CERTIFICATE = "certificate must not be null";
    private static final String ERR_NULL_PRIVATE_KEY = "privateKey must not be null";

    private final X509Certificate certificate;
    private final PrivateKey privateKey;

    /**
     * Construct a certificate holder for KOD II signing.
     *
     * @param certificate the X.509 certificate (carries the serial that
     *     KOD II URLs embed); non-null
     * @param privateKey the private key matching the certificate; used
     *     only to sign the canonical KOD II payload; non-null
     */
    public KsefCertificate(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = Objects.requireNonNull(certificate, ERR_NULL_CERTIFICATE);
        this.privateKey = Objects.requireNonNull(privateKey, ERR_NULL_PRIVATE_KEY);
    }

    /** The wrapped X.509 certificate. */
    public X509Certificate certificate() {
        return certificate;
    }

    /** The wrapped private key — used solely for KOD II signing. */
    public PrivateKey privateKey() {
        return privateKey;
    }

    /**
     * X.509 serial number as the canonical hex-uppercase
     * {@link CertificateSerialNumber} that KSeF embeds into KOD II URLs.
     * Computed from the parsed certificate so it always matches the
     * wrapped X.509 material.
     */
    public CertificateSerialNumber serialNumber() {
        return CertificateSerialNumber.parse(
                certificate.getSerialNumber().toString(16).toUpperCase(java.util.Locale.ROOT));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KsefCertificate that)) {
            return false;
        }
        // Equality on the parsed cert is by encoded form; private key
        // equality is implementation-dependent so we compare references.
        return Objects.equals(certificate, that.certificate)
                && privateKey == that.privateKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificate, System.identityHashCode(privateKey));
    }

    @Override
    public String toString() {
        return "KsefCertificate[serialNumber=" + serialNumber()
                + ", subject=" + certificate.getSubjectX500Principal().getName() + "]";
    }
}
