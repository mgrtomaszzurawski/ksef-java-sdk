/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * A reference to an underlying purchase order for the transaction — the
 * typed view of one {@code Fa/WarunkiTransakcji/Zamowienia} entry. Both
 * members are optional in the XSD.
 *
 * <p>Distinct from the advance-payment order tree ({@code Fa/Zamowienie},
 * singular), which itemises an order priced for an advance invoice.
 *
 * @param date order date ({@code DataZamowienia}); null when not supplied
 * @param number order number ({@code NrZamowienia}); null when not supplied
 *
 * @since 0.1.0
 */
public record PurchaseOrder(@Nullable LocalDate date, @Nullable String number) {
}
