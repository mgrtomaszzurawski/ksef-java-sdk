/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefCryptoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Utility for loading certificates and private keys from PKCS#12 keystores.
 */
public final class CertificateLoader {

    private static final String PKCS12_TYPE = "PKCS12";
    private static final String ERR_LOAD_KEYSTORE = "Failed to load PKCS#12 keystore";
    private static final String ERR_EXTRACT_KEY = "Failed to extract private key from keystore";
    private static final String ERR_EXTRACT_CERT = "Failed to extract certificate from keystore";
    private static final String ERR_EMPTY_KEYSTORE = "Keystore contains no aliases";

    private CertificateLoader() {
    }

    /**
     * Load a PKCS#12 keystore from a file path.
     */
    public static KeyStore loadKeyStore(Path keystorePath, char[] password) {
        try (InputStream inputStream = Files.newInputStream(keystorePath)) {
            return loadKeyStore(inputStream, password);
        } catch (IOException exception) {
            throw new KsefCryptoException(ERR_LOAD_KEYSTORE, exception);
        }
    }

    /**
     * Load a PKCS#12 keystore from an input stream.
     */
    public static KeyStore loadKeyStore(InputStream inputStream, char[] password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(PKCS12_TYPE);
            keyStore.load(inputStream, password);
            return keyStore;
        } catch (GeneralSecurityException | IOException exception) {
            throw new KsefCryptoException(ERR_LOAD_KEYSTORE, exception);
        }
    }

    /**
     * Extract the private key from a keystore using the given alias and password.
     */
    public static PrivateKey getPrivateKey(KeyStore keyStore, String alias, char[] password) {
        try {
            return (PrivateKey) keyStore.getKey(alias, password);
        } catch (GeneralSecurityException exception) {
            throw new KsefCryptoException(ERR_EXTRACT_KEY, exception);
        }
    }

    /**
     * Extract the X.509 certificate from a keystore using the given alias.
     */
    public static X509Certificate getCertificate(KeyStore keyStore, String alias) {
        try {
            return (X509Certificate) keyStore.getCertificate(alias);
        } catch (KeyStoreException exception) {
            throw new KsefCryptoException(ERR_EXTRACT_CERT, exception);
        }
    }

    /**
     * Get the first alias from a keystore.
     *
     * @throws KsefCryptoException if the keystore is empty or inaccessible
     */
    public static String getFirstAlias(KeyStore keyStore) {
        try {
            if (keyStore.size() == 0) {
                throw new KsefCryptoException(ERR_EMPTY_KEYSTORE,
                        new IllegalStateException(ERR_EMPTY_KEYSTORE));
            }
            return keyStore.aliases().nextElement();
        } catch (KeyStoreException exception) {
            throw new KsefCryptoException(ERR_EXTRACT_KEY, exception);
        }
    }
}
