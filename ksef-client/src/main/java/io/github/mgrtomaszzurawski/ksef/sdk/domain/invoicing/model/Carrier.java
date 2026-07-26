/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;

/**
 * The carrier that performed a delivery — the typed view of
 * {@code Fa/WarunkiTransakcji/Transport/Przewoznik}. Both the identifying
 * data and the address are mandatory within {@code Przewoznik}.
 *
 * @param identity carrier identifying data ({@code DaneIdentyfikacyjne})
 * @param address carrier address ({@code AdresPrzewoznika})
 *
 * @since 0.1.0
 */
public record Carrier(TransportParty identity, InvoiceAddress address) {

    private static final String ERR_NULL_IDENTITY = "identity must not be null";
    private static final String ERR_NULL_ADDRESS = "address must not be null";

    public Carrier {
        Objects.requireNonNull(identity, ERR_NULL_IDENTITY);
        Objects.requireNonNull(address, ERR_NULL_ADDRESS);
    }
}
