/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;





import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.InvoiceDocument;
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
 * schema is recognised; an anonymous wrapper for custom form codes),
 * and the {@link UpoEntry} carrying the XAdES-signed receipt bytes plus
 * the parsed {@link UpoSummary}. Logging, archiving, and downstream
 * pipelines work uniformly against this single typed carrier.
 *
 * <p>The {@code <I>} parameter is propagated from {@link SubmittedInvoice}
 * so callers do not need to downcast {@code submitted().invoice()} after
 * a successful clearance. {@link #document()} is intentionally typed as
 * the open {@link InvoiceDocument} (no associated-type for read-side):
 * the runtime instance is the typed {@code Fa3InvoiceDocument} /
 * {@code Fa2InvoiceDocument} / {@code PefInvoiceDocument} /
 * {@code PefKorInvoiceDocument} for known schemas — pattern-match in
 * one line ({@code if (doc instanceof Fa3InvoiceDocument fa3)}) when
 * typed accessors are needed.
 *
 * @param <I> the static {@link Invoice} subtype propagated from the
 *     embedded {@link SubmittedInvoice}
 * @param submitted the post-submission record (see {@link SubmittedInvoice})
 * @param document  archived invoice as fetched from KSeF — pattern-match
 *     on the runtime type for typed access (e.g. {@code Fa3InvoiceDocument})
 * @param upo       the UPO entry — XAdES-signed acknowledgement bytes + parsed view
 *
 * @since 1.0.0
 */
public record ClearedInvoice<I extends Invoice>(
        SubmittedInvoice<I> submitted, InvoiceDocument document, UpoEntry upo) {

    private static final String ERR_NULL_SUBMITTED = "submitted must not be null";
    private static final String ERR_NULL_DOCUMENT = "document must not be null";
    private static final String ERR_NULL_UPO = "upo must not be null";

    public ClearedInvoice {
        Objects.requireNonNull(submitted, ERR_NULL_SUBMITTED);
        Objects.requireNonNull(document, ERR_NULL_DOCUMENT);
        Objects.requireNonNull(upo, ERR_NULL_UPO);
    }
}
