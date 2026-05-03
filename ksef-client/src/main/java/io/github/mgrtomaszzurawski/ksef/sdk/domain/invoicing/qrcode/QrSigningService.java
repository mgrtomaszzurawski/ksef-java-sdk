/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Objects;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;

/**
 * KOD II QR signing convenience.
 *
 * <p>Owns a {@link PrivateKey} and produces a fully signed KOD II
 * verification URL. Auto-detects signature algorithm from key type
 * (RSA → RSASSA-PSS, EC → ECDSA P-256) per
 * {@code ksef-docs/kody-qr.md:197-210}.
 *
 * <p>This service does NOT replace the canonical-payload flow on
 * {@link KsefVerificationLinks#canonicalCertificateSigningPayload}; both
 * are public API. Use this service when you have a Java
 * {@link PrivateKey}; use the canonical-payload flow when signing is
 * external (HSM, offline signer, custom PKI).
 *
 * <p>Spec citations: REQ-QR-14, REQ-QR-16, REQ-QR-17 in
 * {@code context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md}; ADR-019
 * (KOD II signing scheme).
 */
public final class QrSigningService {

    private static final String ALG_RSA_PSS = "RSASSA-PSS";
    private static final String DIGEST_SHA256 = "SHA-256";
    private static final String MGF1_NAME = "MGF1";
    private static final int PSS_SALT_LENGTH = 32;
    private static final int PSS_TRAILER_FIELD = 1;
    private static final String ALG_ECDSA_SHA256 = "SHA256withECDSA";
    private static final int ECDSA_P256_COORDINATE_BYTES = 32;
    private static final int ECDSA_P1363_LENGTH = ECDSA_P256_COORDINATE_BYTES * 2;
    private static final String ERR_UNSUPPORTED_KEY = "Unsupported private key for KOD II signing: ";
    private static final String ERR_SIGNING_FAILED = "KOD II signing failed";
    private static final String ERR_DER_TO_P1363 = "Failed to convert ECDSA DER signature to IEEE P1363";

    /**
     * Sign the canonical KOD II payload and return the full verification URL.
     *
     * @param environment QR environment whose host is included in the signed bytes
     * @param input certificate-verification parameters (no signature)
     * @param privateKey RSA or EC private key from the consumer's KSeF Offline certificate
     * @return fully assembled KOD II URL with Base64URL-encoded signature appended
     */
    public String certificateVerificationUrl(QrEnvironment environment,
                                              KsefVerificationLinks.CertificateSigningInput input,
                                              PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        byte[] payload = KsefVerificationLinks.canonicalCertificateSigningPayload(environment, input);
        byte[] signature = sign(payload, privateKey);
        KsefVerificationLinks.CertificateVerificationParams params =
                new KsefVerificationLinks.CertificateVerificationParams(
                        input.contextType(),
                        input.contextValue(),
                        input.sellerNip(),
                        input.certificateSerial(),
                        input.invoiceSha256(),
                        signature);
        return KsefVerificationLinks.buildCertificateVerificationUrl(environment, params);
    }

    /**
     * Sign the canonical payload bytes with auto-detected algorithm. Public so
     * advanced consumers can inspect the bytes before composing the URL.
     */
    public byte[] sign(byte[] payload, PrivateKey privateKey) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        try {
            if (privateKey instanceof RSAPrivateKey rsa) {
                return signRsaPss(payload, rsa);
            }
            if (privateKey instanceof ECPrivateKey ec) {
                return signEcdsaP1363(payload, ec);
            }
            throw new KsefCryptoException(ERR_UNSUPPORTED_KEY + privateKey.getAlgorithm(), null);
        } catch (GeneralSecurityException ex) {
            throw new KsefCryptoException(ERR_SIGNING_FAILED, ex);
        }
    }

    private static byte[] signRsaPss(byte[] payload, RSAPrivateKey key) throws GeneralSecurityException {
        Signature signer = Signature.getInstance(ALG_RSA_PSS);
        signer.setParameter(new PSSParameterSpec(
                DIGEST_SHA256,
                MGF1_NAME,
                MGF1ParameterSpec.SHA256,
                PSS_SALT_LENGTH,
                PSS_TRAILER_FIELD));
        signer.initSign(key);
        signer.update(payload);
        return signer.sign();
    }

    private static byte[] signEcdsaP1363(byte[] payload, ECPrivateKey key) throws GeneralSecurityException {
        Signature signer = Signature.getInstance(ALG_ECDSA_SHA256);
        signer.initSign(key);
        signer.update(payload);
        byte[] derSignature = signer.sign();
        return derToP1363(derSignature);
    }

    /**
     * Re-encode a JCA-produced DER ECDSA signature as IEEE P1363 R||S
     * (fixed-length concatenation). KSeF KOD II prefers P1363 per
     * {@code kody-qr.md:208-210}.
     */
    private static byte[] derToP1363(byte[] der) {
        try {
            ASN1Sequence seq = ASN1Sequence.getInstance(der);
            byte[] r = ((ASN1Integer) seq.getObjectAt(0)).getPositiveValue().toByteArray();
            byte[] s = ((ASN1Integer) seq.getObjectAt(1)).getPositiveValue().toByteArray();
            byte[] out = new byte[ECDSA_P1363_LENGTH];
            copyRightAligned(r, out, 0, ECDSA_P256_COORDINATE_BYTES);
            copyRightAligned(s, out, ECDSA_P256_COORDINATE_BYTES, ECDSA_P256_COORDINATE_BYTES);
            return out;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new KsefCryptoException(ERR_DER_TO_P1363, ex);
        }
    }

    private static void copyRightAligned(byte[] source, byte[] dest, int destOffset, int width) {
        int sourceOffset = 0;
        // Strip leading zero pad if BigInteger sign byte made it longer than needed
        if (source.length > width) {
            sourceOffset = source.length - width;
        }
        int copyLen = Math.min(source.length - sourceOffset, width);
        int destStart = destOffset + (width - copyLen);
        System.arraycopy(source, sourceOffset, dest, destStart, copyLen);
    }
}
