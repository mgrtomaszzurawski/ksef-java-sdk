/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model;

/**
 * Result of starting an invoice export job.
 *
 * @param referenceNumber export reference number for status polling
 *
 * @since 0.1.0
 */
public record ExportInvoicesResult(String referenceNumber) {

}
