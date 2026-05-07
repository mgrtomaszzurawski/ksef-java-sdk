/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * Invoice buyer information from metadata.
 *
 * @param identifierType type of buyer identifier (null when no identifier was supplied)
 * @param identifierValue buyer identifier value (NIP, VAT UE number, etc.; null when no identifier)
 * @param name buyer name (null when not provided)
 *
 * @since 1.0.0
 */
public record InvoiceBuyer(@Nullable BuyerIdentifierType identifierType, @Nullable String identifierValue, @Nullable String name) {

}
