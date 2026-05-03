/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Invoice buyer information from metadata.
 *
 * @param identifierType type of buyer identifier
 * @param identifierValue buyer identifier value (NIP, VAT UE number, etc.)
 * @param name buyer name (may be null)
 */
public record InvoiceBuyer(BuyerIdentifierType identifierType, String identifierValue, String name) {

}
