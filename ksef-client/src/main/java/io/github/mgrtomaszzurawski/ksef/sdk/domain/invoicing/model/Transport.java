/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One transport leg of a delivery — the typed view of a
 * {@code Fa/WarunkiTransakcji/Transport} entry (an invoice may carry up
 * to twenty).
 *
 * <p>Two XSD choices are surfaced as sibling nullable members: the means
 * of transport is either a coded {@link #transportType()}
 * ({@code RodzajTransportu}) or the "other" marker {@link #otherTransport()}
 * ({@code TransportInny}) detailed by {@link #otherTransportDescription()};
 * the load is either a coded {@link #loadType()} ({@code OpisLadunku}) or
 * the "other" marker {@link #otherLoad()} ({@code LadunekInny}) detailed by
 * {@link #otherLoadDescription()}. Exactly one branch of each choice is
 * populated.
 *
 * @param transportType coded means of transport ({@code RodzajTransportu}); null when "other"
 * @param otherTransport "other means of transport" marker ({@code TransportInny = 1}); null when a coded means is used
 * @param otherTransportDescription free-text detail of the other means ({@code OpisInnegoTransportu}); null when a coded means is used
 * @param carrier the carrier ({@code Przewoznik}); null when not supplied
 * @param transportOrderNumber transport order number ({@code NrZleceniaTransportu}); null when not supplied
 * @param loadType coded load type ({@code OpisLadunku}); null when "other"
 * @param otherLoad "other load" marker ({@code LadunekInny = 1}); null when a coded load is used
 * @param otherLoadDescription free-text detail of the other load ({@code OpisInnegoLadunku}); null when a coded load is used
 * @param packagingUnit packaging unit ({@code JednostkaOpakowania}); null when not supplied
 * @param transportStart transport start timestamp ({@code DataGodzRozpTransportu}); null when not supplied
 * @param transportEnd transport end timestamp ({@code DataGodzZakTransportu}); null when not supplied
 * @param shipFrom origin address ({@code WysylkaZ}); null when not supplied
 * @param shipVia intermediate addresses ({@code WysylkaPrzez}); empty when none
 * @param shipTo destination address ({@code WysylkaDo}); null when not supplied
 *
 * @since 0.1.0
 */
public record Transport(
        @Nullable Integer transportType,
        @Nullable Boolean otherTransport,
        @Nullable String otherTransportDescription,
        @Nullable Carrier carrier,
        @Nullable String transportOrderNumber,
        @Nullable Integer loadType,
        @Nullable Boolean otherLoad,
        @Nullable String otherLoadDescription,
        @Nullable String packagingUnit,
        @Nullable OffsetDateTime transportStart,
        @Nullable OffsetDateTime transportEnd,
        @Nullable InvoiceAddress shipFrom,
        List<InvoiceAddress> shipVia,
        @Nullable InvoiceAddress shipTo) {

    public Transport {
        shipVia = List.copyOf(shipVia);
    }
}
