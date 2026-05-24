/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * Authorized subject row on {@link InvoiceMetadata} — the party who
 * actually issued the invoice on behalf of the seller (typical pattern:
 * a bookkeeping / accounting firm acting as proxy for the seller).
 *
 * <p>The role field corresponds to the server-assigned numeric role code
 * (see KSeF spec — typical values include {@code 1} for self, {@code 2}
 * for representative, etc.).
 *
 * @param nip Polish tax identifier of the authorized subject
 * @param name registered name of the authorized subject (may be null
 *     when the server omits the name for this row)
 * @param role server-assigned role code for the authorization
 *     (may be null when the server omits the role)
 *
 * @since 0.1.0
 */
public record InvoiceAuthorizedSubject(
        String nip,
        @Nullable String name,
        @Nullable Integer role) {
}
