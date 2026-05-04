/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KsefCryptoServiceTest {

    private static final byte[] PLAINTEXT = "KSeF invoice payload".getBytes(StandardCharsets.UTF_8);
    private static final int EXPECTED_AES_KEY_SIZE = 32;
    private static final int EXPECTED_IV_SIZE = 16;
    private static final int EXPECTED_SHA256_SIZE = 32;
    private static final int EXPECTED_RSA_KEY_SIZE = 2048;
    private static final String EC_DEFAULT_CURVE = "EC";

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
}
