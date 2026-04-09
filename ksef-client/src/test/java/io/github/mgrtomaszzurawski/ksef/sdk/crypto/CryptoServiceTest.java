/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServiceTest {

    private static final int AES_KEY_LENGTH = 32;
    private static final int AES_IV_LENGTH = 16;
    private static final int RSA_KEY_SIZE = 2048;
    private static final int EC_KEY_SIZE = 256;
    private static final String RSA_ALGORITHM = "RSA";
    private static final String EC_ALGORITHM = "EC";
    private static final String KSEF_TOKEN = "test-ksef-token-12345";
    private static final byte[] TEST_PLAINTEXT = "Hello KSeF invoice content".getBytes();

    @Test
    void generateAesKey_whenCalled_returns32ByteArray() {
        // when
        byte[] keyBytes = CryptoService.generateAesKey();

        // then
        assertNotNull(keyBytes);
        assertEquals(AES_KEY_LENGTH, keyBytes.length);
    }

    @Test
    void generateAesKey_whenCalledTwice_returnsDifferentKeys() {
        // when
        byte[] firstKey = CryptoService.generateAesKey();
        byte[] secondKey = CryptoService.generateAesKey();

        // then
        boolean different = false;
        for (int index = 0; index < firstKey.length; index++) {
            if (firstKey[index] != secondKey[index]) {
                different = true;
                break;
            }
        }
        assertTrue(different, "Two generated keys should differ");
    }

    @Test
    void generateIv_whenCalled_returns16ByteArray() {
        // when
        byte[] ivBytes = CryptoService.generateIv();

        // then
        assertNotNull(ivBytes);
        assertEquals(AES_IV_LENGTH, ivBytes.length);
    }

    @Test
    void encryptAes_whenDecryptedWithSameKey_returnsOriginalPlaintext() {
        // given
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();

        // when
        byte[] encrypted = CryptoService.encryptAes(TEST_PLAINTEXT, aesKey, initVector);
        byte[] decrypted = CryptoService.decryptAes(encrypted, aesKey, initVector);

        // then
        assertArrayEquals(TEST_PLAINTEXT, decrypted);
    }

    @Test
    void encryptAes_whenEncrypted_producesOutputDifferentFromInput() {
        // given
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();

        // when
        byte[] encrypted = CryptoService.encryptAes(TEST_PLAINTEXT, aesKey, initVector);

        // then
        assertTrue(encrypted.length > 0, "Encrypted output should not be empty");
        boolean different = encrypted.length != TEST_PLAINTEXT.length;
        if (!different) {
            for (int index = 0; index < encrypted.length; index++) {
                if (encrypted[index] != TEST_PLAINTEXT[index]) {
                    different = true;
                    break;
                }
            }
        }
        assertTrue(different, "Encrypted output should differ from plaintext");
    }

    @Test
    void encryptRsa_whenValidKey_producesOutputMatchingKeySize() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // when
        byte[] encrypted = CryptoService.encryptRsa(TEST_PLAINTEXT, keyPair.getPublic());

        // then
        int expectedBlockSize = RSA_KEY_SIZE / 8;
        assertEquals(expectedBlockSize, encrypted.length, "RSA output should match key block size");
    }

    @Test
    void encryptEcdh_whenValidKey_producesNonEmptyOutput() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM);
        keyPairGen.initialize(EC_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // when
        byte[] encrypted = CryptoService.encryptEcdh(TEST_PLAINTEXT, (ECPublicKey) keyPair.getPublic());

        // then
        assertTrue(encrypted.length > TEST_PLAINTEXT.length,
                "ECDH output should be larger than input (contains ephemeral key + nonce + ciphertext)");
    }

    @Test
    void encryptWithPublicKey_whenRsaKey_producesRsaBlockSizeOutput() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // when
        byte[] encrypted = CryptoService.encryptWithPublicKey(TEST_PLAINTEXT, keyPair.getPublic());

        // then
        int expectedBlockSize = RSA_KEY_SIZE / 8;
        assertEquals(expectedBlockSize, encrypted.length);
    }

    @Test
    void encryptWithPublicKey_whenEcKey_producesOutputLargerThanInput() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM);
        keyPairGen.initialize(EC_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // when
        byte[] encrypted = CryptoService.encryptWithPublicKey(TEST_PLAINTEXT, keyPair.getPublic());

        // then
        assertTrue(encrypted.length > TEST_PLAINTEXT.length,
                "EC encrypted output should be larger than input");
    }

    @Test
    void encryptKsefToken_whenValidInput_producesRsaBlockSizeOutput() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        Instant timestamp = Instant.now();

        // when
        byte[] encrypted = CryptoService.encryptKsefToken(KSEF_TOKEN, timestamp, keyPair.getPublic());

        // then
        int expectedBlockSize = RSA_KEY_SIZE / 8;
        assertEquals(expectedBlockSize, encrypted.length,
                "Token encryption should produce RSA block size output");
    }

    @Test
    void sha256Base64_whenSameInput_producesConsistentHash() {
        // when
        String hash1 = CryptoService.sha256Base64(TEST_PLAINTEXT);
        String hash2 = CryptoService.sha256Base64(TEST_PLAINTEXT);

        // then
        assertNotNull(hash1);
        assertEquals(hash1, hash2, "Same input should produce same hash");
    }

    @Test
    void sha256Base64_whenDifferentInput_producesDifferentHash() {
        // given
        byte[] input1 = "input one".getBytes();
        byte[] input2 = "input two".getBytes();

        // when
        String hash1 = CryptoService.sha256Base64(input1);
        String hash2 = CryptoService.sha256Base64(input2);

        // then
        assertTrue(!hash1.equals(hash2), "Different inputs should produce different hashes");
    }

    @Test
    void parsePublicKeyFromPem_whenInvalidPem_throwsCryptoException() {
        // then
        assertThrows(KsefCryptoException.class,
                () -> CryptoService.parsePublicKeyFromPem("not-a-valid-certificate"));
    }
}
