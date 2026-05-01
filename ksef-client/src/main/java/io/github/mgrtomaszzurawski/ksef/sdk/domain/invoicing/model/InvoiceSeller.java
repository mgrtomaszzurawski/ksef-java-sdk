/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataSellerRaw;

/**
 * Invoice seller information from metadata.
 *
 * @param nip seller NIP (tax ID)
 * @param name seller name (may be null)
 */
public record InvoiceSeller(String nip, String name) {

    public static InvoiceSeller from(InvoiceMetadataSellerRaw raw) {
        if (raw == null) {
            return null;
        }
        return new InvoiceSeller(raw.getNip(), raw.getName());
    }
}
