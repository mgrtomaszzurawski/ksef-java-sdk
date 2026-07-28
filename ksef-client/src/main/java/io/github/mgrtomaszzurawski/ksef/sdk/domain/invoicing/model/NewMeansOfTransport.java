/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * New-means-of-transport annotations of an invoice — the typed view of the
 * {@code Fa/Adnotacje/NoweSrodkiTransportu} node (intra-Community supply of
 * new means of transport, art. 42 ust. 5 of the VAT Act). Null on
 * {@code InvoiceDocument.newMeansOfTransport()} when the invoice carries no
 * such annotation.
 *
 * <p>The node is an XSD choice. On the positive branch {@link #intraCommunitySupply()}
 * ({@code P_22}) is set, {@link #article42Paragraph5()} ({@code P_42_5}) records
 * whether the art. 42 ust. 5 obligation applies and {@link #items()} lists each
 * supplied means of transport. On the negative branch {@link #noIntraCommunitySupply()}
 * ({@code P_22N}) is set and {@link #items()} is empty.
 *
 * @param intraCommunitySupply intra-Community supply of new means of transport present ({@code P_22}, marker {@code 1}); null when not flagged
 * @param article42Paragraph5 art. 42 ust. 5 obligation applies ({@code P_42_5}: {@code true} = 1, {@code false} = 2); null when absent
 * @param items the supplied means of transport ({@code NowySrodekTransportu}); empty on the negative branch
 * @param noIntraCommunitySupply no intra-Community supply of new means of transport ({@code P_22N}, marker {@code 1}); null when not flagged
 *
 * @since 0.1.0
 */
public record NewMeansOfTransport(
        @Nullable Boolean intraCommunitySupply,
        @Nullable Boolean article42Paragraph5,
        List<NewTransportItem> items,
        @Nullable Boolean noIntraCommunitySupply) {

    public NewMeansOfTransport {
        items = List.copyOf(items);
    }
}
