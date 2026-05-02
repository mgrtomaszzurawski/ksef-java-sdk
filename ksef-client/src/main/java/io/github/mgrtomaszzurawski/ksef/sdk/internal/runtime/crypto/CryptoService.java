/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptographic operations for KSeF API integration.
 * Handles AES encryption of invoices and RSA/ECDH key exchange.
 */
public final class CryptoService {

    private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    private static final String RSA_OAEP = "RSA/ECB/OAEPPadding";
    private static final String AES_ALGORITHM = "AES";
    private static final String EC_ALGORITHM = "EC";
    private static final String ECDH_ALGORITHM = "ECDH";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String X509_TYPE = "X.509";
    private static final String SECP256R1_CURVE = "secp256r1";
    private static final String CERT_PEM_HEADER = "-----BEGIN CERTIFICATE-----\n";
    private static final String CERT_PEM_FOOTER = "\n-----END CERTIFICATE-----";
    private static final String TOKEN_SEPARATOR = "|";
    private static final String ERR_ENCRYPT_RSA = "RSA-OAEP encryption failed";
    private static final String ERR_ENCRYPT_ECDH = "ECDH encryption failed";
    private static final String ERR_ENCRYPT_AES = "AES encryption failed";
    private static final String ERR_DECRYPT_AES = "AES decryption failed";
    private static final String ERR_PARSE_CERT = "Failed to parse X.509 certificate";
    private static final String ERR_HASH = "SHA-256 hash failed";
    private static final String ERR_UNSUPPORTED_ALGORITHM = "Unsupported public key algorithm: ";
    private static final String ERR_INVALID_KEY_LENGTH = "AES key must be 32 bytes, got: ";
    private static final String ERR_INVALID_IV_LENGTH = "IV must be 16 bytes, got: ";
    private static final String RSA_ALGORITHM_NAME = "RSA";
    private static final String EC_ALGORITHM_NAME = "EC";
    private static final String MGF1_FUNCTION = "MGF1";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int AES_KEY_BYTES = 32;
    private static final int AES_IV_BYTES = 16;
    private static final int ECDH_SHARED_SECRET_SIZE = 32;

    /** SecureRandom is thread-safe; a single shared instance is intentional. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoService() {
    }

    /**
     * Generate a random 256-bit AES key.
     */
    public static byte[] generateAesKey() {
        byte[] keyBytes = new byte[AES_KEY_BYTES];
        SECURE_RANDOM.nextBytes(keyBytes);
        return keyBytes;
    }

    /**
     * Generate a random 16-byte IV for AES-CBC.
     */
    public static byte[] generateIv() {
        byte[] ivBytes = new byte[AES_IV_BYTES];
        SECURE_RANDOM.nextBytes(ivBytes);
        return ivBytes;
    }

    /**
     * Encrypt data with RSA-OAEP using a public key.
     * Used for wrapping AES keys and encrypting KSeF tokens.
     */
    public static byte[] encryptRsa(byte[] plaintext, PublicKey publicKey) {
        try {
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    SHA_256_ALGORITHM, MGF1_FUNCTION, MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            Cipher cipher = Cipher.getInstance(RSA_OAEP);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException exception) {
            throw new KsefCryptoException(ERR_ENCRYPT_RSA, exception);
        }
    }

    /**
     * Encrypt data using ECDH key agreement + AES-GCM.
     * Output format: ephemeral public key || nonce || ciphertext+tag.
     *
     * Note: Raw ECDH shared secret is used directly as AES key without KDF.
     * This matches the official CIRFMF SDK behavior and is required for
     * compatibility with the KSeF server.
     */
    public static byte[] encryptEcdh(byte[] plaintext, ECPublicKey recipientPublicKey) {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM);
            keyPairGen.initialize(new ECGenParameterSpec(SECP256R1_CURVE));
            KeyPair ephemeralKeyPair = keyPairGen.generateKeyPair();

            KeyAgreement keyAgreement = KeyAgreement.getInstance(ECDH_ALGORITHM);
            keyAgreement.init(ephemeralKeyPair.getPrivate());
            keyAgreement.doPhase(recipientPublicKey, true);
            byte[] sharedSecret = keyAgreement.generateSecret();

            SecretKey aesKey = new SecretKeySpec(sharedSecret, 0, ECDH_SHARED_SECRET_SIZE, AES_ALGORITHM);

            byte[] nonce = new byte[GCM_NONCE_BYTES];
            SECURE_RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ciphertextWithTag = cipher.doFinal(plaintext);

            byte[] ephemeralPubEncoded = ephemeralKeyPair.getPublic().getEncoded();

            ByteBuffer buffer = ByteBuffer.allocate(
                    ephemeralPubEncoded.length + nonce.length + ciphertextWithTag.length);
            buffer.put(ephemeralPubEncoded);
            buffer.put(nonce);
            buffer.put(ciphertextWithTag);
            return buffer.array();
        } catch (GeneralSecurityException exception) {
            throw new KsefCryptoException(ERR_ENCRYPT_ECDH, exception);
        }
    }

    /**
     * Encrypt content using the appropriate algorithm based on the public key type.
     */
    public static byte[] encryptWithPublicKey(byte[] plaintext, PublicKey publicKey) {
        String algorithm = publicKey.getAlgorithm();
        if (RSA_ALGORITHM_NAME.equals(algorithm)) {
            return encryptRsa(plaintext, publicKey);
        }
        if (EC_ALGORITHM_NAME.equals(algorithm)) {
            return encryptEcdh(plaintext, (ECPublicKey) publicKey);
        }
        throw new KsefCryptoException(ERR_UNSUPPORTED_ALGORITHM + algorithm,
                new IllegalArgumentException(ERR_UNSUPPORTED_ALGORITHM + algorithm));
    }

    /**
     * Encrypt a KSeF token with timestamp for authentication.
     * Format: "token|timestampMillis" encrypted with public key.
     */
    public static byte[] encryptKsefToken(String ksefToken, Instant challengeTimestamp, PublicKey publicKey) {
        byte[] tokenBytes = (ksefToken + TOKEN_SEPARATOR + challengeTimestamp.toEpochMilli())
                .getBytes(StandardCharsets.UTF_8);
        return encryptWithPublicKey(tokenBytes, publicKey);
    }

    /**
     * Encrypt content with AES-256-CBC.
     *
     * @param plaintext the data to encrypt
     * @param aesKey must be exactly 32 bytes (AES-256)
     * @param initVector must be exactly 16 bytes
     */
    @SuppressWarnings("java:S5542") // KSeF protocol mandates AES-256-CBC + PKCS#7 padding (see ADR-011); GCM/CCM are not supported by the server.
    public static byte[] encryptAes(byte[] plaintext, byte[] aesKey, byte[] initVector) {
        validateKeyAndIv(aesKey, initVector);
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(aesKey, AES_ALGORITHM),
                    new IvParameterSpec(initVector));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException exception) {
            throw new KsefCryptoException(ERR_ENCRYPT_AES, exception);
        }
    }

    /**
     * Create a configured AES-256-CBC Cipher in encrypt mode for streaming
     * usage (e.g. with {@link javax.crypto.CipherOutputStream}).
     *
     * <p>Use this for large payloads to avoid loading the entire content into memory.
     * The returned cipher is single-use — wrap it in a {@code CipherOutputStream},
     * write all input bytes, then close the stream to flush the final block.
     *
     * @param aesKey must be exactly 32 bytes (AES-256)
     * @param initVector must be exactly 16 bytes
     * @return initialized Cipher in encrypt mode
     */
    @SuppressWarnings("java:S5542") // KSeF protocol mandates AES-256-CBC + PKCS#7 padding (see ADR-011).
    public static Cipher newAesEncryptCipher(byte[] aesKey, byte[] initVector) {
        validateKeyAndIv(aesKey, initVector);
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(aesKey, AES_ALGORITHM),
                    new IvParameterSpec(initVector));
            return cipher;
        } catch (GeneralSecurityException exception) {
            throw new KsefCryptoException(ERR_ENCRYPT_AES, exception);
        }
    }

    /**
     * Decrypt content with AES-256-CBC.
     *
     * @param ciphertext the data to decrypt
     * @param aesKey must be exactly 32 bytes (AES-256)
     * @param initVector must be exactly 16 bytes
     */
    @SuppressWarnings("java:S5542") // KSeF protocol mandates AES-256-CBC + PKCS#7 padding (see ADR-011).
    public static byte[] decryptAes(byte[] ciphertext, byte[] aesKey, byte[] initVector) {
        validateKeyAndIv(aesKey, initVector);
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKey, AES_ALGORITHM),
                    new IvParameterSpec(initVector));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new KsefCryptoException(ERR_DECRYPT_AES, exception);
        }
    }

    /**
     * Parse an X.509 public key from a PEM-encoded certificate string.
     * The input should be the Base64 content without PEM headers.
     */
    public static PublicKey parsePublicKeyFromPem(String certificateBase64) {
        String fullPem = CERT_PEM_HEADER + certificateBase64 + CERT_PEM_FOOTER;
        try (ByteArrayInputStream input = new ByteArrayInputStream(fullPem.getBytes(StandardCharsets.UTF_8))) {
            CertificateFactory factory = CertificateFactory.getInstance(X509_TYPE);
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(input);
            return certificate.getPublicKey();
        } catch (CertificateException | IOException exception) {
            throw new KsefCryptoException(ERR_PARSE_CERT, exception);
        }
    }

    /**
     * Compute SHA-256 hash of data, returned as Base64-encoded string.
     */
    public static String sha256Base64(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new KsefCryptoException(ERR_HASH, exception);
        }
    }

    private static void validateKeyAndIv(byte[] aesKey, byte[] initVector) {
        if (aesKey.length != AES_KEY_BYTES) {
            throw new IllegalArgumentException(ERR_INVALID_KEY_LENGTH + aesKey.length);
        }
        if (initVector.length != AES_IV_BYTES) {
            throw new IllegalArgumentException(ERR_INVALID_IV_LENGTH + initVector.length);
        }
    }
}
