/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One additional free-form description entry ({@code Fa/DodatkowyOpis}) —
 * a key/value pair the issuer may attach for data required by law that has
 * no dedicated field. Maps onto the {@code TKluczWartosc} XSD type.
 *
 * @param rowNumber optional line reference the entry relates to
 *     ({@code NrWiersza}); null when it applies to the whole invoice
 * @param key the entry key ({@code Klucz})
 * @param value the entry value ({@code Wartosc})
 *
 * @since 0.1.0
 */
public record AdditionalDescription(@Nullable Integer rowNumber, String key, String value) {

    private static final String ERR_NULL_KEY = "key must not be null";
    private static final String ERR_NULL_VALUE = "value must not be null";

    public AdditionalDescription {
        Objects.requireNonNull(key, ERR_NULL_KEY);
        Objects.requireNonNull(value, ERR_NULL_VALUE);
    }
}
