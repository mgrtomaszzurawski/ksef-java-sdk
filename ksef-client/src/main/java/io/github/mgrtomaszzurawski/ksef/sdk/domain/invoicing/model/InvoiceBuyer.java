/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataBuyerRaw;

/**
 * Invoice buyer information from metadata.
 *
 * @param identifierType type of buyer identifier
 * @param identifierValue buyer identifier value (NIP, VAT UE number, etc.)
 * @param name buyer name (may be null)
 */
public record InvoiceBuyer(BuyerIdentifierType identifierType, String identifierValue, String name) {

    public static InvoiceBuyer from(InvoiceMetadataBuyerRaw raw) {
        if (raw == null) {
            return null;
        }
        BuyerIdentifierType idType = null;
        String idValue = null;
        if (raw.getIdentifier() != null) {
            idType = BuyerIdentifierType.from(raw.getIdentifier().getType());
            idValue = raw.getIdentifier().getValue();
        }
        return new InvoiceBuyer(idType, idValue, raw.getName());
    }
}
