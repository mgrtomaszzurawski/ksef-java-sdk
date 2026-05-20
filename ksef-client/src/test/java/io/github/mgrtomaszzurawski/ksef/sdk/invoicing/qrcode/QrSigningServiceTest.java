/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrSigningService;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts {@link QrSigningService} produces signatures that verify with the
 * paired public key, both for RSA-PSS (RSA-PSS / SHA-256 / MGF1-SHA-256 /
 * salt 32) and ECDSA P-256 (output as IEEE P1363 R||S, 64 bytes).
 *
 * <p>Spec: REQ-QR-14, REQ-QR-16, REQ-QR-17 in
 * {@code context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md}; {@code kody-qr.md:197-210}.
 *
 * <p>Covers TC-QR-004, TC-QR-005, TC-QR-006.
 */
class QrSigningServiceTest {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String EC_ALGORITHM = "EC";
    private static final String EC_CURVE = "secp256r1";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String RSA_PSS_VERIFY_ALG = "RSASSA-PSS";
    private static final String ECDSA_VERIFY_ALG = "SHA256withECDSA";
    private static final int PSS_SALT_LENGTH = 32;
    private static final int PSS_TRAILER_FIELD = 1;
    private static final int ECDSA_P1363_BYTES = 64;
    private static final int ECDSA_COORDINATE_BYTES = 32;
    private static final int FAKE_INVOICE_HASH_LENGTH = 32;
    private static final byte FAKE_HASH_FILL_BYTE = (byte) 0xAB;
    private static final String SELLER_NIP = "1111111111";
    private static final String CONTEXT_VALUE = "1111111111";
    private static final String CERT_SERIAL = "0123456789ABCDEF";

    private final QrSigningService signingService = new QrSigningService();

    @Test
    void sign_withRsaKey_producesSignatureVerifiableByPublicKey() throws Exception {
        // given
        KeyPair rsaPair = generateRsaKeyPair();
        byte[] payload = signingPayload();

        // when
        byte[] signature = signingService.sign(payload, rsaPair.getPrivate());

        // then — verify RSA-PSS signature with the matching public key
        assertNotNull(signature);
        assertTrue(verifyRsaPss(rsaPair.getPublic(), payload, signature),
                "RSA-PSS signature must verify with the paired public key");
    }

    @Test
    void sign_withEcKey_producesP1363SignatureOf64Bytes() throws Exception {
        // given — P-256 key pair
        KeyPair ecPair = generateEcKeyPair();
        byte[] payload = signingPayload();

        // when
        byte[] signature = signingService.sign(payload, ecPair.getPrivate());

        // then — exactly 64 bytes (32-byte R || 32-byte S, fixed-length P1363)
        assertNotNull(signature);
        assertEquals(ECDSA_P1363_BYTES, signature.length,
                "ECDSA P-256 signature in P1363 form must be exactly " + ECDSA_P1363_BYTES + " bytes");
    }

    @Test
    void sign_withEcKey_producesSignatureVerifiableAfterDerReencoding() throws Exception {
        // given
        KeyPair ecPair = generateEcKeyPair();
        byte[] payload = signingPayload();

        // when — sign produces P1363 R||S; verifier wants DER, so we re-encode
        byte[] p1363 = signingService.sign(payload, ecPair.getPrivate());
        byte[] derEncodedSignature = p1363ToDer(p1363);

        // then
        assertTrue(verifyEcdsa(ecPair.getPublic(), payload, derEncodedSignature),
                "ECDSA signature converted back to DER must verify with the paired public key");
    }

    @Test
    void sign_withUnsupportedKeyAlgorithm_throwsKsefCryptoException() {
        // given — DSA is neither RSA nor EC
        PrivateKey unsupported = unsupportedPrivateKey();
        byte[] payload = signingPayload();

        // when / then
        assertThrows(KsefCryptoException.class,
                () -> signingService.sign(payload, unsupported));
    }

    @Test
    void sign_withNullPrivateKey_throwsNullPointerException() {
        byte[] payload = signingPayload();
        assertThrows(NullPointerException.class, () -> signingService.sign(payload, null));
    }

    @Test
    void sign_withNullPayload_throwsNullPointerException() throws Exception {
        PrivateKey key = generateRsaKeyPair().getPrivate();
        assertThrows(NullPointerException.class, () -> signingService.sign(null, key));
    }

    @Test
    void certificateVerificationUrl_withRsa_buildsUrlWithBase64UrlSignatureAppended() throws Exception {
        // given
        KeyPair rsa = generateRsaKeyPair();
        KsefVerificationLinks.CertificateSigningInput input = signingInput();

        // when
        String url = signingService.certificateVerificationUrl(QrEnvironment.TEST, input, rsa.getPrivate());

        // then — URL is well-formed, prefixed with TEST host, contains the cert serial,
        // and ends with a Base64URL-encoded segment that decodes back to a valid signature
        assertTrue(url.startsWith("https://qr-test.ksef.mf.gov.pl/certificate/"),
                "URL must start with TEST environment certificate path; got: " + url);
        assertTrue(url.contains(CERT_SERIAL),
                "URL must contain certificate serial; got: " + url);
        String[] parts = url.split("/");
        String signatureSegment = parts[parts.length - 1];
        byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureSegment);
        assertNotNull(signatureBytes);
        // Signature should verify against the canonical payload
        byte[] payload = KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.TEST, input);
        assertTrue(verifyRsaPss(rsa.getPublic(), payload, signatureBytes),
                "Signature in the URL must verify against the canonical payload");
    }

    @Test
    void certificateVerificationUrl_withEc_buildsUrlAndSignatureVerifiesAfterReencoding() throws Exception {
        // given
        KeyPair ec = generateEcKeyPair();
        KsefVerificationLinks.CertificateSigningInput input = signingInput();

        // when
        String url = signingService.certificateVerificationUrl(QrEnvironment.TEST, input, ec.getPrivate());

        // then
        String[] parts = url.split("/");
        byte[] p1363 = Base64.getUrlDecoder().decode(parts[parts.length - 1]);
        assertEquals(ECDSA_P1363_BYTES, p1363.length);
        byte[] payload = KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.TEST, input);
        assertTrue(verifyEcdsa(ec.getPublic(), payload, p1363ToDer(p1363)),
                "ECDSA signature in URL must verify after re-encoding to DER");
    }

    private static KsefVerificationLinks.CertificateSigningInput signingInput() {
        return new KsefVerificationLinks.CertificateSigningInput(
                QrContextType.NIP, CONTEXT_VALUE, SELLER_NIP, CERT_SERIAL, fakeHash());
    }

    private static byte[] fakeHash() {
        byte[] hash = new byte[FAKE_INVOICE_HASH_LENGTH];
        java.util.Arrays.fill(hash, FAKE_HASH_FILL_BYTE);
        return hash;
    }

    private static byte[] signingPayload() {
        return KsefVerificationLinks.canonicalCertificateSigningPayload(QrEnvironment.TEST, signingInput());
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        gen.initialize(RSA_KEY_SIZE, new SecureRandom());
        return gen.generateKeyPair();
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(EC_ALGORITHM);
        gen.initialize(new ECGenParameterSpec(EC_CURVE), new SecureRandom());
        return gen.generateKeyPair();
    }

    private static PrivateKey unsupportedPrivateKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
            gen.initialize(1024);
            return gen.generateKeyPair().getPrivate();
        } catch (Exception ex) {
            throw new IllegalStateException("DSA key generation unavailable on this JVM", ex);
        }
    }

    private static boolean verifyRsaPss(PublicKey publicKey, byte[] payload, byte[] signature) throws Exception {
        Signature verifier = Signature.getInstance(RSA_PSS_VERIFY_ALG);
        verifier.setParameter(new PSSParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSS_SALT_LENGTH, PSS_TRAILER_FIELD));
        verifier.initVerify(publicKey);
        verifier.update(payload);
        return verifier.verify(signature);
    }

    private static boolean verifyEcdsa(PublicKey publicKey, byte[] payload, byte[] derSignature) throws Exception {
        Signature verifier = Signature.getInstance(ECDSA_VERIFY_ALG);
        verifier.initVerify(publicKey);
        verifier.update(payload);
        return verifier.verify(derSignature);
    }

    /**
     * Re-encode P1363 R||S into ASN.1 DER {@code SEQUENCE(INTEGER r, INTEGER s)}
     * so the JCA verifier can consume it. Mirrors the inverse of
     * {@code derToP1363} inside {@link QrSigningService}.
     */
    private static byte[] p1363ToDer(byte[] p1363) throws Exception {
        if (p1363.length != ECDSA_P1363_BYTES) {
            throw new IllegalArgumentException("expected " + ECDSA_P1363_BYTES + " bytes, got " + p1363.length);
        }
        byte[] r = new byte[ECDSA_COORDINATE_BYTES];
        byte[] s = new byte[ECDSA_COORDINATE_BYTES];
        System.arraycopy(p1363, 0, r, 0, ECDSA_COORDINATE_BYTES);
        System.arraycopy(p1363, ECDSA_COORDINATE_BYTES, s, 0, ECDSA_COORDINATE_BYTES);
        org.bouncycastle.asn1.ASN1EncodableVector vec = new org.bouncycastle.asn1.ASN1EncodableVector();
        vec.add(new org.bouncycastle.asn1.ASN1Integer(new java.math.BigInteger(1, r)));
        vec.add(new org.bouncycastle.asn1.ASN1Integer(new java.math.BigInteger(1, s)));
        return new org.bouncycastle.asn1.DERSequence(vec).getEncoded();
    }
}
