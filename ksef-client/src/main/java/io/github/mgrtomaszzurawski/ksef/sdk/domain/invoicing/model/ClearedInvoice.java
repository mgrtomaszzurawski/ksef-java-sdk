/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import java.util.Objects;

/**
 * Lifecycle phase 3 — an invoice that has been submitted, accepted, and
 * had its UPO retrieved. Completes the
 * {@code Invoice → SubmittedInvoice → ClearedInvoice} chain.
 *
 * <p>Embeds the post-submission {@link SubmittedInvoice} record, the
 * typed {@link InvoiceDocument} as fetched from the KSeF archive (one of
 * {@code Fa2InvoiceDocument} / {@code Fa3InvoiceDocument} /
 * {@code PefInvoiceDocument} / {@code PefKorInvoiceDocument} when the
 * schema is recognised; the minimal {@link InvoiceDocument#fromXml}
 * wrapper for custom form codes), and the {@link UpoEntry} carrying the
 * XAdES-signed receipt bytes plus the parsed {@link UpoSummary}. Logging,
 * archiving, and downstream pipelines work uniformly against this single
 * typed carrier.
 *
 * @param submitted the post-submission record (see {@link SubmittedInvoice})
 * @param document  archived invoice as fetched from KSeF — pattern-match
 *     on the runtime type for typed access (e.g. {@code Fa3InvoiceDocument})
 * @param upo       the UPO entry — XAdES-signed acknowledgement bytes + parsed view
 *
 * @since 1.0.0
 */
public record ClearedInvoice(SubmittedInvoice submitted, InvoiceDocument document, UpoEntry upo) {

    private static final String ERR_NULL_SUBMITTED = "submitted must not be null";
    private static final String ERR_NULL_DOCUMENT = "document must not be null";
    private static final String ERR_NULL_UPO = "upo must not be null";

    public ClearedInvoice {
        Objects.requireNonNull(submitted, ERR_NULL_SUBMITTED);
        Objects.requireNonNull(document, ERR_NULL_DOCUMENT);
        Objects.requireNonNull(upo, ERR_NULL_UPO);
    }
}
