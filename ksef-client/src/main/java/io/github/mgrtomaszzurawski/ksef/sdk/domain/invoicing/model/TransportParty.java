/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * Identifying data of a transaction counterparty carried under a
 * {@code TPodmiot2} XSD complex type — used here for the carrier
 * ({@code Fa/WarunkiTransakcji/Transport/Przewoznik/DaneIdentyfikacyjne}).
 *
 * <p>The tax identifier is an XSD choice: exactly one of a Polish NIP
 * ({@link #nip()}), an EU VAT number ({@link #euVatPrefix()} +
 * {@link #euVatNumber()}), another tax identifier with its issuing
 * country ({@link #taxIdCountryCode()} + {@link #otherTaxId()}), or the
 * "no identifier" marker ({@link #noTaxId()}). All members are therefore
 * nullable; the branch that does not apply is null. {@link #name()} is
 * optional in every branch.
 *
 * @param nip Polish tax identifier ({@code NIP}); null unless the NIP branch applies
 * @param euVatPrefix EU VAT country prefix ({@code KodUE}); null unless the EU-VAT branch applies
 * @param euVatNumber EU VAT number ({@code NrVatUE}); null unless the EU-VAT branch applies
 * @param taxIdCountryCode country that issued the other tax id ({@code KodKraju}); null otherwise
 * @param otherTaxId other tax identifier ({@code NrID}); null unless the other-id branch applies
 * @param noTaxId "no tax identifier" marker ({@code BrakID = 1}); null unless that branch applies
 * @param name party name ({@code Nazwa}); null when not supplied
 *
 * @since 0.1.0
 */
public record TransportParty(
        @Nullable String nip,
        @Nullable String euVatPrefix,
        @Nullable String euVatNumber,
        @Nullable String taxIdCountryCode,
        @Nullable String otherTaxId,
        @Nullable Boolean noTaxId,
        @Nullable String name) {
}
