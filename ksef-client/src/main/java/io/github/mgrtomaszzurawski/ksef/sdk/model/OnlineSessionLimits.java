/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.OnlineSessionEffectiveContextLimitsRaw;

/**
 * Effective limits for online sessions.
 *
 * @param maxInvoiceSizeInMB maximum invoice size in megabytes
 * @param maxInvoiceWithAttachmentSizeInMB maximum invoice with attachment size in megabytes
 * @param maxInvoices maximum number of invoices per session
 */
public record OnlineSessionLimits(Integer maxInvoiceSizeInMB, Integer maxInvoiceWithAttachmentSizeInMB, Integer maxInvoices) {

    public static OnlineSessionLimits from(OnlineSessionEffectiveContextLimitsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new OnlineSessionLimits(
                raw.getMaxInvoiceSizeInMB(),
                raw.getMaxInvoiceWithAttachmentSizeInMB(),
                raw.getMaxInvoices());
    }
}
