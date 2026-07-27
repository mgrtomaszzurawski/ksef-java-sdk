/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * One contact entry for a party — the typed view of a
 * {@code Podmiot3/DaneKontaktowe} entry (a third party may carry up to
 * three). Both members are optional in the XSD.
 *
 * @param email contact e-mail address ({@code Email}); null when not supplied
 * @param phone contact phone number ({@code Telefon}); null when not supplied
 *
 * @since 0.1.0
 */
public record PartyContact(@Nullable String email, @Nullable String phone) {
}
