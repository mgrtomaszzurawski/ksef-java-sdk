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
 * <p><strong>VAT-UE callers (EU-entity flows):</strong> KSeF rejects the
 * intuitive direct-auth path for EU entities. The working contract is:
 * <ol>
 *   <li>The Polish owner first grants {@code EuEntityAdminPermission}
 *       to the EU entity's certificate — the grant subject is the
 *       SHA-256 hex fingerprint of the certificate's public key.</li>
 *   <li>The EU entity's self-signed certificate must use
 *       {@code organizationIdentifier} RDN of {@code VATPL-{ownerNip}}
 *       (not the compound {@code {ownerNip}-{country}{specific}} —
 *       that shape is rejected with KSeF code 21117).</li>
 *   <li>Authenticate with {@code subjectIdentifier} set to
 *       {@link CertificateSubjectIdentifier#fingerprint(String)} and
 *       {@code identifier} set to
 *       {@link KsefIdentifier#nipVatUe(String)} carrying the compound
 *       {@code {ownerNip}-{country}{specific}}.</li>
 * </ol>
 * Skipping step 1 yields KSeF code 21117 (invalid identifier);
 * inverting subject/context yields code 410 (mismatched identifiers).
 *
 * @param certificate X.509 certificate (qualified or test)
 * @param privateKey private key corresponding to the certificate
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 * @param subjectIdentifier strategy for the {@code SubjectIdentifierType} XML element
 * @param signingOptions XAdES profile + digest options
 *
 * @since 1.0.0
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
     * Convenience constructor — accepts a plain NIP string with
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
