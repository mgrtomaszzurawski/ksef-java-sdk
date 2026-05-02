/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Shared test utility for generating self-signed certificates.
 * Used across test classes to avoid duplicating keypair/cert generation logic.
 */
public final class TestCertificates {

    private static final int RSA_KEY_SIZE = 2048;
    private static final int CERT_VALIDITY_DAYS = 365;
    private static final String CERT_SUBJECT = "CN=Test";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String SHA256_WITH_RSA = "SHA256WithRSA";

    private final X509Certificate certificate;
    private final PrivateKey privateKey;

    private TestCertificates(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public static TestCertificates generateRsa() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        X509Certificate cert = buildSelfSignedCertificate(keyPair);
        return new TestCertificates(cert, keyPair.getPrivate());
    }

    private static X509Certificate buildSelfSignedCertificate(KeyPair keyPair) throws Exception {
        X500Name issuer = new X500Name(CERT_SUBJECT);
        Instant notBefore = Instant.now();
        Instant notAfter = notBefore.plus(CERT_VALIDITY_DAYS, ChronoUnit.DAYS);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, BigInteger.ONE,
                Date.from(notBefore), Date.from(notAfter),
                issuer, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(SHA256_WITH_RSA).build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
}
