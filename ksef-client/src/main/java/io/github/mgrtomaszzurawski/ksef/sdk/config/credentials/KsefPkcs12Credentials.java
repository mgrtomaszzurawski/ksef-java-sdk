/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config.credentials;

import io.github.mgrtomaszzurawski.ksef.sdk.config.AuthMethod;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCredentialsDescriptor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * PKCS#12 keystore-based authentication credentials.
 *
 * <p>Convenience variant of {@link KsefCertificateCredentials} that loads
 * the certificate and private key from a PKCS#12 (.p12) keystore file at
 * authentication time. The first alias in the keystore is used
 * automatically.
 *
 * <p>The {@code subjectIdentifier} field mirrors the same option on
 * {@link KsefCertificateCredentials} — see
 * {@link CertificateSubjectIdentifier} for {@code certificateSubject} vs
 * {@code certificateFingerprint} variants. Default
 * {@link CertificateSubjectIdentifier#subject()} preserves pre-1.0
 * behavior.
 *
 * @param keystorePath path to the PKCS#12 keystore file
 * @param password keystore and private key password
 * @param identifier authentication context identifier (NIP, internal id, EU VAT, or Peppol)
 * @param subjectIdentifier strategy for the {@code SubjectIdentifierType}
 *     XML element (REQ-AUTH-033)
 *
 * @since 0.1.0
 */
public record KsefPkcs12Credentials(
        Path keystorePath,
        char[] password,
        KsefIdentifier identifier,
        CertificateSubjectIdentifier subjectIdentifier
) implements KsefCredentials {

    private static final String ERR_NULL_PATH = "keystorePath must not be null";
    private static final String ERR_NULL_PASSWORD = "password must not be null";
    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";
    private static final String ERR_NULL_SUBJECT_ID = "subjectIdentifier must not be null";

    /**
     * Canonical constructor — validates non-null fields and defensively
     * clones the password array.
     */
    public KsefPkcs12Credentials {
        Objects.requireNonNull(keystorePath, ERR_NULL_PATH);
        Objects.requireNonNull(password, ERR_NULL_PASSWORD);
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
        Objects.requireNonNull(subjectIdentifier, ERR_NULL_SUBJECT_ID);
        password = password.clone();
    }

    /**
     * Convenience constructor — defaults
     * {@code subjectIdentifier} to {@link CertificateSubjectIdentifier#subject()}.
     */
    public KsefPkcs12Credentials(Path keystorePath, char[] password, KsefIdentifier identifier) {
        this(keystorePath, password, identifier, CertificateSubjectIdentifier.subject());
    }

    /**
     * Convenience constructor — accepts a plain NIP string and
     * defaults to the {@code certificateSubject} identifier strategy.
     *
     * @param keystorePath path to PKCS#12 keystore
     * @param password keystore and private key password
     * @param nip 10-digit Polish tax identification number
     */
    public KsefPkcs12Credentials(Path keystorePath, char[] password, String nip) {
        this(keystorePath, password, KsefIdentifier.nip(nip), CertificateSubjectIdentifier.subject());
    }

    @Override
    public char[] password() {
        return password.clone();
    }

    /**
     * Zeroise the stored password char array. Idempotent. Call once
     * authentication has completed to remove the password from heap before
     * garbage collection. After {@code clearPassword()} subsequent calls
     * to {@link #password()} return an array of zeroes.
     */
    public void clearPassword() {
        Arrays.fill(password, '\0');
    }

    /**
     * Returns a copy of these credentials with a different
     * {@link CertificateSubjectIdentifier} strategy.
     */
    public KsefPkcs12Credentials withSubjectIdentifier(CertificateSubjectIdentifier newStrategy) {
        return new KsefPkcs12Credentials(keystorePath, password, identifier, newStrategy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KsefPkcs12Credentials other)) {
            return false;
        }
        return Objects.equals(keystorePath, other.keystorePath)
                && Arrays.equals(password, other.password)
                && Objects.equals(identifier, other.identifier)
                && Objects.equals(subjectIdentifier, other.subjectIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keystorePath, identifier, subjectIdentifier, Arrays.hashCode(password));
    }

    @Override
    public String toString() {
        return "KsefPkcs12Credentials[identifier=" + identifier
                + ", subjectIdentifier=" + subjectIdentifier.wireType()
                + ", keystorePath=" + keystorePath + "]";
    }

    @Override
    public KsefCredentialsDescriptor asDescriptor() {
        return KsefCredentialsDescriptor.of(AuthMethod.PKCS12, identifier);
    }
}
