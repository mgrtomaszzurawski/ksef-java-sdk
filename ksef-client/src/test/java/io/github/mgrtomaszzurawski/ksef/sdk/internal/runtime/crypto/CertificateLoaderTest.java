/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CertificateLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CertificateLoaderTest {

    private static final String PKCS12_TYPE = "PKCS12";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int CERT_VALIDITY_DAYS = 365;
    private static final String CERT_SUBJECT = "CN=Test";
    private static final String SHA256_WITH_RSA = "SHA256WithRSA";
    private static final String KEYSTORE_ALIAS = "test-alias";
    private static final char[] KEYSTORE_PASSWORD = "test-password".toCharArray();
    private static final byte[] CORRUPTED_KEYSTORE_DATA = {1, 2, 3, 4, 5};
    private static final int EXPECTED_KEYSTORE_SIZE = 1;

    private static byte[] testKeystoreBytes;
    private static KeyPair testKeyPair;
    private static X509Certificate testCertificate;

    @BeforeAll
    static void createTestKeystore() throws Exception {
        testKeyPair = generateRsaKeyPair();
        testCertificate = generateSelfSignedCertificate(testKeyPair);

        KeyStore keyStore = KeyStore.getInstance(PKCS12_TYPE);
        keyStore.load(null, null);
        keyStore.setKeyEntry(KEYSTORE_ALIAS, testKeyPair.getPrivate(), KEYSTORE_PASSWORD,
                new Certificate[]{testCertificate});

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        keyStore.store(outputStream, KEYSTORE_PASSWORD);
        testKeystoreBytes = outputStream.toByteArray();
    }

    @Test
    void loadKeyStore_whenValidInputStream_returnsKeyStoreWithAlias() throws Exception {
        // given
        InputStream inputStream = new ByteArrayInputStream(testKeystoreBytes);

        // when
        KeyStore loaded = CertificateLoader.loadKeyStore(inputStream, KEYSTORE_PASSWORD);

        // then
        assertNotNull(loaded);
        assertEquals(EXPECTED_KEYSTORE_SIZE, loaded.size());
        assertEquals(KEYSTORE_ALIAS, loaded.aliases().nextElement());
    }

    @Test
    void loadKeyStore_whenValidFilePath_returnsKeyStoreWithAlias(@TempDir Path tempDir) throws Exception {
        // given
        Path keystorePath = tempDir.resolve("test.p12");
        Files.write(keystorePath, testKeystoreBytes);

        // when
        KeyStore loaded = CertificateLoader.loadKeyStore(keystorePath, KEYSTORE_PASSWORD);

        // then
        assertNotNull(loaded);
        assertEquals(EXPECTED_KEYSTORE_SIZE, loaded.size());
        assertEquals(KEYSTORE_ALIAS, loaded.aliases().nextElement());
    }

    @Test
    void loadKeyStore_whenWrongPassword_throwsCryptoException() {
        // given
        InputStream inputStream = new ByteArrayInputStream(testKeystoreBytes);
        char[] wrongPassword = "wrong-password".toCharArray();

        // then
        assertThrows(KsefCryptoException.class,
                () -> CertificateLoader.loadKeyStore(inputStream, wrongPassword));
    }

    @Test
    void loadKeyStore_whenFileNotFound_throwsCryptoException() {
        // given
        Path nonExistentPath = Path.of("/nonexistent/keystore.p12");

        // then
        assertThrows(KsefCryptoException.class,
                () -> CertificateLoader.loadKeyStore(nonExistentPath, KEYSTORE_PASSWORD));
    }

    @Test
    void loadKeyStore_whenCorruptedData_throwsCryptoException() {
        // given
        InputStream corruptStream = new ByteArrayInputStream(CORRUPTED_KEYSTORE_DATA);

        // then
        assertThrows(KsefCryptoException.class,
                () -> CertificateLoader.loadKeyStore(corruptStream, KEYSTORE_PASSWORD));
    }

    @Test
    void getPrivateKey_whenValidAlias_returnsRsaPrivateKey() throws Exception {
        // given
        KeyStore keyStore = loadTestKeyStore();

        // when
        PrivateKey privateKey = CertificateLoader.getPrivateKey(keyStore, KEYSTORE_ALIAS, KEYSTORE_PASSWORD);

        // then
        assertNotNull(privateKey);
        assertEquals(RSA_ALGORITHM, privateKey.getAlgorithm());
        assertEquals(testKeyPair.getPrivate(), privateKey);
    }

    @Test
    void getCertificate_whenValidAlias_returnsX509Certificate() throws Exception {
        // given
        KeyStore keyStore = loadTestKeyStore();

        // when
        X509Certificate certificate = CertificateLoader.getCertificate(keyStore, KEYSTORE_ALIAS);

        // then
        assertNotNull(certificate);
        assertInstanceOf(X509Certificate.class, certificate);
        assertEquals(testCertificate.getSubjectX500Principal(), certificate.getSubjectX500Principal());
    }

    @Test
    void getCertificate_whenUnknownAlias_returnsNull() throws Exception {
        // given
        KeyStore keyStore = loadTestKeyStore();

        // when
        X509Certificate certificate = CertificateLoader.getCertificate(keyStore, "nonexistent-alias");

        // then — KeyStore.getCertificate returns null for unknown alias
        assertNull(certificate);
    }

    @Test
    void getFirstAlias_whenKeyStoreHasEntries_returnsFirstAlias() throws Exception {
        // given
        KeyStore keyStore = loadTestKeyStore();

        // when
        String alias = CertificateLoader.getFirstAlias(keyStore);

        // then
        assertEquals(KEYSTORE_ALIAS, alias);
    }

    @Test
    void getFirstAlias_whenEmptyKeyStore_throwsCryptoException() throws Exception {
        // given
        KeyStore emptyKeyStore = KeyStore.getInstance(PKCS12_TYPE);
        emptyKeyStore.load(null, null);

        // when
        KsefCryptoException exception = assertThrows(KsefCryptoException.class,
                () -> CertificateLoader.getFirstAlias(emptyKeyStore));

        // then
        assertNotNull(exception.getMessage());
    }

    private static KeyStore loadTestKeyStore() {
        InputStream inputStream = new ByteArrayInputStream(testKeystoreBytes);
        return CertificateLoader.loadKeyStore(inputStream, KEYSTORE_PASSWORD);
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        return keyPairGen.generateKeyPair();
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        X500Name issuer = new X500Name(CERT_SUBJECT);
        Instant notBefore = Instant.now();
        Instant notAfter = notBefore.plus(CERT_VALIDITY_DAYS, ChronoUnit.DAYS);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, BigInteger.ONE,
                Date.from(notBefore), Date.from(notAfter),
                issuer, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(SHA256_WITH_RSA).build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
}
