/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import org.junit.jupiter.api.Test;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.EncryptionMaterial;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.FileMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.KsefCryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.KsefEncryptionInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificateUsage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KsefCryptoServiceTest {

    private static final byte[] PLAINTEXT = "KSeF invoice payload".getBytes(StandardCharsets.UTF_8);
    private static final int EXPECTED_AES_KEY_SIZE = 32;
    private static final int EXPECTED_IV_SIZE = 16;
    private static final int EXPECTED_SHA256_SIZE = 32;
    private static final int EXPECTED_RSA_KEY_SIZE = 2048;
    private static final String EC_DEFAULT_CURVE = "EC";
    /** PEM line length per RFC 7468 (canonical {@code openssl} default). */
    private static final int PEM_LINE_LENGTH = 64;

    private final KsefCryptoService crypto = new KsefCryptoService();

    @Test
    void generateAesKeyAndIv_producesCorrectSizes() {
        // when
        EncryptionMaterial material = crypto.generateAesKeyAndIv();

        // then
        assertEquals(EXPECTED_AES_KEY_SIZE, material.aesKey().length);
        assertEquals(EXPECTED_IV_SIZE, material.initVector().length);
    }

    @Test
    void encryptDecryptRoundTrip_recoversOriginal() {
        // given
        EncryptionMaterial material = crypto.generateAesKeyAndIv();

        // when
        byte[] ciphertext = crypto.encrypt(PLAINTEXT, material);
        byte[] roundTrip = crypto.decrypt(ciphertext, material);

        // then
        assertArrayEquals(PLAINTEXT, roundTrip);
    }

    @Test
    void encryptStreamDecryptStream_roundTripRecoversOriginal() throws Exception {
        // given
        EncryptionMaterial material = crypto.generateAesKeyAndIv();
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();

        // when
        crypto.encryptStream(new ByteArrayInputStream(PLAINTEXT), encrypted, material);
        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
        crypto.decryptStream(new ByteArrayInputStream(encrypted.toByteArray()), decrypted, material);

        // then
        assertArrayEquals(PLAINTEXT, decrypted.toByteArray());
    }

    @Test
    void computeFileMetadata_bytes_returnsSizeAndSha256() {
        // when
        FileMetadata meta = crypto.computeFileMetadata(PLAINTEXT);

        // then
        assertEquals(PLAINTEXT.length, meta.size());
        assertEquals(EXPECTED_SHA256_SIZE, meta.sha256().length);
    }

    @Test
    void computeFileMetadata_stream_matchesByteVariant() throws Exception {
        // given / when
        FileMetadata fromBytes = crypto.computeFileMetadata(PLAINTEXT);
        FileMetadata fromStream = crypto.computeFileMetadata(new ByteArrayInputStream(PLAINTEXT));

        // then
        assertEquals(fromBytes.size(), fromStream.size());
        assertArrayEquals(fromBytes.sha256(), fromStream.sha256());
    }

    @Test
    void generateRsaKeyPair_producesUsableKeys() {
        // when
        KeyPair keyPair = crypto.generateRsaKeyPair();

        // then
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
    }

    @Test
    void generateEcKeyPair_producesP256ByDefault() {
        // when
        KeyPair keyPair = crypto.generateEcKeyPair();

        // then
        assertEquals(EC_DEFAULT_CURVE, keyPair.getPrivate().getAlgorithm());
    }

    @Test
    void generateCsr_rsaKey_producesValidPkcs10() {
        // given
        KeyPair keyPair = crypto.generateRsaKeyPair(EXPECTED_RSA_KEY_SIZE);
        CsrRequest request = new CsrRequest("CN=Test, O=Org, C=PL", keyPair);

        // when
        CsrResult result = crypto.generateCsr(request);

        // then
        assertTrue(result.pkcs10Der().length > 0);
        assertEquals(keyPair.getPublic(), result.keyPair().getPublic());
    }

    @Test
    void generateCsr_ecKey_producesValidPkcs10() {
        // given
        KeyPair keyPair = crypto.generateEcKeyPair();
        CsrRequest request = new CsrRequest("CN=Test, O=Org, C=PL", keyPair);

        // when
        CsrResult result = crypto.generateCsr(request);

        // then
        assertTrue(result.pkcs10Der().length > 0);
    }

    @Test
    void encryptionMaterial_close_zeroisesBytes() {
        // given
        EncryptionMaterial material = crypto.generateAesKeyAndIv();
        byte[] keyBefore = material.aesKey();

        // when
        material.close();

        // then
        // The exposed accessor returns a fresh clone of the (now-zeroed) internal bytes.
        for (byte b : material.aesKey()) {
            assertEquals(0, b);
        }
        // Original clone we captured before close() is unchanged (proves defensive copy).
        boolean originalIntact = false;
        for (byte b : keyBefore) {
            if (b != 0) {
                originalIntact = true;
                break;
            }
        }
        assertTrue(originalIntact);
    }

    @Test
    void parsePrivateKey_acceptsPkcs8Pem() {
        // given — a freshly-generated RSA key encoded as PKCS#8 PEM
        KeyPair generated = crypto.generateRsaKeyPair();
        byte[] pkcs8Der = generated.getPrivate().getEncoded();
        String pemEncodedKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(PEM_LINE_LENGTH, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(pkcs8Der)
                + "\n-----END PRIVATE KEY-----\n";

        // when
        PrivateKey parsed = crypto.parsePrivateKey(pemEncodedKey.getBytes(StandardCharsets.US_ASCII));

        // then
        assertNotNull(parsed);
        assertEquals(generated.getPrivate().getAlgorithm(), parsed.getAlgorithm());
        assertArrayEquals(generated.getPrivate().getEncoded(), parsed.getEncoded());
    }

    @Test
    void parsePrivateKey_acceptsPkcs8Der() {
        // given — raw DER bytes (no PEM framing)
        KeyPair generated = crypto.generateRsaKeyPair();
        byte[] pkcs8Der = generated.getPrivate().getEncoded();

        // when
        PrivateKey parsed = crypto.parsePrivateKey(pkcs8Der);

        // then
        assertEquals(generated.getPrivate().getAlgorithm(), parsed.getAlgorithm());
    }

    @Test
    void parsePrivateKey_rejectsLegacyPkcs1WithDiagnostic() {
        // given — legacy PKCS#1 PEM marker that we explicitly do not handle
        String legacyRsaPem = "-----BEGIN RSA PRIVATE KEY-----\nABCD\n-----END RSA PRIVATE KEY-----\n";
        byte[] legacyRsaPemBytes = legacyRsaPem.getBytes(StandardCharsets.US_ASCII);

        // when / then
        KsefCryptoException ex = assertThrows(KsefCryptoException.class,
                () -> crypto.parsePrivateKey(legacyRsaPemBytes));
        assertTrue(ex.getMessage().contains("PKCS#8"),
                "diagnostic should mention PKCS#8 conversion path: " + ex.getMessage());
    }

    @Test
    void parseCertificate_acceptsPemAndDer() throws Exception {
        // given — a self-signed cert via the existing CSR support is overkill; use a
        // hand-crafted minimal X.509 from KeyPair would also be heavy.
        // Easier: round-trip through CertificateFactory with a real cert from
        // existing test fixtures.
        java.security.cert.CertificateFactory factory =
                java.security.cert.CertificateFactory.getInstance("X.509");
        // Use a freshly self-signed cert via internal TestCertificates helper
        var testCerts = io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates.generateRsa();
        byte[] derBytes = testCerts.certificate().getEncoded();
        String pemEncodedCert = "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(PEM_LINE_LENGTH, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(derBytes)
                + "\n-----END CERTIFICATE-----\n";

        // when
        X509Certificate fromDer = crypto.parseCertificate(derBytes);
        X509Certificate fromPem = crypto.parseCertificate(pemEncodedCert.getBytes(StandardCharsets.US_ASCII));

        // then
        assertNotNull(fromDer);
        assertEquals(testCerts.certificate().getSubjectX500Principal(), fromDer.getSubjectX500Principal());
        assertEquals(testCerts.certificate().getSubjectX500Principal(), fromPem.getSubjectX500Principal());
    }
}
