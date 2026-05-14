/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth;

import io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * How the auth XML is being signed — the signing-material side of a
 * KSeF XAdES auth exchange. Pairs with {@link XadesAuthRequest} on
 * {@code AuthClient.authenticateWithXades(...)}.
 *
 * @since 1.0.0
 */
public record XadesSigningMaterial(
        X509Certificate certificate,
        PrivateKey privateKey,
        SigningOptions signingOptions) {
}
