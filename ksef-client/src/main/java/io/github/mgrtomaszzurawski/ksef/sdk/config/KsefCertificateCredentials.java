/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Certificate-based authentication credentials for XAdES signature flow.
 *
 * <p>The certificate and private key are used to create an XAdES-BASELINE-B
 * signature of the authentication challenge. This is the qualified
 * electronic signature method.
 *
 * <p>The {@code subjectIdentifier} field controls the
 * {@code SubjectIdentifierType} XML element in the auth request — see
 * {@link CertificateSubjectIdentifier} for the {@code certificateSubject}
 * vs {@code certificateFingerprint} variants. Default is
 * {@link CertificateSubjectIdentifier#subject()} which preserves
 * pre-1.0 behavior.
 *
 * @param certificate X.509 certificate (qualified or test)
 * @param privateKey private key corresponding to the certificate
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 * @param subjectIdentifier strategy for the {@code SubjectIdentifierType}
 *     XML element (REQ-AUTH-033)
 */
public record KsefCertificateCredentials(
        X509Certificate certificate,
        PrivateKey privateKey,
        KsefIdentifier identifier,
        CertificateSubjectIdentifier subjectIdentifier
) implements KsefCredentials {

    private static final String ERR_NULL_CERT = "certificate must not be null";
    private static final String ERR_NULL_KEY = "privateKey must not be null";
    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";
    private static final String ERR_NULL_SUBJECT_ID = "subjectIdentifier must not be null";

    /**
     * Canonical constructor — validates non-null certificate, private key,
     * identifier and subject identifier strategy.
     */
    public KsefCertificateCredentials {
        Objects.requireNonNull(certificate, ERR_NULL_CERT);
        Objects.requireNonNull(privateKey, ERR_NULL_KEY);
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
        Objects.requireNonNull(subjectIdentifier, ERR_NULL_SUBJECT_ID);
    }

    /**
     * Convenience constructor — defaults
     * {@code subjectIdentifier} to {@link CertificateSubjectIdentifier#subject()}.
     */
    public KsefCertificateCredentials(X509Certificate certificate, PrivateKey privateKey, KsefIdentifier identifier) {
        this(certificate, privateKey, identifier, CertificateSubjectIdentifier.subject());
    }

    /**
     * Backwards-compatible constructor — accepts a plain NIP string and
     * defaults to the {@code certificateSubject} identifier strategy.
     *
     * @param certificate X.509 certificate
     * @param privateKey private key matching the certificate
     * @param nip 10-digit Polish tax identification number
     */
    public KsefCertificateCredentials(X509Certificate certificate, PrivateKey privateKey, String nip) {
        this(certificate, privateKey, KsefIdentifier.nip(nip), CertificateSubjectIdentifier.subject());
    }

    /**
     * Returns a copy of these credentials with a different
     * {@link CertificateSubjectIdentifier} strategy. Useful for callers
     * that need {@code certificateFingerprint} authentication.
     */
    public KsefCertificateCredentials withSubjectIdentifier(CertificateSubjectIdentifier newStrategy) {
        return new KsefCertificateCredentials(certificate, privateKey, identifier, newStrategy);
    }

    @Override
    public String toString() {
        return "KsefCertificateCredentials[identifier=" + identifier
                + ", subjectIdentifier=" + subjectIdentifier.wireType() + "]";
    }
}
