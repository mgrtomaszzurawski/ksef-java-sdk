/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config.credentials;

import java.util.Objects;

/**
 * Strategy for the {@code SubjectIdentifierType} XML element in the KSeF
 * authentication request.
 *
 * <p>The KSeF spec ({@code ksef-docs/auth/podpis-xades.md}) supports two
 * variants of how the request identifies the certificate:
 *
 * <ul>
 *   <li>{@link Subject} — the certificate's Subject DN is used. This is the
 *       default and works for qualified certificates with parseable
 *       Subject attributes (givenName/surname/serialNumber for individuals,
 *       organizationName/organizationIdentifier for organizations).</li>
 *   <li>{@link Fingerprint} — the SHA-256 fingerprint of the DER-encoded
 *       certificate is used as identifier. Required when the certificate's
 *       Subject doesn't expose the spec-mandated attributes; the spec at
 *       {@code auth/podpis-xades.md:140-142} explicitly lists fingerprint
 *       as the fallback identifier mode.</li>
 * </ul>
 *
 * <p>The SDK auto-emits the matching XML element value
 * ({@code certificateSubject} or {@code certificateFingerprint}) when
 * building the {@code AuthTokenRequest} XML.
 *
 * <p>Spec citation: REQ-AUTH-027, REQ-AUTH-033.
 *
 * @since 0.1.0
 */
public sealed interface CertificateSubjectIdentifier
        permits CertificateSubjectIdentifier.Subject,
                CertificateSubjectIdentifier.Fingerprint {

    /**
     * Default identifier strategy — the certificate Subject DN is used.
     */
    static CertificateSubjectIdentifier subject() {
        return Subject.INSTANCE;
    }

    /**
     * Fingerprint identifier strategy — uses the supplied SHA-256
     * fingerprint of the DER-encoded certificate as the identifier.
     *
     * @param fingerprintHex SHA-256 fingerprint as a hex string (case
     *     insensitive; uppercase preferred). Length is not validated here
     *     because the spec doesn't mandate a specific format beyond
     *     "fingerprint of the certificate"; downstream KSeF rejects
     *     unknown fingerprints.
     */
    static CertificateSubjectIdentifier fingerprint(String fingerprintHex) {
        return new Fingerprint(fingerprintHex);
    }

    /**
     * Returns the wire value to emit in the {@code SubjectIdentifierType}
     * XML element.
     */
    String wireType();

    /**
     * Subject DN strategy. Singleton — no per-instance state.
     */
    record Subject() implements CertificateSubjectIdentifier {
        private static final Subject INSTANCE = new Subject();
        private static final String WIRE_TYPE_SUBJECT = "certificateSubject";

        @Override
        public String wireType() {
            return WIRE_TYPE_SUBJECT;
        }
    }

    /**
     * Fingerprint strategy.
     */
    record Fingerprint(String fingerprintHex) implements CertificateSubjectIdentifier {
        private static final String WIRE_TYPE_FINGERPRINT = "certificateFingerprint";
        private static final String ERR_NULL_FINGERPRINT = "fingerprintHex must not be null";
        private static final String ERR_BLANK_FINGERPRINT = "fingerprintHex must not be blank";

        public Fingerprint {
            Objects.requireNonNull(fingerprintHex, ERR_NULL_FINGERPRINT);
            if (fingerprintHex.isBlank()) {
                throw new IllegalArgumentException(ERR_BLANK_FINGERPRINT);
            }
        }

        @Override
        public String wireType() {
            return WIRE_TYPE_FINGERPRINT;
        }
    }
}
