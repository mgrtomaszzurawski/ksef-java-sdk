/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Effective limits for batch sessions.
 *
 * @param maxInvoiceSizeInMB maximum invoice size in megabytes
 * @param maxInvoiceWithAttachmentSizeInMB maximum invoice with attachment size in megabytes
 * @param maxInvoices maximum number of invoices per session
 */
public record BatchSessionLimits(Integer maxInvoiceSizeInMB, Integer maxInvoiceWithAttachmentSizeInMB, Integer maxInvoices) {

}
