/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive.InvoiceArchive;

/**
 * Read-side counterpart of {@link Invoice}. Returned by
 * {@link InvoiceArchive#getByKsefNumber(io.github.mgrtomaszzurawski.ksef.sdk.core.KsefNumber)}
 * for invoices fetched from KSeF (issued by either the consumer or by a
 * counterparty). Open interface — pattern-match (`instanceof` chain) on
 * the concrete typed subtypes when you need schema-specific accessors.
 *
 * <pre>{@code
 * InvoiceDocument doc = client.invoices().archive().getByKsefNumber(num);
 * if (doc instanceof Fa3InvoiceDocument fa3) {
 *     processFa3(fa3.unsafeJaxbView());
 * } else if (doc instanceof Fa2InvoiceDocument fa2) {
 *     processFa2(fa2.unsafeJaxbView());
 * } else if (doc instanceof PefInvoiceDocument pef) {
 *     processPef(pef.invoice());
 * } else if (doc instanceof PefKorInvoiceDocument pefKor) {
 *     processPefKor(pefKor.creditNote());
 * } else {
 *     archive(doc.xml(), doc.formCode());   // unknown / custom forward-compat arm
 * }
 * }</pre>
 *
 * <p><strong>Authored vs fetched.</strong> {@link Invoice} carries data
 * the consumer is about to send (mutable until {@code .build()}, then
 * frozen). {@link InvoiceDocument} carries data already in KSeF —
 * read-only, cannot be re-sent, can have UPO retrieved. Two interfaces
 * keep the roles distinct so that misuse doesn't compile.
 *
 * @since 1.0.0
 */
public interface InvoiceDocument {

    /**
     * Schema this document conforms to. Drives downstream branching for
     * pattern-matching consumers.
     */
    FormCode formCode();

    /**
     * Raw XML bytes the SDK fetched from KSeF — bit-exact, no
     * canonicalisation. Defensive copy on every call.
     */
    byte[] xml();
}
