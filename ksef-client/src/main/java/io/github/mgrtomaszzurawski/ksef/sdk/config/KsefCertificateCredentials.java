/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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
 * {@code SubjectIdentifierType} XML element (REQ-AUTH-033) — see
 * {@link CertificateSubjectIdentifier}. Default is
 * {@link CertificateSubjectIdentifier#subject()}.
 *
 * <p>The {@code signingOptions} field controls XAdES profile and digest
 * algorithm choice. 1.0.0 only validates the
 * {@code BASELINE_B} + {@code SHA256} combination at signing time per
 * ADR-021 (public knobs must mean working support, not aspirational
 * support). Default is {@link SigningOptions#defaults()}.
 *
 * @param certificate X.509 certificate (qualified or test)
 * @param privateKey private key corresponding to the certificate
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 * @param subjectIdentifier strategy for the {@code SubjectIdentifierType} XML element
 * @param signingOptions XAdES profile + digest options
 */
public record KsefCertificateCredentials(
        X509Certificate certificate,
        PrivateKey privateKey,
        KsefIdentifier identifier,
        CertificateSubjectIdentifier subjectIdentifier,
        SigningOptions signingOptions
) implements KsefCredentials {

    private static final String ERR_NULL_CERT = "certificate must not be null";
    private static final String ERR_NULL_KEY = "privateKey must not be null";
    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";
    private static final String ERR_NULL_SUBJECT_ID = "subjectIdentifier must not be null";
    private static final String ERR_NULL_SIGNING_OPTIONS = "signingOptions must not be null";

    /**
     * Canonical constructor — validates non-null fields.
     */
    public KsefCertificateCredentials {
        Objects.requireNonNull(certificate, ERR_NULL_CERT);
        Objects.requireNonNull(privateKey, ERR_NULL_KEY);
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
        Objects.requireNonNull(subjectIdentifier, ERR_NULL_SUBJECT_ID);
        Objects.requireNonNull(signingOptions, ERR_NULL_SIGNING_OPTIONS);
    }

    /**
     * Convenience constructor — defaults {@code signingOptions} to
     * {@link SigningOptions#defaults()}.
     */
    public KsefCertificateCredentials(X509Certificate certificate, PrivateKey privateKey,
                                      KsefIdentifier identifier,
                                      CertificateSubjectIdentifier subjectIdentifier) {
        this(certificate, privateKey, identifier, subjectIdentifier, SigningOptions.defaults());
    }

    /**
     * Convenience constructor — defaults {@code subjectIdentifier} +
     * {@code signingOptions} to defaults.
     */
    public KsefCertificateCredentials(X509Certificate certificate, PrivateKey privateKey, KsefIdentifier identifier) {
        this(certificate, privateKey, identifier,
                CertificateSubjectIdentifier.subject(), SigningOptions.defaults());
    }

    /**
     * Backwards-compatible constructor — accepts a plain NIP string with
     * default subject identifier strategy and default signing options.
     */
    public KsefCertificateCredentials(X509Certificate certificate, PrivateKey privateKey, String nip) {
        this(certificate, privateKey, KsefIdentifier.nip(nip),
                CertificateSubjectIdentifier.subject(), SigningOptions.defaults());
    }

    /**
     * Returns a copy with a different {@link CertificateSubjectIdentifier}.
     */
    public KsefCertificateCredentials withSubjectIdentifier(CertificateSubjectIdentifier newStrategy) {
        return new KsefCertificateCredentials(certificate, privateKey, identifier, newStrategy, signingOptions);
    }

    /**
     * Returns a copy with different {@link SigningOptions}.
     */
    public KsefCertificateCredentials withSigningOptions(SigningOptions newOptions) {
        return new KsefCertificateCredentials(certificate, privateKey, identifier, subjectIdentifier, newOptions);
    }

    @Override
    public String toString() {
        return "KsefCertificateCredentials[identifier=" + identifier
                + ", subjectIdentifier=" + subjectIdentifier.wireType()
                + ", signingOptions=" + signingOptions + "]";
    }
}
