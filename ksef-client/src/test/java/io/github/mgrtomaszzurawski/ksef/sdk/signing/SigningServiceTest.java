/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.signing;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningServiceTest {

    private static final String TEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Root><Data>test</Data></Root>";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int CERT_VALIDITY_DAYS = 365;
    private static final String XADES_SIGNATURE_TAG = "<ds:Signature";
    private static final String SIGNED_INFO_TAG = "SignedInfo";
    private static final String ORIGINAL_CONTENT_TAG = "<Root>";
    private static final String CERT_SUBJECT = "CN=Test";
    private static final String SHA256_WITH_RSA = "SHA256WithRSA";

    @Test
    void signXml_whenValidRsaKeyPair_producesXmlWithSignatureAndOriginalContent() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate certificate = generateSelfSignedCertificate(keyPair);

        // when
        String signedXml = SigningService.signXml(TEST_XML.getBytes(), certificate, keyPair.getPrivate());

        // then
        assertNotNull(signedXml);
        assertTrue(signedXml.contains(XADES_SIGNATURE_TAG), "Should contain XAdES Signature element");
        assertTrue(signedXml.contains(SIGNED_INFO_TAG), "Should contain SignedInfo element");
        assertTrue(signedXml.contains(ORIGINAL_CONTENT_TAG), "Should preserve original content");
        assertTrue(signedXml.length() > TEST_XML.length(), "Signed XML should be longer than original");
    }

    @Test
    void signXml_whenNullInput_throwsCryptoException() {
        // then
        assertThrows(KsefCryptoException.class,
                () -> SigningService.signXml(null, null, null));
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        return keyPairGen.generateKeyPair();
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
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
