/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

/**
 * Internal — minimal wire result of {@code POST /sessions/{ref}/invoices}.
 * The session impl wraps this into a public
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice}
 * before returning to the consumer.
 *
 * @param referenceNumber invoice reference number for status polling and UPO retrieval
 *
 * @since 1.0.0
 */
public record SendInvoiceResult(String referenceNumber) {

}
