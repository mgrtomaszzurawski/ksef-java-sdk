/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataThirdSubjectRaw;

/**
 * Third subject (mediator, representative) on an invoice.
 *
 * @param identifierType type of identifier
 * @param identifierValue identifier value
 * @param name subject name (may be null)
 * @param role role code of the third subject
 */
public record InvoiceThirdSubject(
        ThirdSubjectIdentifierType identifierType,
        String identifierValue,
        String name,
        Integer role) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static InvoiceThirdSubject from(InvoiceMetadataThirdSubjectRaw raw) {
        ThirdSubjectIdentifierType idType = null;
        String idValue = null;
        if (raw.getIdentifier() != null) {
            idType = ThirdSubjectIdentifierType.from(raw.getIdentifier().getType());
            idValue = raw.getIdentifier().getValue();
        }
        return new InvoiceThirdSubject(idType, idValue, raw.getName(), raw.getRole());
    }
}
