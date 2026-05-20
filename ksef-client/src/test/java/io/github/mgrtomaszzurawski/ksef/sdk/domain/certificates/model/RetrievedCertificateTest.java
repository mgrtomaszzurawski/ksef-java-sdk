/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.TestCertificates;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link RetrievedCertificate#from(byte[], String, CertificateSerialNumber, KsefCertificateType)}
 * DER parsing + the X.509-derived accessors (validFrom, validTo,
 * subjectName, issuerName, publicKey, isExpired, isExpiredAt) plus
 * defensive copy on {@code der()} and structural equality.
 */
class RetrievedCertificateTest {

    private static final String CERT_NAME = "SDK Test Cert";
    private static final CertificateSerialNumber SERIAL = CertificateSerialNumber.parse("01");
    private static final KsefCertificateType TYPE = KsefCertificateType.AUTHENTICATION;

    private static byte[] derBytes;
    private static X509Certificate certificate;

    @BeforeAll
    static void initCertificate() throws Exception {
        TestCertificates pair = TestCertificates.generateRsa();
        certificate = pair.certificate();
        derBytes = certificate.getEncoded();
    }

    @Test
    void from_parsesValidDerAndExposesAccessors() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        assertNotNull(cert.certificate());
        assertEquals(CERT_NAME, cert.certificateName());
        assertEquals(SERIAL, cert.certificateSerialNumber());
        assertEquals(TYPE, cert.certificateType());
        assertNotNull(cert.publicKey());
        assertNotNull(cert.subjectName());
        assertNotNull(cert.issuerName());
    }

    @Test
    void from_malformedDerThrowsKsefCryptoException() {
        byte[] garbage = "not-a-der-encoded-cert".getBytes(StandardCharsets.UTF_8);
        assertThrows(KsefCryptoException.class,
                () -> RetrievedCertificate.from(garbage, CERT_NAME, SERIAL, TYPE));
    }

    @Test
    void from_rejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> RetrievedCertificate.from(null, CERT_NAME, SERIAL, TYPE));
        assertThrows(NullPointerException.class,
                () -> RetrievedCertificate.from(derBytes, null, SERIAL, TYPE));
        assertThrows(NullPointerException.class,
                () -> RetrievedCertificate.from(derBytes, CERT_NAME, null, TYPE));
        assertThrows(NullPointerException.class,
                () -> RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, null));
    }

    @Test
    void der_returnsFreshCopyEachCall() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        byte[] first = cert.der();
        byte[] second = cert.der();
        assertNotSame(first, second);
        assertEquals(first.length, second.length);
        first[0] = (byte) 0xFF;
        // Mutating returned bytes must not affect subsequent reads.
        assertNotEquals((byte) 0xFF, cert.der()[0]);
    }

    @Test
    void validFromAndValidTo_matchUnderlyingCertificateDates() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        LocalDate expectedFrom = certificate.getNotBefore().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate expectedTo = certificate.getNotAfter().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        assertEquals(expectedFrom, cert.validFrom());
        assertEquals(expectedTo, cert.validTo());
    }

    @Test
    void isExpired_withClockBeforeNotAfter_returnsFalse() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        Clock currentClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        assertFalse(cert.isExpired(currentClock));
    }

    @Test
    void isExpired_withClockAfterNotAfter_returnsTrue() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        Instant farFuture = certificate.getNotAfter().toInstant().plusSeconds(86_400);
        Clock future = Clock.fixed(farFuture, ZoneId.systemDefault());
        assertTrue(cert.isExpired(future));
    }

    @Test
    void isExpired_nullClockThrows() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        assertThrows(NullPointerException.class, () -> cert.isExpired(null));
    }

    @Test
    void isExpiredAt_withDateBeforeValidTo_returnsFalse() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        assertFalse(cert.isExpiredAt(LocalDate.now()));
    }

    @Test
    void isExpiredAt_withDateAfterValidTo_returnsTrue() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        assertTrue(cert.isExpiredAt(cert.validTo().plusDays(1)));
    }

    @Test
    void isExpiredAt_nullDateThrows() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        assertThrows(NullPointerException.class, () -> cert.isExpiredAt(null));
    }

    @Test
    void equalsAndHashCode_areStructural() {
        RetrievedCertificate first = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        RetrievedCertificate second = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equals_isFalseWhenCertificateNameDiffers() {
        RetrievedCertificate first = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        RetrievedCertificate second = RetrievedCertificate.from(derBytes, "Other name", SERIAL, TYPE);
        assertNotEquals(first, second);
    }

    @Test
    void equals_isFalseAgainstUnrelatedType() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        assertNotEquals("not-a-cert", cert);
    }

    @Test
    void toString_carriesNameSerialAndType() {
        RetrievedCertificate cert = RetrievedCertificate.from(derBytes, CERT_NAME, SERIAL, TYPE);
        String rendered = cert.toString();
        assertTrue(rendered.contains(CERT_NAME));
        assertTrue(rendered.contains(SERIAL.value()));
        assertTrue(rendered.contains(TYPE.name()));
    }
}
