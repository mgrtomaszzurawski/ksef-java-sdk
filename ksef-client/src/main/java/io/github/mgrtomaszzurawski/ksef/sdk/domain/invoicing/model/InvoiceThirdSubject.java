/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * Third subject (mediator, representative) on an invoice.
 *
 * @param identifierType type of identifier (null when no identifier was supplied)
 * @param identifierValue identifier value (null when no identifier)
 * @param name subject name (null when not provided)
 * @param role role code of the third subject (null when not provided)
 *
 * @since 0.1.0
 */
public record InvoiceThirdSubject(
        @Nullable ThirdSubjectIdentifierType identifierType,
        @Nullable String identifierValue,
        @Nullable String name,
        @Nullable Integer role) {

}
