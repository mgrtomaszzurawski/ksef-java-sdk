/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;

/**
 * Lifecycle phase 3 — an invoice that has been submitted, accepted, and
 * had its UPO retrieved. Completes the
 * {@code Invoice → SubmittedInvoice → ClearedInvoice} chain delivered
 * across PR12, PR10, and PR15 respectively.
 *
 * <p>Embeds the full {@link SubmittedInvoice} (which itself embeds the
 * source {@code Invoice} reference number + KSeF number + acceptance
 * timestamp), plus the {@link UpoEntry} carrying both the bit-exact
 * archive bytes and the parsed {@link UpoSummary}. Logging, archiving,
 * and downstream pipelines work uniformly against this single typed
 * carrier.
 *
 * @param submitted the post-submission record (see {@link SubmittedInvoice})
 * @param upo the UPO entry — XAdES-signed acknowledgement bytes + parsed view
 *
 * @since 1.0.0
 */
public record ClearedInvoice(SubmittedInvoice submitted, UpoEntry upo) {

    private static final String ERR_NULL_SUBMITTED = "submitted must not be null";
    private static final String ERR_NULL_UPO = "upo must not be null";

    public ClearedInvoice {
        Objects.requireNonNull(submitted, ERR_NULL_SUBMITTED);
        Objects.requireNonNull(upo, ERR_NULL_UPO);
    }
}
