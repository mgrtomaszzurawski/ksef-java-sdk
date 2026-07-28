/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * Margin-scheme annotations of an invoice — the typed view of the
 * {@code Fa/Adnotacje/PMarzy} node. Indicates whether the invoice is
 * settled under a VAT margin procedure (art. 119 or 120) and, if so,
 * which one.
 *
 * <p>The markers form an XSD choice: either {@link #present()} = 1 with
 * exactly one of the specific-scheme markers, or {@link #none()} = 1.
 * Each member is a single-choice marker surfaced as a nullable boolean
 * (true when the marker carries value 1, null when absent).
 *
 * @param present margin procedure present, art. 119 / 120 ({@code P_PMarzy}); null when not flagged
 * @param travelAgency travel-agency margin, art. 119 ({@code P_PMarzy_2}); null when not flagged
 * @param usedGoods used-goods margin, art. 120 ({@code P_PMarzy_3_1}); null when not flagged
 * @param worksOfArt works-of-art margin, art. 120 ({@code P_PMarzy_3_2}); null when not flagged
 * @param collectorsItems collectors'-items and antiques margin, art. 120 ({@code P_PMarzy_3_3}); null when not flagged
 * @param none no margin procedure applies ({@code P_PMarzyN}); null when not flagged
 *
 * @since 0.1.0
 */
public record MarginScheme(
        @Nullable Boolean present,
        @Nullable Boolean travelAgency,
        @Nullable Boolean usedGoods,
        @Nullable Boolean worksOfArt,
        @Nullable Boolean collectorsItems,
        @Nullable Boolean none) {
}
