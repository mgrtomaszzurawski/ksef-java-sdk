/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefIdentifier;
import java.util.Objects;

/**
 * Invoice seller information from metadata.
 *
 * @param nip seller NIP (tax ID)
 * @param name seller name (may be null)
 *
 * @since 0.1.0
 */
public record InvoiceSeller(String nip, String name) {

    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";

    /**
     * Whether this seller matches the supplied authentication-context
     * identifier. KSeF sellers are always NIP-typed at the spec level, so
     * non-NIP identifiers (INTERNAL_ID, NIP_VAT_UE, PEPPOL_ID) never match.
     * NIP comparison is exact (NIPs are digit-only; case folding is moot).
     * Returns {@code false} when this seller's {@code nip} is null.
     */
    public boolean matches(KsefIdentifier identifier) {
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
        return identifier.type() == KsefIdentifier.Type.NIP
                && nip != null
                && nip.equals(identifier.value());
    }
}
