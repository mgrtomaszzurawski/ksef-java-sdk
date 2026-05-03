/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

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

}
