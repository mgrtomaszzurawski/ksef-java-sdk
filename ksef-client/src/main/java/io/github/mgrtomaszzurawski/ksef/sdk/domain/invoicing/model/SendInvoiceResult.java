/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Result of sending an invoice within a session.
 *
 * @param referenceNumber invoice reference number for status polling and UPO retrieval
 *
 * @since 1.0.0
 */
public record SendInvoiceResult(String referenceNumber) {

}
