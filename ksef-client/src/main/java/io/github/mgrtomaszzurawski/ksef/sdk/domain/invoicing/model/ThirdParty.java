/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A third party associated with the invoice — the typed view of a
 * {@code Faktura/Podmiot3} entry (an invoice may carry up to a hundred).
 * These are parties other than the seller ({@code Podmiot1}) and the
 * buyer ({@code Podmiot2}) — additional buyers, factors, recipients, etc.
 *
 * <p>The party role is an XSD choice surfaced as sibling nullable
 * members: either a coded {@link #role()} ({@code Rola}) or the "other"
 * marker {@link #otherRole()} ({@code RolaInna}) detailed by
 * {@link #roleDescription()}. {@link #identity()} is mandatory in the XSD;
 * everything else is optional.
 *
 * <p>This is the FA XML document view of a {@code Podmiot3}. The REST
 * invoice-metadata surface models the same third party as
 * {@link InvoiceThirdSubject}, with the identifier as a type/value pair
 * rather than the sibling-nullable identity branches used here.
 *
 * @param identity identifying data ({@code DaneIdentyfikacyjne}); never null
 * @param buyerId correction buyer-link key ({@code IDNabywcy}); null when not supplied
 * @param eori EORI number ({@code NrEORI}); null when not supplied
 * @param address address ({@code Adres}); null when not supplied
 * @param correspondenceAddress correspondence address ({@code AdresKoresp}); null when not supplied
 * @param contacts contact entries ({@code DaneKontaktowe}); empty when none
 * @param role coded party role ({@code Rola}); null when "other"
 * @param otherRole "other role" marker ({@code RolaInna = 1}); null when a coded role is used
 * @param roleDescription free-text detail of the other role ({@code OpisRoli}); null when a coded role is used
 * @param share percentage share of an additional buyer ({@code Udzial}); null when not supplied
 * @param customerNumber customer number used in a contract or order ({@code NrKlienta}); null when not supplied
 *
 * @since 0.1.0
 */
public record ThirdParty(
        ThirdPartyIdentity identity,
        @Nullable String buyerId,
        @Nullable String eori,
        @Nullable InvoiceAddress address,
        @Nullable InvoiceAddress correspondenceAddress,
        List<PartyContact> contacts,
        @Nullable Integer role,
        @Nullable Boolean otherRole,
        @Nullable String roleDescription,
        @Nullable BigDecimal share,
        @Nullable String customerNumber) {

    private static final String ERR_NULL_IDENTITY = "identity must not be null";

    public ThirdParty {
        Objects.requireNonNull(identity, ERR_NULL_IDENTITY);
        contacts = List.copyOf(contacts);
    }
}
