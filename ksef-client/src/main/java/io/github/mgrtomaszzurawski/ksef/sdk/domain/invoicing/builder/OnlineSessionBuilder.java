/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FormCodeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSessionOpenRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.security.PublicKey;
import java.util.Objects;

/**
 * Builder for opening an online session.
 * <p>Required: form code, KSeF public key for encryption.
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

    public static OnlineSessionBuilder fa2(PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(SYSTEM_CODE_FA, SCHEMA_VERSION_2, FORM_CODE_FA2, ksefPublicKey);
    }

    public static OnlineSessionBuilder fa3(PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(SYSTEM_CODE_FA, SCHEMA_VERSION_3, FORM_CODE_FA3, ksefPublicKey);
    }

    public static OnlineSessionBuilder custom(String systemCode, String schemaVersion, String formCodeValue, PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(systemCode, schemaVersion, formCodeValue, ksefPublicKey);
    }

    public OnlineSessionBuilder toBuilder() {
        return new OnlineSessionBuilder(this.systemCode, this.schemaVersion, this.formCodeValue, this.ksefPublicKey);
    }

    public OnlineSessionOpenRequest build() {
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, ksefPublicKey);
        return new OnlineSessionOpenRequest(
                new FormCodeInfo(systemCode, schemaVersion, formCodeValue),
                encryptedKey, initVector, aesKey);
    }
}
