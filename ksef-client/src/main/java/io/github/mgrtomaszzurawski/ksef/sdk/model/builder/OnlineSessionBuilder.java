/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;

import java.security.PublicKey;
import java.util.Objects;

/**
 * Builder for opening an online session.
 * <p>
 * Required: form code, KSeF public key for encryption.
 * <p>
 * Usage:
 * <pre>{@code
 * var request = OnlineSessionBuilder.fa2(encryptionPublicKey).build();
 * var request = OnlineSessionBuilder.fa3(encryptionPublicKey).build();
 * }</pre>
 */
public final class OnlineSessionBuilder {

    private static final String SYSTEM_CODE_FA = "FA";
    private static final String SCHEMA_VERSION_2 = "2";
    private static final String SCHEMA_VERSION_3 = "3";
    private static final String FORM_CODE_FA2 = "FA (2)";
    private static final String FORM_CODE_FA3 = "FA (3)";
    private static final String ERR_NULL_KSEF_PUBLIC_KEY = "ksefPublicKey is required";

    private final String systemCode;
    private final String schemaVersion;
    private final String formCodeValue;
    private final PublicKey ksefPublicKey;

    private OnlineSessionBuilder(String systemCode, String schemaVersion, String formCodeValue, PublicKey ksefPublicKey) {
        this.systemCode = systemCode;
        this.schemaVersion = schemaVersion;
        this.formCodeValue = formCodeValue;
        this.ksefPublicKey = Objects.requireNonNull(ksefPublicKey, ERR_NULL_KSEF_PUBLIC_KEY);
    }

    /**
     * Create a builder for FA(2) invoice schema (most common).
     *
     * @param ksefPublicKey the KSeF public key (SymmetricKeyEncryption usage) from SecurityClient
     */
    public static OnlineSessionBuilder fa2(PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(SYSTEM_CODE_FA, SCHEMA_VERSION_2, FORM_CODE_FA2, ksefPublicKey);
    }

    /**
     * Create a builder for FA(3) invoice schema.
     *
     * @param ksefPublicKey the KSeF public key (SymmetricKeyEncryption usage) from SecurityClient
     */
    public static OnlineSessionBuilder fa3(PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(SYSTEM_CODE_FA, SCHEMA_VERSION_3, FORM_CODE_FA3, ksefPublicKey);
    }

    /**
     * Create a builder for a custom form code.
     *
     * @param systemCode e.g. "FA"
     * @param schemaVersion e.g. "2"
     * @param formCodeValue e.g. "FA (2)"
     * @param ksefPublicKey the KSeF public key
     */
    public static OnlineSessionBuilder custom(String systemCode, String schemaVersion, String formCodeValue, PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(systemCode, schemaVersion, formCodeValue, ksefPublicKey);
    }

    /**
     * Result of building a session request, containing the request and the
     * AES key/IV needed for encrypting invoices within the session.
     */
    public record SessionOpenResult(
            OpenOnlineSessionRequestRaw request,
            byte[] aesKey,
            byte[] initVector) {
    }

    /**
     * Build the session opening request. Generates AES key and encrypts it
     * with the KSeF public key automatically. Returns both the request and
     * the AES key/IV needed for encrypting invoices within this session.
     *
     * @return result containing the request and session encryption keys
     */
    public SessionOpenResult build() {
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, ksefPublicKey);

        OpenOnlineSessionRequestRaw request = new OpenOnlineSessionRequestRaw()
                .formCode(new FormCodeRaw()
                        .systemCode(systemCode)
                        .schemaVersion(schemaVersion)
                        .value(formCodeValue))
                .encryption(new EncryptionInfoRaw()
                        .encryptedSymmetricKey(encryptedKey)
                        .initializationVector(initVector));

        return new SessionOpenResult(request, aesKey, initVector);
    }
}
