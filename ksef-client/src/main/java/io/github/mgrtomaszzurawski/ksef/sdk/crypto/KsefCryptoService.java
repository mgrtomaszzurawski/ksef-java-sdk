/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CsrSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/**
 * Public KSeF cryptography facade.
 *
 * <p>Exposes the cryptographic primitives KSeF protocol consumers need
 * for offline workflows, certificate enrollment, custom signing, and
 * advanced batch / export use cases. The same primitives are used
 * internally by sessions, batch, auth, and export.
 *
 * <p>The default no-arg constructor is sufficient — no configuration is
 * required. The facade is stateless and thread-safe.
 *
 * <p>Spec citations: REQ-CRYPTO-001..004,
 * {@code ksef-docs/sesja-interaktywna.md} (AES-256 + IV sizes),
 * {@code ksef-docs/przeglad-kluczowych-zmian-ksef-api-2-0.md} (RSA-OAEP-SHA256),
 * {@code ksef-docs/certyfikaty-KSeF.md} (CSR generation for cert enrollment).
 *
 * @since 1.0.0
 */
public final class KsefCryptoService {

    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String RSA_KEY_ALGORITHM = "RSA";
    private static final String EC_KEY_ALGORITHM = "EC";
    private static final int RSA_DEFAULT_KEY_SIZE = 2048;
    /** KSeF spec mandates RSA key sizes &gt;= 2048 bits per {@code ksef-docs/auth/podpis-xades.md:48}. */
    private static final int RSA_MIN_KEY_SIZE = 2048;
    private static final String EC_DEFAULT_CURVE = "secp256r1";
    private static final int STREAM_BUFFER_BYTES = 8 * 1024;
    private static final String ERR_SHA256_UNAVAILABLE = "SHA-256 algorithm not available";
    private static final String ERR_KEY_GEN_UNAVAILABLE = "Key generation algorithm not available";
    private static final String ERR_RSA_KEY_SIZE_TOO_SMALL =
            "RSA key size must be >= " + RSA_MIN_KEY_SIZE + " bits per KSeF spec";
    private static final String ERR_MATERIAL_NULL = "material must not be null";
    private static final String ERR_IN_NULL = "in must not be null";
    private static final String ERR_BYTES_NULL = "bytes must not be null";
    private static final String ERR_PATH_NULL = "path must not be null";
    private static final String ERR_PEM_NO_BEGIN = "PEM input does not contain a recognised BEGIN marker";
    private static final String ERR_PEM_NO_END = "PEM input does not contain a matching END marker";
    private static final String ERR_PRIVATE_KEY_PARSE = "Failed to parse private key";
    private static final String ERR_CERTIFICATE_PARSE = "Failed to parse X.509 certificate";

    private static final String CERT_X509 = "X.509";
    private static final String PEM_BEGIN_PREFIX = "-----BEGIN ";
    private static final String PEM_END_PREFIX = "-----END ";
    private static final String PEM_FOOTER_SUFFIX = "-----";
    private static final String PEM_PRIVATE_KEY_LABEL = "PRIVATE KEY";
    private static final String PEM_RSA_PRIVATE_KEY_LABEL = "RSA PRIVATE KEY";
    private static final String PEM_EC_PRIVATE_KEY_LABEL = "EC PRIVATE KEY";
    private static final String PEM_ENCRYPTED_PRIVATE_KEY_LABEL = "ENCRYPTED PRIVATE KEY";

    /**
     * Generate a fresh AES-256 key + 16-byte IV pair via secure random.
     */
    public EncryptionMaterial generateAesKeyAndIv() {
        return new EncryptionMaterial(CryptoService.generateAesKey(), CryptoService.generateIv());
    }

    /**
     * Wrap the {@code material}'s AES key with the supplied KSeF public
     * key (RSA-OAEP-SHA256 / MGF1-SHA-256, or ECDH per key type) and pair
     * with the plaintext IV.
     */
    public KsefEncryptionInfo encryptKey(EncryptionMaterial material, PublicKey ksefPublicKey) {
        Objects.requireNonNull(material, ERR_MATERIAL_NULL);
        Objects.requireNonNull(ksefPublicKey, "ksefPublicKey must not be null");
        byte[] wrapped = CryptoService.encryptWithPublicKey(material.aesKey(), ksefPublicKey);
        return new KsefEncryptionInfo(wrapped, material.initVector());
    }

    /**
     * Encrypt {@code plaintext} with AES-256-CBC + PKCS#7 padding using
     * the supplied {@link EncryptionMaterial}.
     */
    public byte[] encrypt(byte[] plaintext, EncryptionMaterial material) {
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        Objects.requireNonNull(material, ERR_MATERIAL_NULL);
        return CryptoService.encryptAes(plaintext, material.aesKey(), material.initVector());
    }

    /**
     * Decrypt {@code ciphertext} with AES-256-CBC + PKCS#7 padding using
     * the supplied {@link EncryptionMaterial}.
     */
    public byte[] decrypt(byte[] ciphertext, EncryptionMaterial material) {
        Objects.requireNonNull(ciphertext, "ciphertext must not be null");
        Objects.requireNonNull(material, ERR_MATERIAL_NULL);
        return CryptoService.decryptAes(ciphertext, material.aesKey(), material.initVector());
    }

    /**
     * Encrypt the entire {@code in} stream into {@code out} using
     * AES-256-CBC + PKCS#7. Closes neither stream.
     */
    public void encryptStream(InputStream in, OutputStream out, EncryptionMaterial material) throws IOException {
        Objects.requireNonNull(in, ERR_IN_NULL);
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(material, ERR_MATERIAL_NULL);
        Cipher cipher = CryptoService.newAesEncryptCipher(material.aesKey(), material.initVector());
        try (CipherOutputStream cipherOut = new CipherOutputStream(new NonClosingOutputStream(out), cipher)) {
            transfer(in, cipherOut);
        }
    }

    /**
     * Decrypt the entire {@code in} stream into {@code out} using
     * AES-256-CBC + PKCS#7. Closes neither stream.
     */
    public void decryptStream(InputStream in, OutputStream out, EncryptionMaterial material) throws IOException {
        Objects.requireNonNull(in, ERR_IN_NULL);
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(material, ERR_MATERIAL_NULL);
        Cipher cipher = CryptoService.newAesDecryptCipher(material.aesKey(), material.initVector());
        // CipherInputStream.close() would close the wrapped caller-provided
        // input stream. Wrapping with NonClosingInputStream preserves the
        // documented contract ("Closes neither stream") for both directions.
        try (CipherInputStream cipherIn = new CipherInputStream(new NonClosingInputStream(in), cipher)) {
            transfer(cipherIn, out);
        }
    }

    /**
     * Compute size + SHA-256 of the supplied bytes.
     */
    public FileMetadata computeFileMetadata(byte[] content) {
        Objects.requireNonNull(content, "content must not be null");
        try {
            byte[] hash = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(content);
            return new FileMetadata(content.length, hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new KsefCryptoException(ERR_SHA256_UNAVAILABLE, ex);
        }
    }

    /**
     * Compute size + SHA-256 of the supplied stream. Reads the entire
     * stream; does not close it.
     */
    public FileMetadata computeFileMetadata(InputStream in) throws IOException {
        Objects.requireNonNull(in, ERR_IN_NULL);
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] buffer = new byte[STREAM_BUFFER_BYTES];
            long size = 0;
            int read = in.read(buffer);
            while (read >= 0) {
                digest.update(buffer, 0, read);
                size += read;
                read = in.read(buffer);
            }
            return new FileMetadata(size, digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new KsefCryptoException(ERR_SHA256_UNAVAILABLE, ex);
        }
    }

    /**
     * Generate a new RSA key pair (default 2048-bit) for KSeF
     * certificate enrollment.
     */
    public KeyPair generateRsaKeyPair() {
        return generateRsaKeyPair(RSA_DEFAULT_KEY_SIZE);
    }

    /**
     * Generate a new RSA key pair of the requested size (>= 2048 bits per
     * KSeF spec {@code ksef-docs/auth/podpis-xades.md:48}).
     */
    public KeyPair generateRsaKeyPair(int keySize) {
        if (keySize < RSA_MIN_KEY_SIZE) {
            throw new IllegalArgumentException(ERR_RSA_KEY_SIZE_TOO_SMALL + ", got " + keySize);
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_KEY_ALGORITHM);
            generator.initialize(keySize);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new KsefCryptoException(ERR_KEY_GEN_UNAVAILABLE, ex);
        }
    }

    /**
     * Generate a new EC key pair (default {@code secp256r1} / P-256) for
     * KSeF certificate enrollment.
     */
    public KeyPair generateEcKeyPair() {
        return generateEcKeyPair(EC_DEFAULT_CURVE);
    }

    /**
     * Generate a new EC key pair on the named curve.
     */
    public KeyPair generateEcKeyPair(String curveName) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(EC_KEY_ALGORITHM);
            generator.initialize(new ECGenParameterSpec(curveName));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new KsefCryptoException(ERR_KEY_GEN_UNAVAILABLE, ex);
        }
    }

    /**
     * Generate a CSR (PKCS#10) for KSeF certificate enrollment.
     *
     * <p>The CSR is signed with the supplied private key. The subject DN
     * is built from {@code request}; the algorithm
     * ({@code SHA256withRSA} or {@code SHA256withECDSA}) is auto-detected
     * from the key type.
     *
     * <p>Spec citation: {@code ksef-docs/certyfikaty-KSeF.md} certificate
     * enrollment workflow.
     */
    public CsrResult generateCsr(CsrRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return CsrSupport.generate(request);
    }

    /**
     * Parse a PKCS#8 private key from PEM or DER bytes. Codex 2026-05-05
     * F5 — public helper for offline-certificate / KOD II workflows where
     * the consumer holds a private key on disk and needs it as a
     * {@link PrivateKey} for signing.
     *
     * <p>Accepts:
     * <ul>
     *   <li>DER bytes (raw PKCS#8 binary)</li>
     *   <li>PEM with {@code -----BEGIN PRIVATE KEY-----} (PKCS#8 text)</li>
     * </ul>
     *
     * <p>Does not accept legacy PKCS#1
     * ({@code -----BEGIN RSA PRIVATE KEY-----} or
     * {@code -----BEGIN EC PRIVATE KEY-----}) or encrypted
     * ({@code -----BEGIN ENCRYPTED PRIVATE KEY-----}) keys; convert
     * those to PKCS#8 first (e.g.
     * {@code openssl pkcs8 -topk8 -nocrypt -in legacy.pem -out pkcs8.pem})
     * — pure-JCA decoding of legacy formats requires BouncyCastle which
     * the SDK does not bundle.
     *
     * <p>The returned key's algorithm is detected from the encoded
     * structure ({@code RSA} or {@code EC}). Throws
     * {@link KsefCryptoException} on malformed input or unsupported
     * algorithm.
     */
    public PrivateKey parsePrivateKey(byte[] bytes) {
        Objects.requireNonNull(bytes, ERR_BYTES_NULL);
        byte[] der = looksLikePem(bytes) ? decodePem(bytes, PEM_PRIVATE_KEY_LABEL) : bytes;
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            try {
                return KeyFactory.getInstance(RSA_KEY_ALGORITHM).generatePrivate(spec);
            } catch (InvalidKeySpecException notRsa) {
                return KeyFactory.getInstance(EC_KEY_ALGORITHM).generatePrivate(spec);
            }
        } catch (GeneralSecurityException ex) {
            throw new KsefCryptoException(ERR_PRIVATE_KEY_PARSE, ex);
        }
    }

    /**
     * Convenience overload — read PEM/DER private key bytes from a file
     * path and parse via {@link #parsePrivateKey(byte[])}.
     */
    public PrivateKey parsePrivateKey(Path path) {
        Objects.requireNonNull(path, ERR_PATH_NULL);
        try {
            return parsePrivateKey(Files.readAllBytes(path));
        } catch (IOException ex) {
            throw new KsefCryptoException(ERR_PRIVATE_KEY_PARSE, ex);
        }
    }

    /**
     * Parse an X.509 certificate from PEM or DER bytes. Codex 2026-05-05
     * F5 — public helper for offline-certificate / KOD II workflows.
     *
     * <p>Accepts:
     * <ul>
     *   <li>DER bytes (raw X.509 binary)</li>
     *   <li>PEM with {@code -----BEGIN CERTIFICATE-----}</li>
     * </ul>
     */
    public X509Certificate parseCertificate(byte[] bytes) {
        Objects.requireNonNull(bytes, ERR_BYTES_NULL);
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERT_X509);
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException ex) {
            throw new KsefCryptoException(ERR_CERTIFICATE_PARSE, ex);
        }
    }

    /**
     * Convenience overload — read PEM/DER certificate bytes from a file
     * path and parse via {@link #parseCertificate(byte[])}.
     */
    public X509Certificate parseCertificate(Path path) {
        Objects.requireNonNull(path, ERR_PATH_NULL);
        try {
            return parseCertificate(Files.readAllBytes(path));
        } catch (IOException ex) {
            throw new KsefCryptoException(ERR_CERTIFICATE_PARSE, ex);
        }
    }

    private static boolean looksLikePem(byte[] bytes) {
        if (bytes.length < PEM_BEGIN_PREFIX.length()) {
            return false;
        }
        String head = new String(bytes, 0, Math.min(bytes.length, 64), StandardCharsets.US_ASCII);
        return head.contains(PEM_BEGIN_PREFIX);
    }

    private static byte[] decodePem(byte[] pem, String requiredLabel) {
        String text = new String(pem, StandardCharsets.US_ASCII);
        String beginMarker = PEM_BEGIN_PREFIX + requiredLabel + PEM_FOOTER_SUFFIX;
        String endMarker = PEM_END_PREFIX + requiredLabel + PEM_FOOTER_SUFFIX;
        int beginIdx = text.indexOf(beginMarker);
        if (beginIdx < 0) {
            throw new KsefCryptoException(ERR_PEM_NO_BEGIN
                    + " (expected " + beginMarker + "; got "
                    + (text.contains(PEM_BEGIN_PREFIX) ? unsupportedPemKindHint(text) : "no PEM markers") + ")", null);
        }
        int afterBegin = beginIdx + beginMarker.length();
        int endIdx = text.indexOf(endMarker, afterBegin);
        if (endIdx < 0) {
            throw new KsefCryptoException(ERR_PEM_NO_END + " for " + requiredLabel, null);
        }
        String base64 = text.substring(afterBegin, endIdx).replaceAll("\\s+", "");
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new KsefCryptoException(ERR_PRIVATE_KEY_PARSE, ex);
        }
    }

    private static String unsupportedPemKindHint(String text) {
        if (text.contains(PEM_BEGIN_PREFIX + PEM_RSA_PRIVATE_KEY_LABEL)) {
            return "legacy PKCS#1 (BEGIN RSA PRIVATE KEY) — convert to PKCS#8";
        }
        if (text.contains(PEM_BEGIN_PREFIX + PEM_EC_PRIVATE_KEY_LABEL)) {
            return "legacy SEC1 (BEGIN EC PRIVATE KEY) — convert to PKCS#8";
        }
        if (text.contains(PEM_BEGIN_PREFIX + PEM_ENCRYPTED_PRIVATE_KEY_LABEL)) {
            return "encrypted PKCS#8 (BEGIN ENCRYPTED PRIVATE KEY) — decrypt first";
        }
        return "unrecognised PEM kind";
    }

    private static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[STREAM_BUFFER_BYTES];
        int read = in.read(buffer);
        while (read >= 0) {
            out.write(buffer, 0, read);
            read = in.read(buffer);
        }
    }

    /**
     * Output stream that ignores {@code close()} so callers can supply
     * their own stream and have it survive a {@code CipherOutputStream}
     * close.
     */
    private static final class NonClosingOutputStream extends OutputStream {
        private final OutputStream delegate;

        NonClosingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() {
            // intentionally do not close the delegate
        }
    }

    /**
     * Input stream that ignores {@code close()} so callers can supply
     * their own stream and have it survive a {@code CipherInputStream}
     * close. Mirror of {@link NonClosingOutputStream} on the input side.
     */
    private static final class NonClosingInputStream extends InputStream {
        private final InputStream delegate;

        NonClosingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() {
            // intentionally do not close the delegate
        }
    }
}
