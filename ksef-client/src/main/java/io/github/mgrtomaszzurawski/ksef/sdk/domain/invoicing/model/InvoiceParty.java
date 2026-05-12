/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Generic invoice party (seller / buyer) shared between the FA(2) and
 * FA(3) typed builders.
 *
 * <p>Carries the minimum identifying information required to construct
 * a structurally valid {@code Podmiot1} (seller) or {@code Podmiot2}
 * (buyer) sub-element. Address fields are kept simple — Polish address
 * only — to satisfy the common-case build target. Consumers needing
 * foreign-address coverage or extended identification fields use
 * {@code Invoice.fromXml(...)} as the escape hatch.
 *
 * <p>For buyers ({@code Podmiot2}), the FA(3) XSD requires JST and GV
 * markers — JST = is this a sub-unit of a JST (Polish local-government
 * unit), GV = is this a member of a VAT group. The {@link #jst()} and
 * {@link #vatGroup()} fields surface those flags as typed setters; the
 * default for both is {@code false}, matching the common non-JST,
 * non-VAT-group buyer.
 *
 * @param nip Polish tax identifier (NIP) — exactly 10 digits
 * @param name registered taxpayer name (Nazwa)
 * @param postalCode Polish postal code, format {@code NN-NNN}
 * @param locality city / locality (Miejscowosc)
 * @param street street name (Ulica) — may be null when not applicable
 * @param houseNumber building number (NrDomu)
 * @param countryCode ISO 3166-1 alpha-2 country code; defaults to
 *     {@code "PL"} when null
 * @param jst is this party a sub-unit of a JST (Polish local-government
 *     unit)? Used as {@code Podmiot2/JST} on FA(3). Ignored for
 *     {@code Podmiot1}. Defaults to {@code false}.
 * @param vatGroup is this party a member of a VAT group? Used as
 *     {@code Podmiot2/GV} on FA(3). Ignored for {@code Podmiot1}.
 *     Defaults to {@code false}.
 *
 * @since 1.0.0
 */
public record InvoiceParty(
        String nip,
        String name,
        String postalCode,
        String locality,
        @Nullable String street,
        String houseNumber,
        @Nullable String countryCode,
        boolean jst,
        boolean vatGroup) {

    private static final String ERR_NULL_NIP = "nip must not be null";
    private static final String ERR_NULL_NAME = "name must not be null";
    private static final String ERR_NULL_POSTAL_CODE = "postalCode must not be null";
    private static final String ERR_NULL_LOCALITY = "locality must not be null";
    private static final String ERR_NULL_HOUSE_NUMBER = "houseNumber must not be null";
    private static final String ERR_BAD_NIP = "nip must be exactly 10 digits";
    private static final String NIP_PATTERN = "\\d{10}";

    /** Default country code (Polish) when {@link #countryCode()} not supplied. */
    public static final String DEFAULT_COUNTRY_CODE = "PL";

    public InvoiceParty {
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        Objects.requireNonNull(name, ERR_NULL_NAME);
        Objects.requireNonNull(postalCode, ERR_NULL_POSTAL_CODE);
        Objects.requireNonNull(locality, ERR_NULL_LOCALITY);
        Objects.requireNonNull(houseNumber, ERR_NULL_HOUSE_NUMBER);
        if (!nip.matches(NIP_PATTERN)) {
            throw new IllegalArgumentException(ERR_BAD_NIP);
        }
    }

    /**
     * Convenience 7-arg constructor that defaults both {@link #jst()}
     * and {@link #vatGroup()} to {@code false} — the common case for
     * non-JST, non-VAT-group parties.
     */
    public InvoiceParty(String nip,
                        String name,
                        String postalCode,
                        String locality,
                        @Nullable String street,
                        String houseNumber,
                        @Nullable String countryCode) {
        this(nip, name, postalCode, locality, street, houseNumber, countryCode, false, false);
    }

    /**
     * Resolve {@link #countryCode()} to a non-null value, falling back
     * to {@link #DEFAULT_COUNTRY_CODE}.
     */
    public String resolvedCountryCode() {
        return countryCode != null ? countryCode : DEFAULT_COUNTRY_CODE;
    }
}
