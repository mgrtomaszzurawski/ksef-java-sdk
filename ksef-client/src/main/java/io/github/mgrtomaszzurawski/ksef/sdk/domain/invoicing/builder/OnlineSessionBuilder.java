/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FormCodeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSessionOpenRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.CryptoService;
import java.security.PublicKey;
import java.util.Objects;

/**
 * Builder for opening an online session.
 *
 * <p>Required: {@link FormCode}, KSeF public key for AES key wrapping.
 *
 * <p>Convenience factories {@link #fa2(PublicKey)} / {@link #fa3(PublicKey)}
 * delegate to the canonical {@link FormCode#FA2} / {@link FormCode#FA3}
 * constants. For non-FA invoice types use
 * {@link #fromFormCode(FormCode, PublicKey)} with one of the predefined
 * {@code FormCode} constants or {@link FormCode#custom(String, String, String)}.
 *
 * @since 1.0.0
 */
public final class OnlineSessionBuilder {

    private static final String ERR_NULL_FORM_CODE = "formCode must not be null";
    private static final String ERR_NULL_KSEF_PUBLIC_KEY = "ksefPublicKey is required";

    private final FormCode formCode;
    private final PublicKey ksefPublicKey;

    private OnlineSessionBuilder(FormCode formCode, PublicKey ksefPublicKey) {
        this.formCode = Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        this.ksefPublicKey = Objects.requireNonNull(ksefPublicKey, ERR_NULL_KSEF_PUBLIC_KEY);
    }

    /** FA(2) — delegates to {@link FormCode#FA2}. */
    public static OnlineSessionBuilder fa2(PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(FormCode.FA2, ksefPublicKey);
    }

    /** FA(3) — delegates to {@link FormCode#FA3}. */
    public static OnlineSessionBuilder fa3(PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(FormCode.FA3, ksefPublicKey);
    }

    /**
     * Open a session for an arbitrary {@link FormCode}. Use one of the
     * predefined {@code FormCode} constants ({@link FormCode#FA2},
     * {@link FormCode#FA3}, {@link FormCode#PEF3},
     * {@link FormCode#PEF_KOR3}) or
     * {@link FormCode#custom(String, String, String)} for non-standard
     * triples.
     */
    public static OnlineSessionBuilder fromFormCode(FormCode formCode, PublicKey ksefPublicKey) {
        return new OnlineSessionBuilder(formCode, ksefPublicKey);
    }

    public OnlineSessionBuilder toBuilder() {
        return new OnlineSessionBuilder(this.formCode, this.ksefPublicKey);
    }

    public OnlineSessionOpenRequest build() {
        byte[] aesKey = CryptoService.generateAesKey();
        byte[] initVector = CryptoService.generateIv();
        byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, ksefPublicKey);
        return new OnlineSessionOpenRequest(
                new FormCodeInfo(formCode.systemCode(), formCode.schemaVersion(), formCode.value()),
                encryptedKey, initVector, aesKey);
    }
}
