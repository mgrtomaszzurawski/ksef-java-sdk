/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

/**
 * Per-session caps the server enforces on batch sessions opened under
 * the current authentication context. Constrains the contents of each
 * part inside a batch package, not the aggregate package size —
 * aggregate caps are fixed by spec at 50 parts / 5 GB pre-encryption
 * per package and enforced internally by the SDK.
 *
 * <p>Typical values on KSeF demo (May 2026): 1 / 3 / 10000. Concrete
 * numbers vary per environment and per taxpayer — query at runtime via
 * {@code client.limits().getContextLimits()} rather than hard-coding.
 *
 * @param maxInvoiceSizeInMB maximum size in megabytes of a single
 *     invoice payload inside any batch part. Server validates this on
 *     batch close after decrypting the parts.
 * @param maxInvoiceWithAttachmentSizeInMB maximum size in megabytes of
 *     an invoice combined with its attachment payload inside a batch
 *     part.
 * @param maxInvoices maximum total invoices the entire batch package
 *     may carry across all parts. Exceeding this cap surfaces as a
 *     wire-level validation error on batch close.
 *
 * @since 1.0.0
 */
public record BatchSessionLimits(Integer maxInvoiceSizeInMB, Integer maxInvoiceWithAttachmentSizeInMB, Integer maxInvoices) {

}
