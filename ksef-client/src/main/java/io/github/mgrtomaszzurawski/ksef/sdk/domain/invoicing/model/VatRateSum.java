/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One entry of the invoice's VAT-rate breakdown — pair of
 * {@code Fa/P_13_x} (net) and the corresponding {@code Fa/P_14_x}
 * (VAT) or net-only for non-taxable buckets.
 *
 * <p>{@link #vatAmount()} is null for buckets that the spec defines
 * as net-only (zero-rate categories, exempt, out-of-territory,
 * intra-EU services, reverse charge, margin) — the wire format has no
 * {@code P_14_x} for those.
 *
 * <p>{@link #vatAmountConvertedToPln()} carries the PLN-converted VAT
 * ({@code Fa/P_14_x_W}) present only on foreign-currency invoices, and
 * only for the four buckets the spec defines it for
 * ({@link VatRateBucket#STANDARD}, {@link VatRateBucket#REDUCED_FIRST},
 * {@link VatRateBucket#REDUCED_SECOND}, {@link VatRateBucket#TAXI_LUMP_SUM}).
 * It is null everywhere else.
 *
 * @param bucket which rate / scheme this sum is for
 * @param netAmount net amount summed across all line items in this bucket
 * @param vatAmount VAT amount summed across all line items in this bucket;
 *     null when the bucket has no VAT line in the spec
 * @param vatAmountConvertedToPln VAT amount converted to PLN
 *     ({@code P_14_x_W}) on a foreign-currency invoice; null when the
 *     invoice is in PLN or the bucket has no {@code _W} field
 *
 * @since 0.1.0
 */
public record VatRateSum(VatRateBucket bucket, BigDecimal netAmount, @Nullable BigDecimal vatAmount,
        @Nullable BigDecimal vatAmountConvertedToPln) {

    private static final String ERR_NULL_BUCKET = "bucket must not be null";
    private static final String ERR_NULL_NET = "netAmount must not be null";

    public VatRateSum {
        Objects.requireNonNull(bucket, ERR_NULL_BUCKET);
        Objects.requireNonNull(netAmount, ERR_NULL_NET);
    }
}
