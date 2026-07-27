/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A buyer's full data as it appeared on the corrected invoice — the typed
 * view of a {@code Fa/Podmiot2K} entry (a correction may carry up to 101,
 * covering the {@code Podmiot2} buyer and any {@code Podmiot3} additional
 * buyers). Present on a correction that changes buyer data.
 *
 * <p>Identity ({@code DaneIdentyfikacyjne}, a {@code TPodmiot2}) is
 * mandatory within {@code Podmiot2K}; the address and buyer-link key are
 * optional.
 *
 * @param identity buyer identifying data ({@code DaneIdentyfikacyjne})
 * @param address buyer address ({@code Adres}); null when not supplied
 * @param buyerId buyer-link key matching {@code Podmiot2}/{@code Podmiot3} on
 *     the correcting invoice ({@code IDNabywcy}); null when not supplied
 *
 * @since 0.1.0
 */
public record CorrectionBuyer(PartyIdentity identity, @Nullable InvoiceAddress address,
        @Nullable String buyerId) {

    private static final String ERR_NULL_IDENTITY = "identity must not be null";

    public CorrectionBuyer {
        Objects.requireNonNull(identity, ERR_NULL_IDENTITY);
    }
}
