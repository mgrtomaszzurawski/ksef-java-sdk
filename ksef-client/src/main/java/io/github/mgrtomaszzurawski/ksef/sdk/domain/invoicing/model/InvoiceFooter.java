/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;

/**
 * The invoice footer — the typed view of the {@code Faktura/Stopka} node.
 * Null on {@code InvoiceDocument.footer()} when the invoice carries no
 * footer.
 *
 * <p>{@link #notes()} carries the free-text footer lines
 * ({@code Informacje/StopkaFaktury}, up to three); {@link #registries()}
 * carries the issuer's numbers in external registers ({@code Rejestry}).
 *
 * @param notes free-text footer lines ({@code Informacje/StopkaFaktury}); empty when none
 * @param registries external-register references ({@code Rejestry}); empty when none
 *
 * @since 0.1.0
 */
public record InvoiceFooter(List<String> notes, List<RegistryEntry> registries) {

    public InvoiceFooter {
        notes = List.copyOf(notes);
        registries = List.copyOf(registries);
    }
}
