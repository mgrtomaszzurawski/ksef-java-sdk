/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

/**
 * Per-session caps the server enforces on online (interactive) sessions
 * opened under the current authentication context. All three caps are
 * required by the server.
 *
 * <p>Typical values on KSeF demo (May 2026): 1 / 3 / 10000. Concrete
 * numbers vary per environment and per taxpayer — query at runtime via
 * {@code client.limits().getContextLimits()} rather than hard-coding.
 *
 * @param maxInvoiceSizeInMB maximum size in megabytes of a single
 *     invoice payload (the XML body). Exceeding this cap surfaces as a
 *     wire-level validation error on {@code sendInvoice}.
 * @param maxInvoiceWithAttachmentSizeInMB maximum size in megabytes of
 *     an invoice combined with its attachment payload. Tighter
 *     enforcement than {@link #maxInvoiceSizeInMB()} when the invoice
 *     carries an embedded attachment.
 * @param maxInvoices maximum total invoices that may be sent in a
 *     single online session before close. The session refuses further
 *     {@code sendInvoice} calls once the count is reached.
 *
 * @since 0.1.0
 */
public record OnlineSessionLimits(Integer maxInvoiceSizeInMB, Integer maxInvoiceWithAttachmentSizeInMB, Integer maxInvoices) {

}
