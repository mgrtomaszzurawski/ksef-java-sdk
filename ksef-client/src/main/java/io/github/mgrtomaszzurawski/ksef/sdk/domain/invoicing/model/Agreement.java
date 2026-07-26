/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * A reference to an underlying contract for the transaction — the typed
 * view of one {@code Fa/WarunkiTransakcji/Umowy} entry. Both members are
 * optional in the XSD.
 *
 * @param date contract date ({@code DataUmowy}); null when not supplied
 * @param number contract number ({@code NrUmowy}); null when not supplied
 *
 * @since 0.1.0
 */
public record Agreement(@Nullable LocalDate date, @Nullable String number) {
}
