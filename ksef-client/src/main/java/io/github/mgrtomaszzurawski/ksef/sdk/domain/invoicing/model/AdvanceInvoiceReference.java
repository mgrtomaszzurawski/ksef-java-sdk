/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * A reference to an advance-payment invoice settled by a final invoice — the
 * typed view of a {@code Fa/FakturaZaliczkowa} element. A final (settlement)
 * invoice lists each advance invoice it accounts for.
 *
 * <p>{@link #ksefNumber()} and {@link #withoutKsefNumber()} form the schema's
 * KSeF-number choice: either the referenced advance invoice carries its KSeF
 * number ({@code NrKSeFFaZaliczkowej}) or it is marked as having none
 * ({@code NrKSeFZN} = 1, e.g. an offline advance invoice).
 *
 * @param invoiceNumber the advance invoice number ({@code NrFaZaliczkowej})
 * @param ksefNumber the KSeF number of the advance invoice ({@code NrKSeFFaZaliczkowej}); null when it has none
 * @param withoutKsefNumber {@code true} when the advance invoice has no KSeF number ({@code NrKSeFZN} = 1); null when absent
 *
 * @since 0.1.2
 */
public record AdvanceInvoiceReference(
        @Nullable String invoiceNumber,
        @Nullable String ksefNumber,
        @Nullable Boolean withoutKsefNumber) {
}
