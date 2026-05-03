/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CsrRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CsrResult;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/**
 * Internal CSR (PKCS#10) generation. Auto-detects signature algorithm
 * from key type ({@code SHA256withRSA} for RSA, {@code SHA256withECDSA}
 * for EC). Used by {@code KsefCryptoService.generateCsr(...)}.
 *
 * <p>Spec: {@code ksef-docs/certyfikaty-KSeF.md} certificate enrollment.
 */
public final class CsrSupport {

    private static final String ALGORITHM_RSA_SHA256 = "SHA256withRSA";
    private static final String ALGORITHM_EC_SHA256 = "SHA256withECDSA";
    private static final String ERR_UNSUPPORTED_KEY = "Unsupported private key type for CSR signing: ";
    private static final String ERR_CSR_GENERATION = "Failed to generate CSR";

    private CsrSupport() { }

    public static CsrResult generate(CsrRequest request) {
        PrivateKey privateKey = request.keyPair().getPrivate();
        PublicKey publicKey = request.keyPair().getPublic();
        String signatureAlgorithm = selectSignatureAlgorithm(privateKey);
        try {
            X500Name subject = new X500Name(request.subjectDn());
            PKCS10CertificationRequestBuilder builder =
                    new JcaPKCS10CertificationRequestBuilder(subject, publicKey);
            ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);
            PKCS10CertificationRequest csr = builder.build(signer);
            return new CsrResult(csr.getEncoded(), request.keyPair());
        } catch (OperatorCreationException | IOException ex) {
            throw new KsefCryptoException(ERR_CSR_GENERATION, ex);
        }
    }

    private static String selectSignatureAlgorithm(PrivateKey key) {
        if (key instanceof RSAPrivateKey) {
            return ALGORITHM_RSA_SHA256;
        }
        if (key instanceof ECPrivateKey) {
            return ALGORITHM_EC_SHA256;
        }
        throw new KsefCryptoException(ERR_UNSUPPORTED_KEY + key.getAlgorithm(), null);
    }
}
