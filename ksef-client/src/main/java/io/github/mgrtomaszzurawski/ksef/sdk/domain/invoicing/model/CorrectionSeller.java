/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The seller's full data as it appeared on the corrected invoice — the
 * typed view of {@code Fa/Podmiot1K}. Present on a correction that
 * changes seller data; carries the pre-correction seller so the change is
 * traceable. Does not apply to correcting a wrong NIP (that requires a
 * correction to zero).
 *
 * <p>Identity ({@code DaneIdentyfikacyjne}, a {@code TPodmiot1}) and
 * address are mandatory within {@code Podmiot1K}; the taxpayer EU prefix
 * is optional.
 *
 * @param nip seller tax identifier ({@code DaneIdentyfikacyjne/NIP})
 * @param name seller name ({@code DaneIdentyfikacyjne/Nazwa})
 * @param address seller address ({@code Adres})
 * @param taxpayerPrefix EU VAT country prefix ({@code PrefiksPodatnika}); null when not supplied
 *
 * @since 0.1.0
 */
public record CorrectionSeller(String nip, String name, InvoiceAddress address,
        @Nullable String taxpayerPrefix) {

    private static final String ERR_NULL_NIP = "nip must not be null";
    private static final String ERR_NULL_NAME = "name must not be null";
    private static final String ERR_NULL_ADDRESS = "address must not be null";

    public CorrectionSeller {
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        Objects.requireNonNull(name, ERR_NULL_NAME);
        Objects.requireNonNull(address, ERR_NULL_ADDRESS);
    }
}
