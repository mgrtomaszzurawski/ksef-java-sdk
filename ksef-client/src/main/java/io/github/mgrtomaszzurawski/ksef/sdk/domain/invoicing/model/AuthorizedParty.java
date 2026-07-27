/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An authorised party associated with the invoice — the typed view of
 * {@code Faktura/PodmiotUpowazniony} (e.g. a party authorised to issue or
 * receive on behalf of the seller or buyer).
 *
 * <p>Identity ({@code DaneIdentyfikacyjne}, a {@code TPodmiot1}), address
 * and role are mandatory within {@code PodmiotUpowazniony}; everything
 * else is optional.
 *
 * @param nip party tax identifier ({@code DaneIdentyfikacyjne/NIP})
 * @param name party name ({@code DaneIdentyfikacyjne/Nazwa})
 * @param eori EORI number ({@code NrEORI}); null when not supplied
 * @param address address ({@code Adres})
 * @param correspondenceAddress correspondence address ({@code AdresKoresp}); null when not supplied
 * @param contacts contact entries ({@code DaneKontaktowe}); empty when none
 * @param role coded party role ({@code RolaPU})
 *
 * @since 0.1.0
 */
public record AuthorizedParty(
        String nip,
        String name,
        @Nullable String eori,
        InvoiceAddress address,
        @Nullable InvoiceAddress correspondenceAddress,
        List<PartyContact> contacts,
        Integer role) {

    private static final String ERR_NULL_NIP = "nip must not be null";
    private static final String ERR_NULL_NAME = "name must not be null";
    private static final String ERR_NULL_ADDRESS = "address must not be null";
    private static final String ERR_NULL_ROLE = "role must not be null";

    public AuthorizedParty {
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        Objects.requireNonNull(name, ERR_NULL_NAME);
        Objects.requireNonNull(address, ERR_NULL_ADDRESS);
        Objects.requireNonNull(role, ERR_NULL_ROLE);
        contacts = List.copyOf(contacts);
    }
}
