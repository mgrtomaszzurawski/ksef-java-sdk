/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

/**
 * Authentication method backing a {@link KsefCredentials} instance.
 * Surface on {@link KsefCredentialsDescriptor} for safe diagnostics
 * (no secrets) — see {@link KsefClient#config()}.
 *
 * @since 0.1.0
 */
public enum AuthMethod {

    /** KSeF-issued token redeemed in {@code POST /auth/ksef-token}. */
    TOKEN,

    /** PKCS#12 keystore backing XAdES authentication. */
    PKCS12,

    /** Raw X.509 certificate + private key backing XAdES authentication. */
    CERTIFICATE
}
