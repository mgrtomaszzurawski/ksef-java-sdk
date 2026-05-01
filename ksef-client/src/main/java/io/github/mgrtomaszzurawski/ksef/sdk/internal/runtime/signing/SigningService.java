/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.signing;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * XAdES-BASELINE-B signature service for KSeF authentication.
 * Signs XML documents with qualified certificates (RSA or ECDSA).
 */
public final class SigningService {

    private static final String ERR_SIGN_FAILED = "XAdES signing failed";
    private static final String ERR_KEY_MISMATCH = "Certificate and private key algorithm mismatch";
    private static final String PKCS12_TYPE = "PKCS12";
    private static final String KEYSTORE_ALIAS = "signing-key";
    private static final int EC_FIELD_SIZE_256 = 256;
    private static final int EC_FIELD_SIZE_384 = 384;

    private SigningService() {
    }

    /**
     * Sign XML content with XAdES-BASELINE-B enveloped signature.
     *
     * @param xmlContent the XML bytes to sign
     * @param certificate the signing certificate
     * @param privateKey the private key matching the certificate
     * @return the signed XML as a string
     */
    public static String signXml(byte[] xmlContent, X509Certificate certificate, PrivateKey privateKey) {
        if (xmlContent == null || certificate == null || privateKey == null) {
            throw new KsefCryptoException(ERR_SIGN_FAILED,
                    new IllegalArgumentException("xmlContent, certificate, and privateKey must not be null"));
        }
        try {
            DSSDocument document = new InMemoryDocument(xmlContent, null, MimeTypeEnum.XML);
            XAdESSignatureParameters parameters = buildParameters(certificate, privateKey);
            XAdESService xadesService = new XAdESService(new CommonCertificateVerifier());

            ToBeSigned dataToSign = xadesService.getDataToSign(document, parameters);
            SignatureValue signatureValue = createSignatureValue(dataToSign, certificate, privateKey);
            DSSDocument signedDocument = xadesService.signDocument(document, parameters, signatureValue);

            try (InputStream stream = signedDocument.openStream()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException | DSSException exception) {
            throw new KsefCryptoException(ERR_SIGN_FAILED, exception);
        }
    }

    private static XAdESSignatureParameters buildParameters(X509Certificate certificate, PrivateKey privateKey) {
        XAdESSignatureParameters parameters = new XAdESSignatureParameters();
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
        parameters.setEn319132(false);

        EncryptionAlgorithm encryptionAlgorithm;
        DigestAlgorithm digestAlgorithm = DigestAlgorithm.SHA256;

        if (isRsaPair(certificate, privateKey)) {
            encryptionAlgorithm = EncryptionAlgorithm.RSA;
        } else if (isEcdsaPair(certificate, privateKey)) {
            encryptionAlgorithm = EncryptionAlgorithm.ECDSA;
            digestAlgorithm = selectEcDigest(certificate);
        } else {
            throw new KsefCryptoException(ERR_KEY_MISMATCH, null);
        }

        parameters.setEncryptionAlgorithm(encryptionAlgorithm);
        parameters.setDigestAlgorithm(digestAlgorithm);
        parameters.setSigningCertificateDigestMethod(digestAlgorithm);
        parameters.setSigningCertificate(new CertificateToken(certificate));
        return parameters;
    }

    private static SignatureValue createSignatureValue(
            ToBeSigned toBeSigned, X509Certificate certificate, PrivateKey privateKey) {
        try {
            KeyStore keyStore = KeyStore.getInstance(PKCS12_TYPE);
            keyStore.load(null, null);
            keyStore.setKeyEntry(KEYSTORE_ALIAS, privateKey, null, new Certificate[]{certificate});

            ByteArrayOutputStream keystoreBytes = new ByteArrayOutputStream();
            keyStore.store(keystoreBytes, null);

            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                    KEYSTORE_ALIAS, new KeyStore.PasswordProtection(null));
            KSPrivateKeyEntry dssEntry = new KSPrivateKeyEntry(KEYSTORE_ALIAS, privateKeyEntry);
            SignatureAlgorithm signatureAlgorithm = selectSignatureAlgorithm(certificate, privateKey);

            try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(
                    keystoreBytes.toByteArray(), new KeyStore.PasswordProtection(null))) {
                return token.sign(toBeSigned, signatureAlgorithm, dssEntry);
            }
        } catch (GeneralSecurityException | IOException exception) {
            throw new KsefCryptoException(ERR_SIGN_FAILED, exception);
        }
    }

    private static SignatureAlgorithm selectSignatureAlgorithm(X509Certificate certificate, PrivateKey privateKey) {
        if (isRsaPair(certificate, privateKey)) {
            return SignatureAlgorithm.RSA_SHA256;
        }
        ECPublicKey ecPublicKey = (ECPublicKey) certificate.getPublicKey();
        int fieldSize = ecPublicKey.getParams().getCurve().getField().getFieldSize();
        if (fieldSize <= EC_FIELD_SIZE_256) {
            return SignatureAlgorithm.ECDSA_SHA256;
        }
        if (fieldSize <= EC_FIELD_SIZE_384) {
            return SignatureAlgorithm.ECDSA_SHA384;
        }
        return SignatureAlgorithm.ECDSA_SHA512;
    }

    private static DigestAlgorithm selectEcDigest(X509Certificate certificate) {
        ECPublicKey ecPublicKey = (ECPublicKey) certificate.getPublicKey();
        int fieldSize = ecPublicKey.getParams().getCurve().getField().getFieldSize();
        if (fieldSize <= EC_FIELD_SIZE_256) {
            return DigestAlgorithm.SHA256;
        }
        if (fieldSize <= EC_FIELD_SIZE_384) {
            return DigestAlgorithm.SHA384;
        }
        return DigestAlgorithm.SHA512;
    }

    private static boolean isRsaPair(X509Certificate cert, PrivateKey privateKey) {
        return cert.getPublicKey() instanceof RSAPublicKey && privateKey instanceof RSAPrivateKey;
    }

    private static boolean isEcdsaPair(X509Certificate cert, PrivateKey privateKey) {
        return cert.getPublicKey() instanceof ECPublicKey && privateKey instanceof ECPrivateKey;
    }
}
