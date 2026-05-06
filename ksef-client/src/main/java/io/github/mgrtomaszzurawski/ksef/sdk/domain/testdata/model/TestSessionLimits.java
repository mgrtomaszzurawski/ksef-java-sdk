/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

/**
 * Session limit override values used in {@link TestSessionLimitsRequest}.
 *
 * @param maxInvoiceSizeMb max single invoice size in MB
 * @param maxInvoiceWithAttachmentSizeMb max invoice with attachment size in MB
 * @param maxInvoices max number of invoices per session
 *
 * @since 1.0.0
 */
public record TestSessionLimits(int maxInvoiceSizeMb, int maxInvoiceWithAttachmentSizeMb, int maxInvoices) {
}
