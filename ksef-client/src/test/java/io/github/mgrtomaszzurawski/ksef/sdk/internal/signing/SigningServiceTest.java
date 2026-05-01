/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.signing;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.signing.SigningService;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningServiceTest {

    private static final String TEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Root><Data>test</Data></Root>";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String EC_ALGORITHM = "EC";
    private static final String EC_CURVE = "secp256r1";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int CERT_VALIDITY_DAYS = 365;
    private static final String XADES_SIGNATURE_TAG = "<ds:Signature";
    private static final String SIGNED_INFO_TAG = "SignedInfo";
    private static final String ORIGINAL_CONTENT_TAG = "<Root>";
    private static final String CERT_SUBJECT = "CN=Test";
    private static final String SHA256_WITH_RSA = "SHA256WithRSA";
    private static final String SHA256_WITH_ECDSA = "SHA256WithECDSA";

    @Test
    void signXml_whenValidRsaKeyPair_producesXmlWithSignatureAndOriginalContent() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate certificate = generateSelfSignedCertificate(keyPair, SHA256_WITH_RSA);

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
    void signXml_whenValidEcdsaKeyPair_producesXmlWithSignatureAndOriginalContent() throws Exception {
        // given
        KeyPair keyPair = generateEcKeyPair();
        X509Certificate certificate = generateSelfSignedCertificate(keyPair, SHA256_WITH_ECDSA);

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
    void signXml_whenNullXmlContent_throwsCryptoException() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate certificate = generateSelfSignedCertificate(keyPair, SHA256_WITH_RSA);

        // then
        java.security.PrivateKey privateKey = keyPair.getPrivate();
        assertThrows(KsefCryptoException.class,
                () -> SigningService.signXml(null, certificate, privateKey));
    }

    @Test
    void signXml_whenNullCertificate_throwsCryptoException() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        byte[] xml = TEST_XML.getBytes();
        java.security.PrivateKey privateKey = keyPair.getPrivate();

        // then
        assertThrows(KsefCryptoException.class,
                () -> SigningService.signXml(xml, null, privateKey));
    }

    @Test
    void signXml_whenNullPrivateKey_throwsCryptoException() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate certificate = generateSelfSignedCertificate(keyPair, SHA256_WITH_RSA);
        byte[] xml = TEST_XML.getBytes();

        // then
        assertThrows(KsefCryptoException.class,
                () -> SigningService.signXml(xml, certificate, null));
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        return keyPairGen.generateKeyPair();
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM);
        keyPairGen.initialize(new ECGenParameterSpec(EC_CURVE));
        return keyPairGen.generateKeyPair();
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String sigAlgorithm) throws Exception {
        X500Name issuer = new X500Name(CERT_SUBJECT);
        Instant notBefore = Instant.now();
        Instant notAfter = notBefore.plus(CERT_VALIDITY_DAYS, ChronoUnit.DAYS);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, BigInteger.ONE,
                Date.from(notBefore), Date.from(notAfter),
                issuer, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(sigAlgorithm).build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
}
