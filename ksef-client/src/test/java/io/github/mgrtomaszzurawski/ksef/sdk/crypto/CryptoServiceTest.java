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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoServiceTest {

    private static final int AES_KEY_LENGTH = 32;
    private static final int AES_IV_LENGTH = 16;
    private static final String TEST_PLAINTEXT = "Hello KSeF invoice content";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String EC_ALGORITHM = "EC";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String KSEF_TOKEN = "test-ksef-token-12345";

    @Test
    void generateAesKey_returns32Bytes() {
        // when
        byte[] keyBytes = CryptoService.generateAesKey();

        // then
        assertNotNull(keyBytes);
        assertEquals(AES_KEY_LENGTH, keyBytes.length);
    }

    @Test
    void generateAesKey_returnsDifferentKeysEachCall() {
        // when
        byte[] firstKey = CryptoService.generateAesKey();
        byte[] secondKey = CryptoService.generateAesKey();

        // then
        assertNotEquals(java.util.HexFormat.of().formatHex(firstKey),
                java.util.HexFormat.of().formatHex(secondKey));
    }

    @Test
    void generateIv_returns16Bytes() {
        // when
        byte[] ivBytes = CryptoService.generateIv();

        // then
        assertNotNull(ivBytes);
        assertEquals(AES_IV_LENGTH, ivBytes.length);
    }

    @Test
    void encryptAes_decryptAes_roundTrip() {
        // given
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] plaintext = TEST_PLAINTEXT.getBytes();

        // when
        byte[] encrypted = CryptoService.encryptAes(plaintext, aesKey, initVector);
        byte[] decrypted = CryptoService.decryptAes(encrypted, aesKey, initVector);

        // then
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void encryptRsa_producesOutput() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        byte[] plaintext = TEST_PLAINTEXT.getBytes();

        // when
        byte[] encrypted = CryptoService.encryptRsa(plaintext, keyPair.getPublic());

        // then
        assertNotNull(encrypted);
        assertNotEquals(0, encrypted.length);
    }

    @Test
    void encryptEcdh_producesOutput() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM);
        keyPairGen.initialize(256);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        byte[] plaintext = TEST_PLAINTEXT.getBytes();

        // when
        byte[] encrypted = CryptoService.encryptEcdh(plaintext, (ECPublicKey) keyPair.getPublic());

        // then
        assertNotNull(encrypted);
        assertNotEquals(0, encrypted.length);
    }

    @Test
    void encryptWithPublicKey_rsa_works() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // when
        byte[] encrypted = CryptoService.encryptWithPublicKey(TEST_PLAINTEXT.getBytes(), keyPair.getPublic());

        // then
        assertNotNull(encrypted);
    }

    @Test
    void encryptWithPublicKey_ec_works() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM);
        keyPairGen.initialize(256);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // when
        byte[] encrypted = CryptoService.encryptWithPublicKey(TEST_PLAINTEXT.getBytes(), keyPair.getPublic());

        // then
        assertNotNull(encrypted);
    }

    @Test
    void encryptKsefToken_producesOutput() throws Exception {
        // given
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        Instant timestamp = Instant.now();

        // when
        byte[] encrypted = CryptoService.encryptKsefToken(KSEF_TOKEN, timestamp, keyPair.getPublic());

        // then
        assertNotNull(encrypted);
        assertNotEquals(0, encrypted.length);
    }

    @Test
    void sha256Base64_producesConsistentHash() {
        // given
        byte[] data = TEST_PLAINTEXT.getBytes();

        // when
        String hash1 = CryptoService.sha256Base64(data);
        String hash2 = CryptoService.sha256Base64(data);

        // then
        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    void sha256Base64_differentInputProducesDifferentHash() {
        // when
        String hash1 = CryptoService.sha256Base64("input one".getBytes());
        String hash2 = CryptoService.sha256Base64("input two".getBytes());

        // then
        assertNotEquals(hash1, hash2);
    }

    @Test
    void parsePublicKeyFromPem_whenInvalidPem_throwsCryptoException() {
        assertThrows(KsefCryptoException.class,
                () -> CryptoService.parsePublicKeyFromPem("not-a-valid-certificate"));
    }
}
