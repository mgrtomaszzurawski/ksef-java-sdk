/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * VAT-rate bucket on the FA(2)/FA(3) invoice summary
 * ({@code Fa/P_13_x} + {@code Fa/P_14_x}). Maps spec field positions
 * to a stable enum so consumers can switch over buckets without
 * hard-coding rate strings (which the spec itself allows to vary —
 * the standard rate is "currently 23% or 22%" depending on
 * legislative cycle).
 *
 * <p>Buckets carry their wire field positions; consumers reading
 * {@link VatRateSum} get the correct net / VAT pair without manual
 * P_13/P_14 indexing.
 *
 * @since 0.1.0
 */
public enum VatRateBucket {

    /** Standard rate — currently 23% (or 22% in earlier legislative cycles). P_13_1 / P_14_1. */
    STANDARD,
    /** First reduced rate — currently 8% (or 7% in earlier cycles). P_13_2 / P_14_2. */
    REDUCED_FIRST,
    /** Second reduced rate — currently 5%. P_13_3 / P_14_3. */
    REDUCED_SECOND,
    /** Flat-rate taxi-passenger lump-sum scheme. P_13_4 / P_14_4. */
    TAXI_LUMP_SUM,
    /** Dział XII special procedure (e.g. OSS / IOSS for distance-selling). P_13_5 / P_14_5. */
    SPECIAL_PROCEDURE,
    /** Exempt from VAT. P_13_7 (net only). */
    EXEMPT,
    /** Out-of-territory supply. P_13_8 (net only). */
    OUTSIDE_TERRITORY,
    /** Intra-EU services per art. 100 ust. 1 pkt 4 ustawy. P_13_9 (net only). */
    INTRA_EU_SERVICES,
    /** Reverse-charge — buyer is the taxpayer. P_13_10 (net only). */
    REVERSE_CHARGE,
    /** Margin scheme per art. 119 / art. 120. P_13_11 (net only). */
    MARGIN_SCHEME
}
