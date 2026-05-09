/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;

/**
 * A retrieved KSeF certificate with parsed X.509 surface plus the raw
 * DER bytes for archive / forward use.
 *
 * <p>Constructed only via {@link #from(byte[], String, String, KsefCertificateType)}.
 * The factory parses the DER and throws {@link KsefCryptoException} on
 * malformed input.
 *
 * @since 1.0.0
 */
public final class RetrievedCertificate {

    private static final String CERT_TYPE_X509 = "X.509";
    private static final String ERR_PARSE = "Failed to parse RetrievedCertificate DER bytes";
    private static final String ERR_NULL_DER = "der";
    private static final String ERR_NULL_NAME = "certificateName";
    private static final String ERR_NULL_SERIAL = "certificateSerialNumber";
    private static final String ERR_NULL_TYPE = "certificateType";
    private static final String ERR_NULL_CLOCK = "clock";
    private static final String ERR_NULL_DATE = "date";

    private final byte[] der;
    private final X509Certificate certificate;
    private final String certificateName;
    private final String certificateSerialNumber;
    private final KsefCertificateType certificateType;

    private RetrievedCertificate(byte[] der,
                                  X509Certificate certificate,
                                  String certificateName,
                                  String certificateSerialNumber,
                                  KsefCertificateType certificateType) {
        this.der = der;
        this.certificate = certificate;
        this.certificateName = certificateName;
        this.certificateSerialNumber = certificateSerialNumber;
        this.certificateType = certificateType;
    }

    /**
     * Parse the DER bytes and build a fully-typed instance.
     *
     * @throws KsefCryptoException when the bytes do not represent a valid
     *     DER-encoded X.509 certificate
     */
    public static RetrievedCertificate from(byte[] der,
                                              String certificateName,
                                              String certificateSerialNumber,
                                              KsefCertificateType certificateType) {
        Objects.requireNonNull(der, ERR_NULL_DER);
        Objects.requireNonNull(certificateName, ERR_NULL_NAME);
        Objects.requireNonNull(certificateSerialNumber, ERR_NULL_SERIAL);
        Objects.requireNonNull(certificateType, ERR_NULL_TYPE);
        byte[] derCopy = der.clone();
        X509Certificate parsed = parseDer(derCopy);
        return new RetrievedCertificate(derCopy, parsed, certificateName,
                certificateSerialNumber, certificateType);
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

    /** Consumer-assigned certificate name (set at enrollment). */
    public String certificateName() {
        return certificateName;
    }

    /** X.509 serial number as reported by the server (hex string). */
    public String certificateSerialNumber() {
        return certificateSerialNumber;
    }

    /** Type of KSeF certificate (Authentication or Offline). */
    public KsefCertificateType certificateType() {
        return certificateType;
    }

    /** Validity start (X.509 {@code notBefore}) as a {@link LocalDate}. */
    public LocalDate validFrom() {
        return certificate.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Validity end (X.509 {@code notAfter}) as a {@link LocalDate}. */
    public LocalDate validTo() {
        return certificate.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Subject distinguished name in RFC 2253 format. */
    public String subjectName() {
        return certificate.getSubjectX500Principal().getName();
    }

    /** Issuer distinguished name in RFC 2253 format. */
    public String issuerName() {
        return certificate.getIssuerX500Principal().getName();
    }

    /** Public key extracted from the parsed certificate. */
    public PublicKey publicKey() {
        return certificate.getPublicKey();
    }

    /**
     * Whether the X.509 {@code notAfter} is in the past relative to the
     * supplied clock.
     */
    public boolean isExpired(Clock clock) {
        Objects.requireNonNull(clock, ERR_NULL_CLOCK);
        return clock.instant().isAfter(certificate.getNotAfter().toInstant());
    }

    /**
     * Whether the X.509 {@code notAfter} is on or before the supplied
     * date (compared as {@link LocalDate} in the system default zone).
     */
    public boolean isExpiredAt(LocalDate date) {
        Objects.requireNonNull(date, ERR_NULL_DATE);
        LocalDate notAfter = certificate.getNotAfter().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return !date.isBefore(notAfter);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RetrievedCertificate that)) {
            return false;
        }
        return Arrays.equals(der, that.der)
                && Objects.equals(certificateName, that.certificateName)
                && Objects.equals(certificateSerialNumber, that.certificateSerialNumber)
                && Objects.equals(certificateType, that.certificateType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificateName, certificateSerialNumber, certificateType, Arrays.hashCode(der));
    }

    @Override
    public String toString() {
        return "RetrievedCertificate[der=byte[" + der.length + "]"
                + ", certificateName=" + certificateName
                + ", certificateSerialNumber=" + certificateSerialNumber
                + ", certificateType=" + certificateType + "]";
    }
}
