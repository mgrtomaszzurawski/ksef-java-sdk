/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * KSeF public key certificate for encryption operations.
 *
 * <p>Holds a parsed {@link X509Certificate} plus the raw DER bytes for
 * archive / forward use. Convenience accessors expose validity dates,
 * subject name, and the {@link PublicKey} so consumers do not have to
 * re-parse the X.509 themselves.
 *
 * <p>Constructed only via {@link #from(byte[], OffsetDateTime, OffsetDateTime, List)}
 * — the factory parses the DER and throws {@link KsefCryptoException} on
 * malformed input rather than returning an unusable instance.
 *
 * @since 1.0.0
 */
public final class PublicKeyCertificate {

    private static final String CERT_TYPE_X509 = "X.509";
    private static final String ERR_PARSE = "Failed to parse PublicKeyCertificate DER bytes";
    private static final String ERR_NULL_DER = "der";
    private static final String ERR_NULL_VALID_FROM = "validFrom";
    private static final String ERR_NULL_VALID_TO = "validTo";
    private static final String ERR_NULL_CLOCK = "clock";

    private final byte[] der;
    private final X509Certificate certificate;
    private final OffsetDateTime validFrom;
    private final OffsetDateTime validTo;
    private final List<PublicKeyCertificateUsage> usage;

    private PublicKeyCertificate(byte[] der,
                                  X509Certificate certificate,
                                  OffsetDateTime validFrom,
                                  OffsetDateTime validTo,
                                  List<PublicKeyCertificateUsage> usage) {
        this.der = der;
        this.certificate = certificate;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.usage = usage;
    }

    /**
     * Parse the DER bytes and build a fully-typed instance.
     *
     * @throws KsefCryptoException when the bytes do not represent a valid
     *     DER-encoded X.509 certificate
     */
    public static PublicKeyCertificate from(byte[] der,
                                              OffsetDateTime validFrom,
                                              OffsetDateTime validTo,
                                              List<PublicKeyCertificateUsage> usage) {
        Objects.requireNonNull(der, ERR_NULL_DER);
        Objects.requireNonNull(validFrom, ERR_NULL_VALID_FROM);
        Objects.requireNonNull(validTo, ERR_NULL_VALID_TO);
        byte[] derCopy = der.clone();
        X509Certificate parsed = parseDer(derCopy);
        List<PublicKeyCertificateUsage> usageCopy = usage == null ? List.of() : List.copyOf(usage);
        return new PublicKeyCertificate(derCopy, parsed, validFrom, validTo, usageCopy);
    }

    private static X509Certificate parseDer(byte[] der) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE_X509);
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
        } catch (CertificateException certificateFailure) {
            throw new KsefCryptoException(ERR_PARSE, certificateFailure);
        }
    }

    /** Parsed X.509 certificate. */
    public X509Certificate certificate() {
        return certificate;
    }

    /**
     * Raw DER bytes — defensive copy, safe to mutate. Useful for archive
     * or forwarding to systems that expect raw bytes.
     */
    public byte[] der() {
        return der.clone();
    }

    /** Server-reported validity start. */
    public OffsetDateTime validFrom() {
        return validFrom;
    }

    /** Server-reported validity end. */
    public OffsetDateTime validTo() {
        return validTo;
    }

    /** Permitted usages for this certificate. */
    public List<PublicKeyCertificateUsage> usage() {
        return usage;
    }

    /** Public key extracted from the parsed certificate. */
    public PublicKey publicKey() {
        return certificate.getPublicKey();
    }

    /** Subject distinguished name in RFC 2253 format. */
    public String subjectName() {
        return certificate.getSubjectX500Principal().getName();
    }

    /**
     * Whether the server-reported validity end has elapsed by the given
     * clock. The server-reported {@code validTo} is the source of truth
     * for KSeF rotation, not the embedded {@code notAfter}.
     */
    public boolean isExpired(Clock clock) {
        Objects.requireNonNull(clock, ERR_NULL_CLOCK);
        return OffsetDateTime.ofInstant(clock.instant(), validTo.getOffset()).isAfter(validTo);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PublicKeyCertificate that)) {
            return false;
        }
        return Arrays.equals(der, that.der)
                && Objects.equals(validFrom, that.validFrom)
                && Objects.equals(validTo, that.validTo)
                && Objects.equals(usage, that.usage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validFrom, validTo, usage, Arrays.hashCode(der));
    }

    @Override
    public String toString() {
        return "PublicKeyCertificate[der=byte[" + der.length + "]"
                + ", validFrom=" + validFrom
                + ", validTo=" + validTo
                + ", usage=" + usage + "]";
    }
}
