/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * UBL postal address used by PEF(3) and PEF_KOR(3) party records
 * (AccountingSupplierParty, AccountingCustomerParty).
 *
 * @param streetName street name (cbc:StreetName)
 * @param cityName city / locality (cbc:CityName)
 * @param postalZone postal code (cbc:PostalZone)
 * @param countryCode ISO 3166-1 alpha-2 country code (cac:Country/cbc:IdentificationCode)
 *
 * @since 1.0.0
 */
public record PefAddress(
        @Nullable String streetName,
        String cityName,
        String postalZone,
        String countryCode) {

    private static final String ERR_NULL_CITY = "cityName must not be null";
    private static final String ERR_NULL_POSTAL_ZONE = "postalZone must not be null";
    private static final String ERR_NULL_COUNTRY_CODE = "countryCode must not be null";

    public PefAddress {
        Objects.requireNonNull(cityName, ERR_NULL_CITY);
        Objects.requireNonNull(postalZone, ERR_NULL_POSTAL_ZONE);
        Objects.requireNonNull(countryCode, ERR_NULL_COUNTRY_CODE);
    }
}
