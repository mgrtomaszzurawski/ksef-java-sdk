/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Result of sending an invoice within a session.
 *
 * @param referenceNumber invoice reference number for status polling and UPO retrieval
 */
public record SendInvoiceResult(String referenceNumber) {

}
