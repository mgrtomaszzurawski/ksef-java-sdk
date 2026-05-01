/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceResponseRaw;

/**
 * Result of sending an invoice within a session.
 *
 * @param referenceNumber invoice reference number for status polling and UPO retrieval
 */
public record SendInvoiceResult(String referenceNumber) {

    public static SendInvoiceResult from(SendInvoiceResponseRaw raw) {
        return new SendInvoiceResult(raw.getReferenceNumber());
    }
}
