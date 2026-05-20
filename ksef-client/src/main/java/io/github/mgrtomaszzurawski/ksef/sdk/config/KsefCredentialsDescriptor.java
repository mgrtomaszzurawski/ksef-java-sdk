/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefIdentifier;

/**
 * Masked summary of a {@link KsefCredentials} instance — safe to log,
 * include in diagnostics, or compare across clients without exposing
 * secrets (tokens, private keys, PKCS#12 passwords).
 *
 * <p>Surfaces:
 * <ul>
 *   <li>{@link AuthMethod} — which backing flow is in use</li>
 *   <li>{@link KsefIdentifier.Type} — the identifier kind the consumer
 *       authenticated under (NIP / PESEL / EU-VAT-UE / Peppol)</li>
 *   <li>{@code identifierMasked} — last four characters of the
 *       identifier value, prefixed with {@code ***}. Enough to
 *       distinguish two clients in a log line; not enough to leak the
 *       full identifier.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public record KsefCredentialsDescriptor(
        AuthMethod authMethod,
        KsefIdentifier.Type identifierType,
        String identifierMasked) {

    private static final int MASKED_TAIL_LENGTH = 4;
    private static final String MASK_PREFIX = "***";

    /** Build a descriptor with the canonical {@code ***NNNN} tail-mask. */
    public static KsefCredentialsDescriptor of(AuthMethod authMethod,
                                                 KsefIdentifier identifier) {
        return new KsefCredentialsDescriptor(authMethod, identifier.type(),
                maskTail(identifier.value()));
    }

    private static String maskTail(String value) {
        int from = Math.max(0, value.length() - MASKED_TAIL_LENGTH);
        return MASK_PREFIX + value.substring(from);
    }
}
