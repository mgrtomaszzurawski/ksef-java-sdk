/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A postal address on an FA(2)/FA(3) invoice — the typed view of the
 * {@code TAdres} XSD complex type. Used for the transport ship-from,
 * ship-via and ship-to points and the carrier address under
 * {@code Fa/WarunkiTransakcji/Transport}.
 *
 * <p>KSeF models an address as two free-form lines plus a country code:
 * {@link #addressLine1()} carries the street and locality,
 * {@link #addressLine2()} an optional continuation (foreign addresses,
 * flat numbers). Both {@link #countryCode()} and {@link #addressLine1()}
 * are mandatory within {@code TAdres}.
 *
 * @param countryCode ISO 3166-1 alpha-2 country code ({@code KodKraju})
 * @param addressLine1 primary address line ({@code AdresL1})
 * @param addressLine2 optional secondary address line ({@code AdresL2}); null when absent
 * @param gln Global Location Number ({@code GLN}); null when absent
 *
 * @since 0.1.0
 */
public record InvoiceAddress(String countryCode, String addressLine1,
        @Nullable String addressLine2, @Nullable String gln) {

    private static final String ERR_NULL_COUNTRY = "countryCode must not be null";
    private static final String ERR_NULL_LINE1 = "addressLine1 must not be null";

    public InvoiceAddress {
        Objects.requireNonNull(countryCode, ERR_NULL_COUNTRY);
        Objects.requireNonNull(addressLine1, ERR_NULL_LINE1);
    }
}
